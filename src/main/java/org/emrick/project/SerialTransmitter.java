package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;

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
    }

    public void setSerialPort(String port) {
        sp = SerialPort.getCommPort(port);
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
