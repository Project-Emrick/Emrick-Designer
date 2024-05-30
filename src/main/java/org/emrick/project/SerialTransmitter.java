package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;

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

    public boolean setSerialPort(String port) {
        SerialPort s = SerialPort.getCommPort(port);
        if (s != null) {
            System.out.println(s.getDescriptivePortName());
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

    public void enterProgMode() {
        sp.clearRTS();
        sp.clearDTR();
        if (!sp.openPort()) {
            System.out.println("Port is busy");
        }
        String str;
        try {
            str = "p" + InetAddress.getLocalHost().getHostAddress() + "\n";
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
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
