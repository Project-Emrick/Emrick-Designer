/*
    Filename: PlatformIOFunction.java
    Author: John Danison
    Date Created: 4/6/2025

    Description:
        This class will be handling the platformio filesystem upload for wired show programming.

 */
package org.emrick.project;

/* Import Packages */
import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlatformIOFunction {
    /**
     *  Method to verify if PlatformIO is installed and accessible
     **/
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

    /**
     * Master Method to Upload the Filesystem to a board
     * @param dataDirectory path to dataDirectory for PlatformIO
     */
    public static void uploadFilesystem(File dataDirectory) {
        OutputDialog dialog = new OutputDialog();
        dialog.setVisible(true);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                "Invalid data directory: " + dataDirectory.getAbsolutePath(),
                                "Configuration Error",
                                JOptionPane.ERROR_MESSAGE));
                        return null;
                    }

                    File projectDir = dataDirectory.getParentFile();
                    if (projectDir == null || !projectDir.exists()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                "Invalid project directory for path: " + dataDirectory.getAbsolutePath(),
                                "Configuration Error",
                                JOptionPane.ERROR_MESSAGE));
                        return null;
                    }

                    // Step 1: Build with output consumer
                    if (!buildFilesystem(projectDir, this::publishLine)) {
                        return null;
                    }

                    // Step 2: Detect COM port
                    String comPort = detectSingleCOMPort();
                    if (comPort == null) {
                        return null;
                    }

                    // Step 3: Upload with output consumer
                    uploadToPort(comPort, projectDir, this::publishLine);

                } catch (Exception e) {
                    publishLine("Error: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }

            private void publishLine(String line) {
                publish(line);
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    dialog.appendLine(line);
                }
            }

            @Override
            protected void done() {
                dialog.enableCloseButton();
            }
        };

        worker.execute();
    }

    private static boolean buildFilesystem(File projectDir, Consumer<String> outputConsumer) {
        try {
            String activationPath = getActivationPath();
            String command = String.join(" && ",
                    "call \"" + activationPath + "\"",
                    "pio run --target buildfs"
            );

            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.directory(projectDir);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            readProcessOutput(process, outputConsumer);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Error building filesystem. Exit code: " + exitCode,
                        "Build Error",
                        JOptionPane.ERROR_MESSAGE));
                return false;
            }
            return true;
        } catch (IOException | InterruptedException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Build error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE));
            return false;
        }
    }

    private static void uploadToPort(String port, File projectDir, Consumer<String> outputConsumer) {
        try {
            String activationPath = getActivationPath();
            String command = String.join(" && ",
                    "call \"" + activationPath + "\"",
                    "pio run --target uploadfs --upload-port " + port
            );

            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
            builder.directory(projectDir);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            readProcessOutput(process, outputConsumer);

            int exitCode = process.waitFor();
            SwingUtilities.invokeLater(() -> {
                if (exitCode == 0) {
                    JOptionPane.showMessageDialog(null,
                            "Upload successful to " + port,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Upload failed to " + port + ". Exit code: " + exitCode,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (IOException | InterruptedException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Upload error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE));
        }
    }

    private static String detectSingleCOMPort() throws IOException, InterruptedException {
        List<String> ports = getAvailableCOMPorts();

        if (ports.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "No COM ports found. Please connect a device and try again.",
                    "No COM Ports",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (ports.size() > 1) {
            JOptionPane.showMessageDialog(null,
                    "Multiple COM ports detected. Only one device should be connected at a time.\nDetected ports: " + ports,
                    "Multiple Ports",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return ports.get(0);
    }

    private static List<String> getAvailableCOMPorts() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("wmic", "path", "Win32_SerialPort", "get", "DeviceID", "/format:list");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> ports = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("DeviceID=")) {
                    ports.add(line.substring("DeviceID=".length()));
                }
            }
        }
        process.waitFor();
        return ports;
    }

    private static String getActivationPath() {
        String username = System.getProperty("user.name");
        return "C:\\Users\\" + username + "\\.platformio\\penv\\Scripts\\activate";
    }

    private static void readProcessOutput(Process process, Consumer<String> outputConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputConsumer.accept(line);
            }
        }
    }
}