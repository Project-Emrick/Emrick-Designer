/*
    Filename: SetupFileSystem.java
    Author: Alex Bolinger && John Danison
    Date Created: 4/6/2025

    Description:
        This class will be a class method of implementing the SetupFileSystem.jar in regard to the
        wired show programming feature. This will eventually update the /data folder of the heltec receiver
        firmware to then be flashed by the PlatormIOFunction.java methods.

        This class is used to handle the strings of data and put them into their separate folders. All credit goes
        to Alex for writing this. The GOAT for real.

 */

package org.emrick.project;

/* Import Packages */
import java.io.*;
import java.util.ArrayList;

public class SetupFileSystem {
    /* Method to handle the file operations */
    public static void processShowData(File dataDir, File pktDir, File csv, String token, String color, String ledCount, String label) {
        try {
            StringBuilder csvDataBuilder = new StringBuilder();
            BufferedReader bfr = new BufferedReader(new FileReader(csv));

            String line;
            for (line = bfr.readLine(); line != null; line = bfr.readLine()) {
                csvDataBuilder.append(line);
            }

            bfr.close();
            String csvData = csvDataBuilder.toString();
            csvData = csvData.substring(0, csvData.indexOf("," + label));
            csvData = csvData.substring(csvData.lastIndexOf(",") + 1);
            int id = Integer.parseInt(csvData);
            String position = label.substring(label.length() - 1);
            File showFile = new File(pktDir.getAbsolutePath() + File.separator + id);
            bfr = new BufferedReader(new FileReader(showFile));
            StringBuilder showBuilder = new StringBuilder();

            for (line = bfr.readLine(); line != null; line = bfr.readLine()) {
                showBuilder.append(line + "\n");
            }

            String packet = showBuilder.toString();
            File showstxt = new File(dataDir.getAbsolutePath() + File.separator + "shows.txt");
            packet = packet.substring(packet.indexOf("Pkt_count: ") + 11);
            int pkt_count = Integer.parseInt(packet.substring(0, packet.indexOf(",")));
            String data = "Pkt_count: " + pkt_count + "\n";
            packet = packet.substring(packet.indexOf("Size: "));
            ArrayList<Integer> offsets = new ArrayList<>();
            ArrayList<Integer> triggers = new ArrayList<>();
            int triggerNum = 0;
            String lastPacket = "";

            String offsetData;
            for (int i = 0; i < pkt_count; ++i) {
                offsetData = packet.substring(0, packet.indexOf("\n"));
                offsets.add(data.length());
                data = data + toStorageFormat(offsetData + "\n") + "\n";
                if (i > 0) {
                    if ((getFlags(lastPacket) & 4) > 0) {
                        triggers.add(triggerNum, i);
                        ++triggerNum;
                    }
                } else {
                    triggers.add(i);
                    ++triggerNum;
                }

                lastPacket = offsetData;
                packet = packet.substring(packet.indexOf("\n") + 1);
            }

            // Write to showstxt, offsets, triggers, etc.
            writeToFile(showstxt, data);
            writeOffsets(dataDir, offsets);
            writeTriggers(dataDir, triggers);
            writeToken(dataDir, token);
            writeColor(dataDir, color);
            writeBoardId(dataDir, id);
            writePosition(dataDir, position);
            writeLedCount(dataDir, ledCount);
            writeBatteryCalibration(dataDir);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Utility method to write data to file */
    private static void writeToFile(File file, String data) throws IOException {
        BufferedWriter bfw = new BufferedWriter(new FileWriter(file));
        bfw.write(data);
        bfw.flush();
        bfw.close();
    }

    /* Write offsets to file */
    private static void writeOffsets(File dataDir, ArrayList<Integer> offsets) throws IOException {
        File offsetstxt = new File(dataDir.getAbsolutePath() + File.separator + "offsets.txt");
        StringBuilder offsetData = new StringBuilder();
        for (Integer offset : offsets) {
            offsetData.append(offset).append("\n");
        }
        writeToFile(offsetstxt, offsetData.toString());
    }

    /* Write triggers to file */
    private static void writeTriggers(File dataDir, ArrayList<Integer> triggers) throws IOException {
        File triggerstxt = new File(dataDir.getAbsolutePath() + File.separator + "triggers.txt");
        StringBuilder triggerData = new StringBuilder();
        for (Integer trigger : triggers) {
            triggerData.append(trigger).append("\n");
        }
        writeToFile(triggerstxt, triggerData.toString());
    }

    /* Write token to file */
    private static void writeToken(File dataDir, String token) throws IOException {
        File tokentxt = new File(dataDir.getAbsolutePath() + File.separator + "token.txt");
        writeToFile(tokentxt, token);
    }

    /* Write color to file */
    private static void writeColor(File dataDir, String color) throws IOException {
        File colortxt = new File(dataDir.getAbsolutePath() + File.separator + "verification_color.txt");
        writeToFile(colortxt, color);
    }

    /* Write board ID to file */
    private static void writeBoardId(File dataDir, int id) throws IOException {
        File boardidtxt = new File(dataDir.getAbsolutePath() + File.separator + "board_id.txt");
        writeToFile(boardidtxt, "" + id + "\n");
    }

    /* Write position to file */
    private static void writePosition(File dataDir, String position) throws IOException {
        File positiontxt = new File(dataDir.getAbsolutePath() + File.separator + "position.txt");
        writeToFile(positiontxt, position);
    }

    /* Write LED count to file */
    private static void writeLedCount(File dataDir, String ledCount) throws IOException {
        File ledcounttxt = new File(dataDir.getAbsolutePath() + File.separator + "led_count.txt");
        writeToFile(ledcounttxt, ledCount);
    }

    /* Write battery calibration data to file */
    private static void writeBatteryCalibration(File dataDir) throws IOException {
        File batteryCalibrationtxt = new File(dataDir.getAbsolutePath() + File.separator + "battery_calibration.txt");
        writeToFile(batteryCalibrationtxt, "3.90,3.45,90.0,");
    }

    /* Get flags from a packet (same as original code) */
    public static int getFlags(String str) {
        str = str.substring(str.indexOf("Size: ") + 6);
        int size = Integer.parseInt(str.substring(0, str.indexOf(",")));
        str = str.substring(str.indexOf("Strip_id: ") + 10);
        String strip_id = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf("Set_id: ") + 8);
        String set_id = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf("Flags: ") + 7);
        return Integer.parseInt(str.substring(0, str.indexOf(",")));
    }

    /* Convert data to storage format */
    public static String toStorageFormat(String str) {
        // Extract Size
        str = str.substring(str.indexOf("Size: ") + 6);
        int size = Integer.parseInt(str.substring(0, str.indexOf(",")));

        // Extract Strip_id
        str = str.substring(str.indexOf("Strip_id: ") + 10);
        String strip_id = str.substring(0, str.indexOf(","));

        // Extract Set_id
        str = str.substring(str.indexOf("Set_id: ") + 8);
        String set_id = str.substring(0, str.indexOf(","));

        // Extract Flags
        str = str.substring(str.indexOf("Flags: ") + 7);
        int flags = Integer.parseInt(str.substring(0, str.indexOf(",")));

        // Extract Start Color values
        str = str.substring(str.indexOf("Start_color: ") + 13);
        String start_colorred = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf(",") + 2);
        String start_colorgreen = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf(",") + 2);
        String start_colorblue = str.substring(0, str.indexOf(","));

        // Extract End Color values
        str = str.substring(str.indexOf("End_color: ") + 11);
        String end_colorred = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf(",") + 2);
        String end_colorgreen = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf(",") + 2);
        String end_colorblue = str.substring(0, str.indexOf(","));

        // Extract Delay and Duration
        str = str.substring(str.indexOf("Delay: ") + 7);
        String delay = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf("Duration: ") + 10);
        String duration = str.substring(0, str.indexOf(","));

        // Extract Function and Timeout
        str = str.substring(str.indexOf("Function: ") + 10);
        String function = str.substring(0, str.indexOf(","));
        str = str.substring(str.indexOf("Timeout: ") + 9);
        String timeout = str.substring(0, str.indexOf("\n"));

        // Extract ExtraParameters if Size > 0
        String extraParameters = "";
        if (size > 0) {
            str = str.substring(str.indexOf("ExtraParameters: ") + 17);
            extraParameters = str.substring(0, str.indexOf("\n"));
            timeout = timeout.substring(0, timeout.indexOf(","));
        }

        // Construct the output string with all the extracted data
        String out = "";
        out += size + "," + set_id + "," + flags + "," + start_colorred + "," + start_colorgreen + "," + start_colorblue;
        out += "," + end_colorred + "," + end_colorgreen + "," + end_colorblue;

        // Conditionally add delay, duration, function, and timeout based on flags
        if ((flags & 8) > 0) {
            out += "," + delay;
        }
        if ((flags & 2) > 0) {
            out += "," + duration;
        }
        if ((flags & 1) == 0) {
            out += "," + function;
        }
        if ((flags & 4) > 0) {
            out += "," + timeout;
        }

        // Add extra parameters if size > 0
        if (size > 0) {
            out += "," + extraParameters;
        }

        return out;
    }
}
