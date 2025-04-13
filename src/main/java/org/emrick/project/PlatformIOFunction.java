/*
    Filename: PlatformIOFunction.java
    Author: John Danison
    Date Created: 4/6/2025

    Description:
        This class will be handling the platformio filesystem upload for wired show programming.

 */
package org.emrick.project;

/* Import Packages */
import javax.swing.JOptionPane;
import java.io.*;

public class PlatformIOFunction {
    /* Method to verify if PlatformIO is installed and accessible */
    public static boolean verifyInstallation() {
        String username = System.getProperty("user.name");
        String activationPath = "C:\\Users\\" + username + "\\.platformio\\penv\\Scripts\\activate";
        String command = String.join(" && ", "call \"" + activationPath + "\"", "pio --version");

        try {
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.toLowerCase().contains("platformio")) {
                    found = true;
                }
            }

            int exitCode = process.waitFor();
            if (found && exitCode == 0) {
                return true; // PlatformIO is installed
            } else {
                return false; // PlatformIO is not found
            }
        } catch (IOException | InterruptedException e) {
            JOptionPane.showMessageDialog(null,
                    "An error occurred while checking for PlatformIO. Please try again.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            return false; // An error occurred during the check
        }
    }

}
