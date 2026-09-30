package org.emrick.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.emrick.project.serde.ColorAdapter;
import org.emrick.project.serde.DurationAdapter;
import org.emrick.project.serde.GeneratedEffectAdapter;
import org.emrick.project.serde.JButtonAdapter;
import org.emrick.project.serde.OldProjectFile;
import org.emrick.project.serde.PairAdapter;
import org.emrick.project.serde.Point2DAdapter;
import org.emrick.project.serde.ProjectFile;
import org.emrick.project.effect.GeneratedEffect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectPersistenceTest {
    @TempDir
    Path temporaryDirectory;

    private Gson createProjectGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Color.class, new ColorAdapter())
                .registerTypeAdapter(Point2D.class, new Point2DAdapter())
                .registerTypeAdapter(SyncTimeGUI.Pair.class, new PairAdapter())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .registerTypeAdapter(GeneratedEffect.class, new GeneratedEffectAdapter())
                .registerTypeAdapter(JButton.class, new JButtonAdapter())
                .serializeNulls()
                .create();
    }

    @Test
    void savesAReadableProjectArchive() throws Exception {
        Gson gson = createProjectGson();
        ProjectFile projectFile = new ProjectFile(
            new Drill(), new ArrayList<>(), null, 0f, null, null, new ArrayList<>());
        Path asset = temporaryDirectory.resolve("show.3dz");
        Path destination = temporaryDirectory.resolve("show.emrick");
        Files.writeString(asset, "asset data", StandardCharsets.UTF_8);

        ProjectPersistence.save(destination, "show.json", gson.toJson(projectFile), List.of(asset), gson);

        assertTrue(Files.isRegularFile(destination));
        try (ZipFile archive = new ZipFile(destination.toFile())) {
            assertEquals(2, archive.size());
            assertTrue(archive.getEntry("show.json") != null);
            assertTrue(archive.getEntry("show.3dz") != null);
        }
    }

    @Test
    void leavesExistingProjectUntouchedWhenCandidateValidationFails() throws Exception {
        Gson gson = createProjectGson();
        Path destination = temporaryDirectory.resolve("show.emrick");
        byte[] originalContents = "last known good project".getBytes(StandardCharsets.UTF_8);
        Files.write(destination, originalContents);

        assertThrows(IOException.class, () -> ProjectPersistence.save(
                destination, "show.json", "not valid json", List.of(), gson));

        assertEquals("last known good project", Files.readString(destination, StandardCharsets.UTF_8));
    }

    private Path writeLegacyProject(Gson gson, String fileName) throws IOException {
        OldProjectFile legacy = new OldProjectFile(
            new Drill(), "show.3dz", null, 0f, null, null, new ArrayList<>());
        Path archive = temporaryDirectory.resolve(fileName);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("show.json"));
            zip.write(gson.toJson(legacy).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("show.3dz"));
            zip.write("asset data".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return archive;
    }

    @Test
    void acceptsLegacySingleArchiveProjects() throws Exception {
        Gson gson = createProjectGson();
        Path legacy = writeLegacyProject(gson, "legacy.emrick");

        assertDoesNotThrow(() -> ProjectPersistence.validateProjectArchive(legacy, gson));
    }

    @Test
    void savesOverALegacyProjectInTheCurrentFormat() throws Exception {
        Gson gson = createProjectGson();
        Path destination = writeLegacyProject(gson, "legacy.emrick");
        ArrayList<String> archiveNames = new ArrayList<>(List.of("show.3dz"));
        ProjectFile upgraded = new ProjectFile(
            new Drill(), archiveNames, null, 0f, null, null, new ArrayList<>());

        ProjectPersistence.save(destination, "show.json", gson.toJson(upgraded), List.of(), gson);

        try (ZipFile archive = new ZipFile(destination.toFile())) {
            String json = new String(archive.getInputStream(archive.getEntry("show.json")).readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(archiveNames, gson.fromJson(json, ProjectFile.class).archiveNames);
        }
        // The pre-save backup lands in the real per-user Recovery folder; don't leave test copies there
        Path recoveryCopy = ProjectPersistence.recoveryFile(destination);
        try {
            assertTrue(Files.isRegularFile(recoveryCopy));
        } finally {
            Files.deleteIfExists(recoveryCopy);
        }
    }

    @Test
    void rejectsTruncatedProjectJson() throws Exception {
        Gson gson = createProjectGson();
        ProjectFile projectFile = new ProjectFile(
            new Drill(), new ArrayList<>(List.of("show.3dz")), null, 0f, null, null, new ArrayList<>());
        String json = gson.toJson(projectFile);
        Path archive = temporaryDirectory.resolve("truncated.emrick");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("show.json"));
            zip.write(json.substring(0, json.length() - 10).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(IOException.class, () -> ProjectPersistence.validateProjectArchive(archive, gson));
    }

    @Test
    void rejectsProjectsWithoutAnyArchiveReference() throws Exception {
        Gson gson = createProjectGson();
        Path archive = temporaryDirectory.resolve("broken.emrick");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("show.json"));
            zip.write("{\"drill\": {}}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertThrows(IOException.class, () -> ProjectPersistence.validateProjectArchive(archive, gson));
    }
}