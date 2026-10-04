package org.emrick.project.dev;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.emrick.project.PathConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * The Heltec Receiver firmware published from the firmware repo's main branch to the Emrick Flash site
 * (the same files the web flasher uses). Releases are downloaded into the user's Emrick Designer folder
 * (firmware/receiver/&lt;commit&gt;/) so a previously downloaded version can still be flashed without internet.
 */
public class FirmwareRelease {
    public static final String MANIFEST_URL = "https://project-emrick.github.io/Emrick-Flash/receiver/manifest.json";

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static FirmwareRelease latestKnown;
    private static String lastProblem;

    /** manifest.json, in esp-web-tools format plus commit/date/sha256 */
    public static class Manifest {
        public String name;
        public String version;
        public String commit;
        public String date;
        public List<Build> builds;
    }

    public static class Build {
        public String chipFamily;
        public List<Part> parts;
    }

    public static class Part {
        public String path;
        public int offset;
        public String sha256;
    }

    private final Manifest manifest;
    private final File directory;
    private final boolean fromCacheOnly;

    private FirmwareRelease(Manifest manifest, File directory, boolean fromCacheOnly) {
        this.manifest = manifest;
        this.directory = directory;
        this.fromCacheOnly = fromCacheOnly;
    }

    public String commit() {
        return manifest.commit;
    }

    public String date() {
        return manifest.date;
    }

    /** @return e.g. "abc1234 (10-03-2026)" */
    public String version() {
        return manifest.commit + " (" + DevUI.date(manifest.date) + ")";
    }

    /** @return true when the latest release couldn't be checked and this is the newest downloaded one */
    public boolean isFromCacheOnly() {
        return fromCacheOnly;
    }

    public List<Part> parts() {
        return manifest.builds.get(0).parts;
    }

    public File file(Part part) {
        return new File(directory, part.path);
    }

    /** @return the most recent release found by {@link #fetchLatest} this session (or the newest cached one) */
    public static synchronized FirmwareRelease latestKnown() {
        if (latestKnown == null) {
            latestKnown = newestCached();
        }
        return latestKnown;
    }

    private static File cacheRoot() {
        return new File(PathConverter.pathConverter("firmware/receiver/", false));
    }

    /**
     * Checks the flash site for the latest release and downloads it if it isn't cached yet. If the site
     * can't be reached, falls back to the newest release downloaded before.
     *
     * @return the release to flash
     * @throws IOException if the site can't be reached and nothing was ever downloaded
     */
    public static FirmwareRelease fetchLatest() throws IOException {
        Manifest manifest;
        try {
            HttpResponse<String> response = HTTP.send(
                    HttpRequest.newBuilder(URI.create(MANIFEST_URL)).timeout(Duration.ofSeconds(15))
                            .header("Cache-Control", "no-cache").GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("Flash site returned HTTP " + response.statusCode());
            }
            manifest = GSON.fromJson(response.body(), Manifest.class);
            validate(manifest);
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            return cachedFallback(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return cachedFallback(new IOException("Interrupted"));
        }

        File dir = new File(cacheRoot(), manifest.commit);
        if (!isComplete(dir, manifest)) {
            try {
                download(manifest, dir);
            } catch (IOException e) {
                return cachedFallback(e);
            }
        }
        FirmwareRelease release = new FirmwareRelease(manifest, dir, false);
        synchronized (FirmwareRelease.class) {
            latestKnown = release;
            lastProblem = null;
        }
        return release;
    }

    /** @return why the last check for the latest firmware failed, in plain words, or null if it succeeded */
    public static synchronized String lastProblem() {
        return lastProblem;
    }

    private static String describe(Exception cause) {
        String msg = String.valueOf(cause.getMessage());
        if (msg.contains("HTTP 404")) {
            return "The Emrick Flash site isn't published yet. It appears after the firmware repo's first build on main.";
        }
        if (cause instanceof java.net.ConnectException || cause instanceof java.net.UnknownHostException
                || cause instanceof java.net.http.HttpTimeoutException || msg.contains("UnresolvedAddress")) {
            return "No internet connection, or the Emrick Flash site didn't respond.";
        }
        if (msg.startsWith("Checksum mismatch")) {
            return "The download was corrupted. Try again.";
        }
        return msg;
    }

    private static FirmwareRelease cachedFallback(Exception cause) throws IOException {
        synchronized (FirmwareRelease.class) {
            lastProblem = describe(cause);
        }
        FirmwareRelease cached = newestCached();
        if (cached == null) {
            throw new IOException("No receiver firmware is available on this computer yet.\n\n" + lastProblem()
                    + "\n\nOnce the firmware has been downloaded one time, it can be flashed without internet.", cause);
        }
        FirmwareRelease offline = new FirmwareRelease(cached.manifest, cached.directory, true);
        synchronized (FirmwareRelease.class) {
            latestKnown = offline;
        }
        return offline;
    }

    /** @return the cached release with the newest date, or null if nothing complete is cached */
    private static FirmwareRelease newestCached() {
        File[] dirs = cacheRoot().listFiles(File::isDirectory);
        if (dirs == null) {
            return null;
        }
        FirmwareRelease newest = null;
        for (File dir : dirs) {
            try {
                Manifest m = GSON.fromJson(Files.readString(new File(dir, "manifest.json").toPath()), Manifest.class);
                validate(m);
                if (isComplete(dir, m) && (newest == null || m.date.compareTo(newest.manifest.date) > 0)) {
                    newest = new FirmwareRelease(m, dir, true);
                }
            } catch (IOException | JsonSyntaxException | IllegalStateException ignored) {
                // Incomplete or damaged download; skip it
            }
        }
        return newest;
    }

    private static void validate(Manifest m) {
        if (m == null || m.commit == null || m.date == null || m.builds == null || m.builds.isEmpty()
                || m.builds.get(0).parts == null || m.builds.get(0).parts.isEmpty()) {
            throw new IllegalStateException("Firmware manifest is missing required fields");
        }
        for (Part part : m.builds.get(0).parts) {
            // Only plain file names, so a bad manifest can't write outside the cache folder
            if (part.path == null || !part.path.matches("[A-Za-z0-9_.-]+") || part.sha256 == null) {
                throw new IllegalStateException("Firmware manifest has an invalid part");
            }
        }
    }

    private static boolean isComplete(File dir, Manifest m) {
        if (!new File(dir, "manifest.json").isFile()) {
            return false;
        }
        for (Part part : m.builds.get(0).parts) {
            File f = new File(dir, part.path);
            try {
                if (!f.isFile() || !sha256(f).equalsIgnoreCase(part.sha256)) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    /** Downloads every part into a temporary folder, verifies checksums, then moves it into place. */
    private static void download(Manifest manifest, File dir) throws IOException {
        String base = MANIFEST_URL.substring(0, MANIFEST_URL.lastIndexOf('/') + 1);
        File tmp = new File(cacheRoot(), manifest.commit + ".partial");
        deleteRecursively(tmp);
        Files.createDirectories(tmp.toPath());
        try {
            for (Part part : manifest.builds.get(0).parts) {
                File dest = new File(tmp, part.path);
                downloadFile(base + part.path, dest);
                if (!sha256(dest).equalsIgnoreCase(part.sha256)) {
                    throw new IOException("Checksum mismatch for " + part.path + "; download was corrupted");
                }
            }
            Files.writeString(new File(tmp, "manifest.json").toPath(), GSON.toJson(manifest));
            deleteRecursively(dir);
            Files.move(tmp.toPath(), dir.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } finally {
            deleteRecursively(tmp);
        }
    }

    static void downloadFile(String url, File dest) throws IOException {
        try {
            HttpResponse<InputStream> response = HTTP.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " downloading " + url);
            }
            try (InputStream in = response.body()) {
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    static String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file.toPath())) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    digest.update(buf, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static void deleteRecursively(File f) {
        if (!f.exists()) {
            return;
        }
        File[] children = f.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        f.delete();
    }
}
