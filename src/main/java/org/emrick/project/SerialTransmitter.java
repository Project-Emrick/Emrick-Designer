package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;

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
                String query = "q";
                if (!s.openPort()) {
                    System.out.println("Port is busy");
                    return "";
                }
                s.closePort();
                s.clearDTR();
                s.clearRTS();
                s.openPort();
                s.flushIOBuffers();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                s.writeBytes(query.getBytes(), query.length());
                byte[] buf = new byte[100];
                s.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
                int read = s.readBytes(buf, 100);
                s.closePort();
                if (read > 0) {
                    char type = (char) buf[read-1];
                    String out;
                    switch (type) {
                        case 'r' : out = "Receiver"; break;
                        case 't' : out = "Transmitter"; break;
                        default : out = "";
                    }
                    return out;
                }
            }
        }
        return "";
    }

    public void writeSet(int set, boolean isLightBoardMode) {
        sp.clearRTS();
        sp.clearDTR();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        String str;
        if (isLightBoardMode) {
            str = "b";
        } else {
            str = "s";
        }
        str += set + "\n";
        byte[] out = str.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
    }

    public synchronized void enterProgMode(String ssid, String password, int port, int id, long token, Color verificationColor, boolean mode) {
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
        String str;
        if (mode) {
            str = "l";
        } else {
            str = "p";
        }
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

    public synchronized void enterRSSILoggerMode(String ssid, String password, int port, int allowedConnections) {
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
        String str;

        try {
            str = "t" + InetAddress.getLocalHost().getHostAddress() + "\n" + ssid + "\n" + password + "\n" + port + "\n" + allowedConnections + "\n";
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }

        byte[] out = str.getBytes();
        writeFully(sp, out);
        drainOutput(sp, out.length);
        sp.closePort();
    }

    public void clearRSSIData() {
        String query = "x\n";

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
}

