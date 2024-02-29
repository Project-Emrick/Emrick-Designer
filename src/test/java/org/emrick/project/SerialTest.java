package org.emrick.project;

public class SerialTest {
    @org.junit.jupiter.api.Test
    public void testSerial() {
        SerialTransmitter st = new SerialTransmitter();
        st.setSerialPort("COM7");
        st.writeToSerialPort("s3\n");
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
