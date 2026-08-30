package org.emrick.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.emrick.project.serde.ColorAdapter;
import org.emrick.project.serde.DurationAdapter;
import org.emrick.project.serde.GeneratedEffectAdapter;
import org.emrick.project.serde.JButtonAdapter;
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
import java.util.zip.ZipFile;

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
}