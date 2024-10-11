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
        sp.writeBytes(out, query.length());
        sp.flushIOBuffers();
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
        sp.writeBytes(out, query.length());
        sp.flushIOBuffers();
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
        sp.writeBytes(out, str.length());
        sp.flushIOBuffers();
        sp.closePort();
    }

    public void enterProgMode(String ssid, String password, int port, int id, long token, Color verificationColor, boolean mode) {
        sp.clearRTS();
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
        //System.out.println(str);
        byte[] out = str.getBytes();
        sp.writeBytes(out, str.length());
        sp.flushIOBuffers();
        sp.closePort();
    }

    public void writeToSerialPort(String str) {
        sp.clearRTS();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        byte[] out = str.getBytes();


        int num = sp.writeBytes(out, str.length());
        sp.flushIOBuffers();
        sp.closePort();
        System.out.println("INFO: " + sp.getDescriptivePortName() + " " + num + " " + str);
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
            sp.writeBytes(out, len);
            sp.flushIOBuffers();
        }
    }

}

