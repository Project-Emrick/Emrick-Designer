package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;

import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SerialTransmitter {
    private SerialPort sp;
    public SerialTransmitter() {
        SerialPort[] sps = SerialPort.getCommPorts();
        for (SerialPort s : sps) {
            if (s.getDescriptivePortName().toLowerCase().contains("cp210x")) {
                sp = s;
                break;
            }
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
            if (s.getDescriptivePortName().toLowerCase().contains("cp210x")) {
                sp = s;
                return true;
            }
        }
        return false;
    }

    public SerialPort getSerialPort() {
        return sp;
    }

    public void writeSet(int set) {
        sp.clearRTS();
        sp.clearDTR();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        String str = "s" + set + "\n";
        byte[] out = str.getBytes();
        sp.writeBytes(out, str.length());
        sp.flushIOBuffers();
        sp.closePort();
    }

    public void enterProgMode(String ssid, String password, int id, long token, Color verificationColor) {
        sp.clearRTS();
        sp.clearDTR();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        String str;
        try {
            str = "p" + InetAddress.getLocalHost().getHostAddress() + "\n" + ssid + "\n" + password + "\n" + id + "\n"
                    + token + "\n" + verificationColor.getRed() + "\n" + verificationColor.getGreen() + "\n" + verificationColor.getBlue() + "\n";
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
        System.out.println(str);
        byte[] out = str.getBytes();
        sp.writeBytes(out, str.length());
        sp.flushIOBuffers();
        sp.closePort();
    }

    public void writeToSerialPort(String str) {
        sp.clearRTS();
        sp.clearDTR();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        byte[] out = str.getBytes();
        sp.writeBytes(out, str.length());
        sp.flushIOBuffers();
        sp.closePort();
    }
}
