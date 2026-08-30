package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class BoardDriverInstaller {

    private static final String WINDOWS_DRIVER_URL = "https://www.silabs.com/documents/public/software/CP210x_Universal_Windows_Driver.zip";
    private static final String MAC_DRIVER_URL = "https://www.silabs.com/documents/public/software/Mac_OSX_VCP_Driver.zip";
    private static final String DRIVER_INFO_URL = "https://www.silabs.com/software-and-tools/usb-to-uart-bridge-vcp-drivers?tab=downloads";
    private static final Duration POST_INSTALL_VERIFY_TIMEOUT = Duration.ofSeconds(15);

    private BoardDriverInstaller() {
    }

    public static void installBoardDriver(JFrame owner, HardwareStatusIndicator hardwareStatusIndicator,
                                          Consumer<String> sysMsgWriter) {
        ThemedLoadingDialog dialog = new ThemedLoadingDialog(
            owner,
            "Install Board Driver",
            "DRIVER SETUP",
            "Preparing driver install...",
            "Checking your system and attached board.",
            "This can take a moment while the driver package downloads and installs."
        );
        Consumer<String> safeWriter = sysMsgWriter != null ? sysMsgWriter : message -> {
        };

        SwingWorker<InstallOutcome, ProgressUpdate> worker = new SwingWorker<>() {
            @Override
            protected InstallOutcome doInBackground() {
                OperatingSystem os = OperatingSystem.detect();
                safeWriter.accept("Checking board driver status...");
                publish(new ProgressUpdate("Checking board", "Scanning for a connected CP210x device on " + os.displayName + "..."));

                DeviceProbe beforeProbe = probeForDevice(os);
                if (beforeProbe.serialReady) {
                    return InstallOutcome.info(
                            "Board Ready",
                            "A CP210x board is already available on " + String.join(", ", beforeProbe.serialPorts) + ". No driver install was needed."
                    );
                }

                if (beforeProbe.devicePresent) {
                    publish(new ProgressUpdate("Driver needed", "Board detected, but no usable CP210x serial driver is active yet."));
                } else {
                    return InstallOutcome.warning(
                            "Board Not Detected",
                            "No CP210x board was detected. Connect the board to a COM port, then run Install Board Driver again."
                    );
                }

                try {
                    switch (os) {
                        case WINDOWS -> installWindowsDriver(update -> publish(update));
                        case MAC -> installMacDriver(update -> publish(update));
                        case LINUX -> installLinuxDriver(update -> publish(update));
                        default -> {
                            return InstallOutcome.error(
                                    "Unsupported OS",
                                    "Automatic CP210x driver installation is not supported on this operating system."
                            );
                        }
                    }
                } catch (Exception ex) {
                    return InstallOutcome.error("Driver Install Failed", ex.getMessage());
                }

                publish(new ProgressUpdate("Verifying device", "Rechecking whether the CP210x board is now available..."));
                refreshHardwareIndicator(hardwareStatusIndicator);
                DeviceProbe afterProbe = waitForDevice(os, POST_INSTALL_VERIFY_TIMEOUT, update -> publish(update));

                if (afterProbe.serialReady) {
                    return InstallOutcome.success(
                            "Driver Installed",
                            "The CP210x driver is available and the board is now detected on " + String.join(", ", afterProbe.serialPorts) + "."
                    );
                }

                if (afterProbe.devicePresent) {
                    return InstallOutcome.warning(
                            "Installer Completed",
                            "The driver flow completed, but the board still is not exposed as a serial device. Reconnect the board and run Install Board Driver again if needed."
                    );
                }

                return InstallOutcome.warning(
                        "Verification Incomplete",
                        "The driver flow finished, but no CP210x device could be detected afterward. Plug the board back in and try again to confirm installation."
                );
            }

            @Override
            protected void process(List<ProgressUpdate> chunks) {
                for (ProgressUpdate update : chunks) {
                    dialog.update(update.title(), update.detail());
                    safeWriter.accept(update.detail);
                }
            }

            @Override
            protected void done() {
                dialog.dispose();
                if (isCancelled()) {
                    safeWriter.accept("Board driver installation cancelled.");
                    return;
                }
                try {
                    InstallOutcome outcome = get();
                    safeWriter.accept(outcome.message);
                    JOptionPane.showMessageDialog(owner, outcome.message, outcome.title, outcome.messageType);
                } catch (Exception ex) {
                    safeWriter.accept("Board driver installation failed.");
                    JOptionPane.showMessageDialog(owner,
                            "The board driver flow failed: " + ex.getMessage(),
                            "Driver Install Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        dialog.setCancelAction(() -> worker.cancel(true));
        worker.execute();
        dialog.setVisible(true);
    }

    private static DeviceProbe waitForDevice(OperatingSystem os, Duration timeout, Consumer<ProgressUpdate> progressConsumer) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        DeviceProbe probe = probeForDevice(os);
        while (!probe.serialReady && System.currentTimeMillis() < deadline) {
            sleep(1000);
            probe = probeForDevice(os);
            if (probe.devicePresent && progressConsumer != null) {
                progressConsumer.accept(new ProgressUpdate("Waiting for device", "Board detected. Waiting for the serial port to come online..."));
            }
        }
        return probe;
    }

    private static void installWindowsDriver(Consumer<ProgressUpdate> progressConsumer) throws IOException, InterruptedException {
        progressConsumer.accept(new ProgressUpdate("Downloading driver", "Downloading the Silicon Labs Windows CP210x driver package..."));
        Path extractedDir = downloadAndExtract(WINDOWS_DRIVER_URL, "cp210x-windows", progressConsumer);

        progressConsumer.accept(new ProgressUpdate("Installing driver", "Installing Windows CP210x drivers with pnputil..."));
        CommandResult installResult = runCommand(List.of(
                "pnputil",
                "/add-driver",
                extractedDir.toString() + "\\*.inf",
                "/subdirs",
                "/install"
        ));

        if (installResult.exitCode != 0) {
            CommandResult elevatedResult = runCommand(List.of(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    "Start-Process -FilePath 'pnputil.exe' -ArgumentList @('/add-driver','" +
                            escapeForPowerShell(extractedDir.toString() + "\\*.inf") +
                            "','/subdirs','/install') -Verb RunAs -Wait"
            ));

            if (elevatedResult.exitCode != 0) {
                throw new IOException("Windows driver install failed. " + joinOutput(installResult, elevatedResult));
            }
        }
    }

    private static void installMacDriver(Consumer<ProgressUpdate> progressConsumer) throws IOException, InterruptedException {
        progressConsumer.accept(new ProgressUpdate("Downloading driver", "Downloading the Silicon Labs macOS CP210x driver package..."));
        Path extractedDir = downloadAndExtract(MAC_DRIVER_URL, "cp210x-macos", progressConsumer);

        Path installer = findFirstMatching(extractedDir, path -> {
            String lower = path.getFileName().toString().toLowerCase(Locale.ENGLISH);
            return lower.endsWith(".pkg") || lower.endsWith(".dmg");
        });

        if (installer == null) {
            throw new IOException("No macOS installer package was found in the downloaded archive.");
        }

        progressConsumer.accept(new ProgressUpdate("Launching installer", "Opening the macOS installer package. Follow the system prompts to finish installation."));
        CommandResult result = runCommand(List.of("open", "-W", installer.toString()));
        if (result.exitCode != 0) {
            throw new IOException("Failed to launch the macOS installer. " + joinOutput(result));
        }
    }

    private static void installLinuxDriver(Consumer<ProgressUpdate> progressConsumer) throws IOException, InterruptedException {
        progressConsumer.accept(new ProgressUpdate("Loading kernel module", "Linux normally ships the CP210x driver in-kernel. Attempting to load the cp210x module..."));
        CommandResult result = runCommand(List.of("sh", "-lc", "modprobe cp210x"));
        if (result.exitCode != 0) {
            throw new IOException("Linux could not load the cp210x kernel module automatically. Try running 'sudo modprobe cp210x' manually. " + DRIVER_INFO_URL);
        }
    }

    private static DeviceProbe probeForDevice(OperatingSystem os) {
        List<String> serialPorts = findCp210SerialPorts();
        if (!serialPorts.isEmpty()) {
            return new DeviceProbe(true, true, serialPorts);
        }

        return switch (os) {
            case WINDOWS -> new DeviceProbe(false, commandShowsDevice(List.of(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    "Get-PnpDevice -PresentOnly | Where-Object { $_.InstanceId -match 'VID_10C4' -or $_.FriendlyName -match 'CP210|Silicon Labs' } | Format-List -Property Status,FriendlyName,InstanceId | Out-String -Width 500"
            )), serialPorts);
            case MAC -> new DeviceProbe(false, commandShowsDevice(List.of(
                    "system_profiler",
                    "SPUSBDataType"
            )), serialPorts);
            case LINUX -> new DeviceProbe(false, commandShowsDevice(List.of(
                    "sh",
                    "-lc",
                    "lsusb 2>/dev/null"
            )), serialPorts);
            default -> new DeviceProbe(false, false, serialPorts);
        };
    }

    private static boolean commandShowsDevice(List<String> command) {
        try {
            CommandResult result = runCommand(command);
            String output = result.output.toLowerCase(Locale.ENGLISH);
            return output.contains("vid_10c4")
                    || output.contains("10c4")
                    || output.contains("cp210")
                    || output.contains("silicon labs");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static List<String> findCp210SerialPorts() {
        List<String> ports = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            String combined = (safe(port.getDescriptivePortName()) + " " + safe(port.getPortDescription()) + " " + safe(port.getSystemPortName())).toLowerCase(Locale.ENGLISH);
            if (combined.contains("cp210") || combined.contains("silicon labs")) {
                ports.add(port.getSystemPortName());
            }
        }
        return ports;
    }

    private static Path downloadAndExtract(String url, String folderName, Consumer<ProgressUpdate> progressConsumer) throws IOException {
        Path tempRoot = Files.createTempDirectory("emrick-board-driver-");
        Path zipPath = tempRoot.resolve(folderName + ".zip");
        Path extractDir = tempRoot.resolve(folderName);
        Files.createDirectories(extractDir);

        try (InputStream inputStream = new BufferedInputStream(new URL(url).openStream())) {
            Files.copy(inputStream, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        progressConsumer.accept(new ProgressUpdate("Preparing package", "Extracting the downloaded driver package..."));
        unzip(zipPath, extractDir);
        return extractDir;
    }

    private static void unzip(Path zipPath, Path destinationDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path resolvedPath = destinationDir.resolve(entry.getName()).normalize();
                if (!resolvedPath.startsWith(destinationDir)) {
                    throw new IOException("Blocked invalid archive entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Path parent = resolvedPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipInputStream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private static Path findFirstMatching(Path root, Predicate<Path> predicate) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(predicate)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static CommandResult runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = readOutput(process.getInputStream());
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output);
    }

    private static String readOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    output.append(line).append(System.lineSeparator());
                }
            }
        }
        return output.toString();
    }

    private static void refreshHardwareIndicator(HardwareStatusIndicator hardwareStatusIndicator) {
        if (hardwareStatusIndicator == null) {
            return;
        }

        if (SwingUtilities.isEventDispatchThread()) {
            hardwareStatusIndicator.scanForHardwareBlocking();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(hardwareStatusIndicator::scanForHardwareBlocking);
        } catch (Exception ignored) {
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String escapeForPowerShell(String value) {
        return value.replace("'", "''");
    }

    private static String joinOutput(CommandResult... results) {
        StringBuilder builder = new StringBuilder();
        for (CommandResult result : results) {
            if (result != null && result.output != null && !result.output.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(result.output.trim());
            }
        }
        return builder.toString().trim();
    }

    private record CommandResult(int exitCode, String output) {
    }

    private record DeviceProbe(boolean serialReady, boolean devicePresent, List<String> serialPorts) {
    }

    private record ProgressUpdate(String title, String detail) {
    }

    private record InstallOutcome(String title, String message, int messageType) {
        private static InstallOutcome success(String title, String message) {
            return new InstallOutcome(title, message, JOptionPane.INFORMATION_MESSAGE);
        }

        private static InstallOutcome info(String title, String message) {
            return new InstallOutcome(title, message, JOptionPane.INFORMATION_MESSAGE);
        }

        private static InstallOutcome warning(String title, String message) {
            return new InstallOutcome(title, message, JOptionPane.WARNING_MESSAGE);
        }

        private static InstallOutcome error(String title, String message) {
            return new InstallOutcome(title, message, JOptionPane.ERROR_MESSAGE);
        }
    }

    private enum OperatingSystem {
        WINDOWS("Windows"),
        MAC("macOS"),
        LINUX("Linux"),
        OTHER("Unknown OS");

        private final String displayName;

        OperatingSystem(String displayName) {
            this.displayName = displayName;
        }

        private static OperatingSystem detect() {
            String osName = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
            if (osName.contains("win")) {
                return WINDOWS;
            }
            if (osName.contains("mac")) {
                return MAC;
            }
            if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                return LINUX;
            }
            return OTHER;
        }
    }

}