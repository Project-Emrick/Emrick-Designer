package org.emrick.project;

public class SerialTest {
    @org.junit.jupiter.api.Test
    public void testSerial() {
        SerialTransmitter st = new SerialTransmitter();
        st.setSerialPort("COM21");
        st.writeToSerialPort("p");
    }
}
