package org.emrick.project.dev;

import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serial helpers for talking to Emrick units (Heltec boards behind a CP210x USB-UART bridge) at 9600 baud.
 *
 * Opening a port normally does not reset the unit (DTR/RTS are cleared first). Receivers on current
 * firmware answer runtime commands ("info", "debug on", "blink", "label ...") at any time. Older firmware
 * and transmitters only answer the board-type query 'q' in the 1 second window after a reset, so
 * {@link #probeType} falls back to resetting the unit.
 *
 * Ports are claimed by one Developer Mode feature at a time through {@link #claim} so, for example,
 * the flasher never fights the terminal for a port.
 */
public final class UnitPort {
    public static final String INFO_PREFIX = "@INFO ";
    public static final String RECEIVER = "Receiver";
    public static final String TRANSMITTER = "Transmitter";
    public static final String UNKNOWN = "Unknown";

    private static final int BAUD = 9600;
    private static final ConcurrentHashMap<String, String> owners = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Runnable> releasers = new ConcurrentHashMap<>();
    // Last type seen on each port, so one tab can reuse what another already found without probing again
    private static final ConcurrentHashMap<String, String> knownTypes = new ConcurrentHashMap<>();
    // Last details read from each receiver, so dropdowns can show board labels instead of COM ports
    private static final ConcurrentHashMap<String, UnitInfo> knownInfo = new ConcurrentHashMap<>();

    private UnitPort() {
    }

    /** @return every connected Emrick unit (CP210x bridge), sorted by port name */
    public static List<SerialPort> emrickPorts() {
        List<SerialPort> ports = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            String name = (p.getDescriptivePortName() + " " + p.getPortDescription()).toLowerCase();
            if (name.contains("cp210")) {
                ports.add(p);
            }
        }
        ports.sort(Comparator.comparing(SerialPort::getSystemPortName));
        return ports;
    }

    /** @return a fresh handle for the port with this system name (e.g. COM12), or null if it's gone */
    public static SerialPort find(String systemPortName) {
        return Arrays.stream(SerialPort.getCommPorts())
                .filter(p -> p.getSystemPortName().equals(systemPortName))
                .findFirst().orElse(null);
    }

    // ---------------------------------------------------------------- port ownership

    /** @return true if the port was free and is now owned by {@code owner} */
    public static boolean claim(String systemPortName, String owner) {
        String current = owners.putIfAbsent(systemPortName, owner);
        return current == null || current.equals(owner);
    }

    /**
     * Claims a port and registers how to give it back early, so a higher-priority feature (flashing) can
     * take it over. {@code letGo} must eventually call {@link #release}.
     */
    public static boolean claim(String systemPortName, String owner, Runnable letGo) {
        if (!claim(systemPortName, owner)) {
            return false;
        }
        releasers.put(systemPortName, letGo);
        return true;
    }

    /**
     * Like {@link #claim(String, String, Runnable)}, but waits for short-lived users (e.g. a unit being read)
     * to finish instead of failing right away. Never takes the port from anyone.
     */
    public static boolean claimWhenFree(String systemPortName, String owner, Runnable letGo, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!claim(systemPortName, owner, letGo)) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
            sleep(50);
        }
        return true;
    }

    public static void release(String systemPortName, String owner) {
        if (owners.remove(systemPortName, owner)) {
            releasers.remove(systemPortName);
        }
    }

    /**
     * Claims a port even if another feature has it: the holder is asked to let go (e.g. the terminal
     * disconnects), and short-lived users (a unit being read) are waited for.
     *
     * @return true if {@code owner} now has the port
     */
    public static boolean takeOver(String systemPortName, String owner, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        boolean asked = false;
        while (!claim(systemPortName, owner)) {
            Runnable letGo = releasers.get(systemPortName);
            if (letGo != null && !asked) {
                asked = true;
                letGo.run();
            }
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
            sleep(50);
        }
        return true;
    }

    /** @return true if any port is held by one of these features (e.g. "Terminal") */
    public static boolean isAnyPortOwnedBy(String... featureNames) {
        for (String owner : owners.values()) {
            for (String name : featureNames) {
                if (owner.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** @return the feature using the port (e.g. "Terminal"), or null if it's free */
    public static String ownerOf(String systemPortName) {
        return owners.get(systemPortName);
    }

    // ---------------------------------------------------------------- low level

    /** Opens the port without resetting the unit. */
    public static boolean open(SerialPort sp) {
        if (sp.isOpen()) {
            return true;
        }
        sp.setBaudRate(BAUD);
        sp.clearDTR();
        sp.clearRTS();
        if (!sp.openPort()) {
            return false;
        }
        sp.clearDTR();
        sp.clearRTS();
        sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        return true;
    }

    /** Opens the port without a reset, retrying briefly in case something else just let go of it. */
    public static boolean openWithRetry(SerialPort sp, int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (open(sp)) {
                return true;
            }
            sleep(500);
        }
        return false;
    }

    /** Resets the unit by pulsing the bridge's RTS line (wired to the ESP32 enable pin). Port must be open. */
    public static void reset(SerialPort sp) {
        sp.clearDTR();
        sp.setRTS();
        sleep(120);
        sp.clearRTS();
    }

    public static boolean write(SerialPort sp, String text) {
        byte[] data = text.getBytes(StandardCharsets.US_ASCII);
        int written = 0;
        while (written < data.length) {
            int n = sp.writeBytes(data, data.length - written, written);
            if (n < 0) {
                return false;
            }
            written += n;
        }
        // Let the bytes leave the PC before a caller closes the port
        long deadline = System.currentTimeMillis() + 2000 + data.length;
        while (sp.bytesAwaitingWrite() > 0 && System.currentTimeMillis() < deadline) {
            sleep(5);
        }
        return true;
    }

    /** Reads whatever arrives within {@code millis}. */
    public static byte[] readFor(SerialPort sp, int millis) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            int n = sp.bytesAvailable() > 0 ? sp.readBytes(buf, Math.min(buf.length, sp.bytesAvailable())) : 0;
            if (n > 0) {
                out.write(buf, 0, n);
            } else {
                sleep(10);
            }
        }
        return out.toByteArray();
    }

    /**
     * Opens the port (without a reset) if needed, writes a single short message, and closes it again if
     * this call opened it. Used for transmitter commands such as identify.
     */
    public static boolean sendRaw(SerialPort sp, String text) {
        boolean wasOpen = sp.isOpen();
        if (!open(sp)) {
            return false;
        }
        try {
            return write(sp, text);
        } finally {
            if (!wasOpen) {
                sp.closePort();
            }
        }
    }

    // ---------------------------------------------------------------- receiver commands

    /**
     * Sends a runtime command and waits for the reply line starting with {@code replyPrefix}. The command
     * is re-sent every 2 seconds in case the unit was still booting.
     *
     * @return the reply line, or null on timeout (e.g. older firmware without runtime commands)
     */
    public static String sendCommand(SerialPort sp, String command, String replyPrefix, int timeoutMs) {
        boolean wasOpen = sp.isOpen();
        if (!open(sp)) {
            return null;
        }
        try {
            if (!wasOpen) {
                sp.flushIOBuffers();
            }
            StringBuilder pending = new StringBuilder();
            long end = System.currentTimeMillis() + timeoutMs;
            long nextSend = 0;
            while (System.currentTimeMillis() < end) {
                if (System.currentTimeMillis() >= nextSend) {
                    write(sp, command + "\n");
                    nextSend = System.currentTimeMillis() + 2000;
                }
                byte[] data = readFor(sp, 100);
                pending.append(new String(data, StandardCharsets.US_ASCII));
                int nl;
                while ((nl = pending.indexOf("\n")) >= 0) {
                    String line = pending.substring(0, nl).trim();
                    pending.delete(0, nl + 1);
                    if (line.startsWith(replyPrefix)) {
                        return line;
                    }
                }
            }
            return null;
        } finally {
            if (!wasOpen) {
                sp.closePort();
            }
        }
    }

    /** @return the unit's details, or null if it didn't answer (not a receiver, or older firmware) */
    public static UnitInfo queryInfo(SerialPort sp, int timeoutMs) {
        UnitInfo info = UnitInfo.parse(sendCommand(sp, "info", INFO_PREFIX, timeoutMs));
        if (info != null) {
            knownInfo.put(sp.getSystemPortName(), info);
        }
        return info;
    }

    /**
     * Friendly name for a port, for dropdowns: the board label, else "Board 12", else the unit type, else the
     * port itself if the unit hasn't been read yet.
     */
    public static String displayName(String systemPortName) {
        UnitInfo info = knownInfo.get(systemPortName);
        if (info != null && info.label != null && !info.label.isBlank()) {
            return info.label;
        }
        if (info != null) {
            return "Board " + info.id;
        }
        String type = knownTypes.get(systemPortName);
        return type != null ? type : systemPortName;
    }

    /** Starts or stops blinking the receiver's status LED red, to find it. @return true if confirmed */
    public static boolean blink(SerialPort sp, boolean on) {
        return sendCommand(sp, on ? "blink" : "blink off", "@OK blink", 3000) != null;
    }

    public static boolean setDebug(SerialPort sp, boolean on) {
        return sendCommand(sp, on ? "debug on" : "debug off", "@OK debug", 3000) != null;
    }

    // ---------------------------------------------------------------- board type

    /**
     * Works out whether a unit is a receiver or transmitter. Current receivers answer 'q' at any time; if
     * that gets no answer the unit is reset and asked again during its boot window, which every receiver
     * and transmitter firmware supports.
     *
     * @return {@link #RECEIVER}, {@link #TRANSMITTER}, or {@link #UNKNOWN}
     */
    public static String probeType(SerialPort sp) {
        String type = probe(sp);
        if (UNKNOWN.equals(type)) {
            knownTypes.remove(sp.getSystemPortName());
        } else {
            knownTypes.put(sp.getSystemPortName(), type);
        }
        if (!RECEIVER.equals(type)) {
            knownInfo.remove(sp.getSystemPortName());
        }
        return type;
    }

    /** @return the type last found on this port by {@link #probeType}, or null if it hasn't been probed */
    public static String knownType(String systemPortName) {
        return knownTypes.get(systemPortName);
    }

    private static String probe(SerialPort sp) {
        boolean wasOpen = sp.isOpen();
        if (!open(sp)) {
            return UNKNOWN;
        }
        try {
            sp.flushIOBuffers();
            write(sp, "q");
            String quick = lettersIn(readFor(sp, 400));
            if (quick.equals("r")) {
                return RECEIVER;
            }

            // Reset, drop the ROM bootloader's noise, then ask during the boot window
            reset(sp);
            sleep(300);
            sp.flushIOBuffers();
            long end = System.currentTimeMillis() + 3500;
            while (System.currentTimeMillis() < end) {
                write(sp, "q");
                // The reply is a single 'r' or 't' on its own. Requiring that avoids matching letters in
                // the receiver's boot messages.
                String reply = lettersIn(readFor(sp, 150));
                if (reply.equals("r")) {
                    return RECEIVER;
                }
                if (reply.equals("t")) {
                    return TRANSMITTER;
                }
            }
            return UNKNOWN;
        } finally {
            if (!wasOpen) {
                sp.closePort();
            }
        }
    }

    private static String lettersIn(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (b > ' ' && b < 127) {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
