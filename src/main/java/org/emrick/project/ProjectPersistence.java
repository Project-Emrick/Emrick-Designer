package org.emrick.project;

import com.google.gson.Gson;
import org.emrick.project.serde.ProjectFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ProjectPersistence {
    private static final String RECOVERY_DIRECTORY = "Recovery";

    private ProjectPersistence() {
    }

    public static void save(Path destination, String jsonFileName, String projectJson,
                            List<Path> projectFiles, Gson gson) throws IOException {
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        Path parentDirectory = normalizedDestination.getParent();
        if (parentDirectory == null) {
            throw new IOException("The project file must have a parent directory.");
        }
        Files.createDirectories(parentDirectory);

        Path candidate = Files.createTempFile(parentDirectory, ".emrick-save-", ".tmp");
        try {
            writeArchive(candidate, jsonFileName, projectJson, projectFiles);
            validateArchive(candidate, gson);

            if (Files.exists(normalizedDestination)) {
                Path recoveryFile = recoveryFileFor(normalizedDestination);
                Files.createDirectories(recoveryFile.getParent());
                copyAndValidate(normalizedDestination, recoveryFile, gson);
            }

            moveReplace(candidate, normalizedDestination);
        } finally {
            Files.deleteIfExists(candidate);
        }
    }

    public static Path recoveryFile(Path projectPath) throws IOException {
        return recoveryFileFor(projectPath.toAbsolutePath().normalize());
    }

    public static void saveRecoverySnapshot(Path projectPath, String jsonFileName, String projectJson,
                                            List<Path> projectFiles, Gson gson) throws IOException {
        Path recoveryFile = recoveryFile(projectPath);
        Files.createDirectories(recoveryFile.getParent());
        Path candidate = Files.createTempFile(recoveryFile.getParent(), ".emrick-autosave-", ".tmp");
        try {
            writeArchive(candidate, jsonFileName, projectJson, projectFiles);
            validateArchive(candidate, gson);
            moveReplace(candidate, recoveryFile);
        } finally {
            Files.deleteIfExists(candidate);
        }
    }

    public static Path getRecoveryDirectory() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
        Path userHome = Path.of(System.getProperty("user.home"));

        if (osName.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            return (localAppData == null || localAppData.isBlank()
                    ? userHome.resolve("AppData").resolve("Local")
                    : Path.of(localAppData))
                    .resolve("Emrick Designer").resolve(RECOVERY_DIRECTORY);
        }
        if (osName.contains("mac")) {
            return userHome.resolve("Library").resolve("Application Support")
                    .resolve("Emrick Designer").resolve(RECOVERY_DIRECTORY);
        }

        String xdgStateHome = System.getenv("XDG_STATE_HOME");
        Path stateHome = xdgStateHome == null || xdgStateHome.isBlank()
                ? userHome.resolve(".local").resolve("state")
                : Path.of(xdgStateHome);
        return stateHome.resolve("emrick-designer").resolve("recovery");
    }

    public static void validateProjectArchive(Path archive, Gson gson) throws IOException {
        validateArchive(archive, gson);
    }

    private static Path recoveryFileFor(Path destination) throws IOException {
        return getRecoveryDirectory().resolve(sha256(destination.toString()) + ".emrick");
    }

    private static void writeArchive(Path archive, String jsonFileName, String projectJson,
                                     List<Path> projectFiles) throws IOException {
        if (!jsonFileName.endsWith(".json")) {
            throw new IOException("Project data must be written as a JSON archive entry.");
        }

        Set<String> entryNames = new HashSet<>();
        entryNames.add(jsonFileName);
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zipOutput = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zipOutput.putNextEntry(new ZipEntry(jsonFileName));
            zipOutput.write(projectJson.getBytes(StandardCharsets.UTF_8));
            zipOutput.closeEntry();

            for (Path projectFile : projectFiles) {
                if (!Files.isRegularFile(projectFile)) {
                    continue;
                }
                String entryName = projectFile.getFileName().toString();
                if (!entryNames.add(entryName)) {
                    throw new IOException("Project archive contains duplicate file name: " + entryName);
                }
                zipOutput.putNextEntry(new ZipEntry(entryName));
                Files.copy(projectFile, zipOutput);
                zipOutput.closeEntry();
            }
        }
    }

    private static void copyAndValidate(Path source, Path recoveryFile, Gson gson) throws IOException {
        Path recoveryCandidate = Files.createTempFile(recoveryFile.getParent(), ".emrick-recovery-", ".tmp");
        try {
            Files.copy(source, recoveryCandidate, StandardCopyOption.REPLACE_EXISTING);
            validateArchive(recoveryCandidate, gson);
            moveReplace(recoveryCandidate, recoveryFile);
        } finally {
            Files.deleteIfExists(recoveryCandidate);
        }
    }

    private static void validateArchive(Path archive, Gson gson) throws IOException {
        String projectJson = null;
        int jsonCount = 0;
        try (ZipFile zipFile = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
                    jsonCount++;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                        projectJson = reader.lines().reduce("", (left, right) -> left + right);
                    }
                }
            }
        }
        if (jsonCount != 1 || projectJson == null) {
            throw new IOException("Project archive must contain exactly one JSON project file.");
        }
        try {
            ProjectFile projectFile = gson.fromJson(projectJson, ProjectFile.class);
            if (projectFile == null || projectFile.drill == null || projectFile.archiveNames == null) {
                throw new IOException("Project JSON does not contain a complete project.");
            }
        } catch (RuntimeException exception) {
            throw new IOException("Project archive JSON could not be read.", exception);
        }
    }

    private static void moveReplace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(String value) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte currentByte : digest) {
                builder.append(String.format("%02x", currentByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is not available for recovery storage.", exception);
        }
    }
}