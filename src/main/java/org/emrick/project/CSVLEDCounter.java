/*
    Filename: CSVLEDCounter.java
    Author: John Danison
    Date Created: 6/2/2025

    Description:
        This class will take the CSV file input from a user and return the appropriate number of LEDs associated with the
        marcher label.

 */

package org.emrick.project;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CSVLEDCounter {
    private static final Log log = LogFactory.getLog(CSVLEDCounter.class);

    public static String getLedCount(String marcherLabel, String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split CSV line by commas
                String[] parts = line.split(",");

                // Check if this row contains the Marcher Label
                if (parts.length > 3 && parts[2].trim().equals(marcherLabel)) {
                    return parts[3].trim(); // Extract the LED Count
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading or parsing file: " + e.getMessage());
        }
        return "-1";
    }
}