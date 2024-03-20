package org.emrick.project;

public class SerialTest {
    @org.junit.jupiter.api.Test
    public void testSerial() {
        SerialTransmitter st = new SerialTransmitter();
        st.setSerialPort("COM29");
        st.writeToSerialPort("s0\n");
    }
}
