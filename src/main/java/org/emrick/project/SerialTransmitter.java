package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;
import org.emrick.project.dev.UnitPort;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SerialTransmitter {
    private SerialPort sp;
    private String type;
    public SerialTransmitter() {
        SerialPort[] sps = SerialPort.getCommPorts();
        for (SerialPort s : sps) {
            if (s.getDescriptivePortName().toLowerCase().contains("cp210")) {
                sp = s;
                type = getBoardType(sp.getDescriptivePortName());
                break;
            }
            type = "";
        }
        if (sp == null) {

            sp = SerialPort.getCommPorts()[0];
        }
    }

    /**
     * Writes every byte, looping in case the driver accepts only part of the buffer in one call.
     * @return number of bytes written, or -1 on failure
     */
    private static int writeFully(SerialPort port, byte[] data) {
        int written = 0;
        while (written < data.length) {
            int n = port.writeBytes(data, data.length - written, written);
            if (n < 0) {
                return -1;
            }
            written += n;
        }
        return written;
    }

    /**
     * Blocks until everything written has actually left the PC, so the port can be safely closed.
     * Do not call flushIOBuffers() after a write: on Windows it purges bytes still queued for
     * transmission, which silently cut off the tail of long writes such as a full show upload.
     */
    private static void drainOutput(SerialPort port, int byteCount) {
        // ~1 ms per byte covers even 9600 baud, plus headroom
        long deadline = System.currentTimeMillis() + 2000 + byteCount;
        try {
            while (port.bytesAwaitingWrite() > 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(5);
            }
            // Give the USB-UART bridge time to shift out its last buffered bytes before close
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static SerialPort[] getPortNames() {
        return SerialPort.getCommPorts();
    }

    public boolean setSerialPort(String port) {
        SerialPort[] allPorts = SerialPort.getCommPorts();
        SerialPort s = null;
        for (SerialPort p : allPorts) {
            if (p.getDescriptivePortName().equals(port)) {
                s = p;
            }
        }
        if (s != null) {
            if (s.getDescriptivePortName().toLowerCase().contains("cp210")) {
                sp = s;
                type = getBoardType(sp.getDescriptivePortName());
                System.out.println(type);
                return true;
            }
        }
        return false;
    }

    public SerialPort getSerialPort() {
        return sp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void writeBoardID(String boardID, String position) {
        String query = "b" + boardID + "," + position + "\n";
        sp.setDTR();
        sp.setRTS();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
            return;
        }
        sp.closePort();
        sp.clearDTR();
        sp.clearRTS();
        sp.openPort();
        sp.flushIOBuffers();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] out = query.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
    }

    /**
     * Stores the board label (e.g. T10L) on a receiver so it can identify itself without a project open.
     * Sent as a runtime command after boot, so receivers on firmware without label support simply ignore it.
     * @return true if the receiver confirmed the label
     */
    public boolean writeLabel(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        String reply = org.emrick.project.dev.UnitPort.sendCommand(sp, "label " + label.trim(), "@OK label", 8000);
        return reply != null;
    }

    public void writeLEDCount(String ledCount) {
        String query = "l" + ledCount + "\n";
        sp.setDTR();
        sp.setRTS();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
            return;
        }
        sp.closePort();
        sp.clearDTR();
        sp.clearRTS();
        sp.openPort();
        sp.flushIOBuffers();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] out = query.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
    }

    private boolean inPreviewMode = false;

    /** Resets the Receiver into hardware Preview Mode. Idempotent; leaves the port open for subsequent packets. */
    public void enterPreviewMode() {
        if (inPreviewMode) return;
        String query = "e\n";
        sp.setDTR();
        sp.setRTS();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
            return;
        }
        sp.closePort();
        sp.clearDTR();
        sp.clearRTS();
        sp.openPort();
        sp.flushIOBuffers();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] out = query.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        inPreviewMode = true;
    }

    /** Sends one packet line (Packet::fromString format) to be rendered immediately by the Receiver. */
    public void writePreviewPacket(String packetLine) {
        if (!inPreviewMode) {
            enterPreviewMode();
        }
        String query = packetLine + "\n";
        byte[] out = query.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
    }

    /** Tells the Receiver to leave Preview Mode and resume normal operation. */
    public void exitPreviewMode() {
        if (!inPreviewMode) return;
        String query = "stop\n";
        byte[] out = query.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
        inPreviewMode = false;
    }

    public void writeShow(String token, String show) {
        String query = "p" + token + "," + show + "\n";
        System.out.println("data written to serial: \n" + query);
        sp.setDTR();
        sp.setRTS();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
            return;
        }
        sp.closePort();
        sp.clearDTR();
        sp.clearRTS();
        sp.openPort();
        sp.flushIOBuffers();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] out = query.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
    }


    public String getBoardType(String port) {
        SerialPort[] allPorts = SerialPort.getCommPorts();
        SerialPort s = null;
        for (SerialPort p : allPorts) {
            if (p.getDescriptivePortName().equals(port)) {
                s = p;
            }
        }

        if (s != null) {
            if (s.getDescriptivePortName().toLowerCase().contains("cp210x")) {
                // Same probe Developer Mode uses: it retries through the boot window and ignores boot
                // messages, where a single timed read could miss the reply and report no board.
                String owner = UnitPort.ownerOf(s.getSystemPortName());
                if (owner != null) {
                    System.out.println("Port is busy: " + s.getSystemPortName() + " is in use by Developer Mode (" + owner + ")");
                    return "";
                }
                String type = UnitPort.probeType(s);
                if (UnitPort.UNKNOWN.equals(type)) {
                    System.out.println("No board type reply on " + s.getSystemPortName());
                    return "";
                }
                return type;
            }
        }
        return "";
    }

    public void writeSet(int set) {
        sp.clearRTS();
        sp.clearDTR();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        String str = "s" + set + "\n";
        byte[] out = str.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
    }

    public synchronized void enterProgMode(String ssid, String password, int port, int id, long token, Color verificationColor) {
        sp.clearRTS();
        sp.clearDTR();
        try {
            Thread.sleep(250);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        String str = "p";
        try {
            str += InetAddress.getLocalHost().getHostAddress() + "\n" + ssid + "\n" + password + "\n" + port + "\n" + id + "\n"
                    + token + "\n" + verificationColor.getRed() + "\n" + verificationColor.getGreen() + "\n" + verificationColor.getBlue() + "\n";
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
        //System.out.println(str.replaceAll("\n", ","));
        byte[] out = str.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
    }

    public void writeToSerialPort(String str) {
        sp.clearRTS();
        sp.clearDTR();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        byte[] out = str.getBytes();
        int num = writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
        System.out.println("INFO: " + sp.getDescriptivePortName() + " " + num + " " + str);
        if (num == -1) {
            // show error popup
            System.err.println("ERROR: failed to write to serial port: " + sp.getDescriptivePortName());
            // show a Swing dialog on the EDT to notify the user
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "Failed to write to serial port: " + sp.getDescriptivePortName(),
                            "Serial Port Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                }
            });
        }
    }
    public class BlockingThread implements Runnable {
        byte[] out;
        int len;
        public BlockingThread(byte[] out, int len) {
            this.out = out;
            this.len = len;

        }

        public void run() {

            //Thread.sleep(2000);
            writeFully(sp, out);
            drainOutput(sp, out.length);
        }
    }
}
