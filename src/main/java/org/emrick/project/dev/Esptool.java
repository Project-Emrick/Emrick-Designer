package org.emrick.project.dev;

import org.emrick.project.PathConverter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Locates (or downloads once) Espressif's esptool and uses it to flash receivers.
 *
 * Order of preference: the esptool bundled with an existing PlatformIO install (no download), then a
 * standalone esptool previously downloaded into the Emrick Designer folder, then a one-time download of the
 * standalone build from Espressif's GitHub releases. Downloads are pinned to a version and checked against
 * Espressif's published SHA-256 digests.
 */
public final class Esptool {
    private static final String VERSION = "v4.12.0";
    private static final String RELEASE_URL = "https://github.com/espressif/esptool/releases/download/" + VERSION + "/";

    /** Standalone esptool builds for each platform: asset name and its published SHA-256. */
    private record Asset(String name, String sha256) {
    }

    private static final Asset WINDOWS = new Asset("esptool-" + VERSION + "-windows-amd64.zip",
            "42fddc5e6a05716868ad77fb43acbf53be041f97abed87ff850df1dc88140889");
    private static final Asset LINUX_AMD64 = new Asset("esptool-" + VERSION + "-linux-amd64.tar.gz",
            "fd92bfde850baa20c4090457b317c767df22878600a567237b092f9b151a5161");
    private static final Asset LINUX_ARM64 = new Asset("esptool-" + VERSION + "-linux-aarch64.tar.gz",
            "33499dd910187ea94cfb908851bcf8212a6a06dd9271f6f2109e5fb80f243a42");
    private static final Asset MAC_ARM64 = new Asset("esptool-" + VERSION + "-macos-arm64.tar.gz",
            "b3ec710bcae20e93eaa18af4c50084feda8bdbbfd0b5d165b956a29f7c6ff8c3");
    private static final Asset MAC_AMD64 = new Asset("esptool-" + VERSION + "-macos-amd64.tar.gz",
            "c884e37652ee7057ae905eadd108383b608cda8bc5ab03369156802cdf1128df");

    private static final Pattern PROGRESS = Pattern.compile("\\((\\d+) ?%\\)");

    private static List<String> cachedCommand;

    private Esptool() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * @param status receives short progress messages (e.g. while downloading)
     * @return the command prefix that runs esptool (e.g. [path/esptool.exe] or [python, esptool.py])
     */
    public static synchronized List<String> command(Consumer<String> status) throws IOException {
        if (cachedCommand != null) {
            return cachedCommand;
        }
        List<String> pio = fromPlatformIO();
        if (pio != null) {
            cachedCommand = pio;
            return pio;
        }
        File dir = new File(PathConverter.pathConverter("tools/esptool-" + VERSION + "/", false));
        File exe = findExecutable(dir);
        if (exe == null) {
            download(dir, status);
            exe = findExecutable(dir);
            if (exe == null) {
                throw new IOException("esptool download did not contain an esptool executable");
            }
        }
        cachedCommand = List.of(exe.getAbsolutePath());
        return cachedCommand;
    }

    /** @return the esptool that ships with PlatformIO, if PlatformIO is installed */
    private static List<String> fromPlatformIO() {
        Path home = Path.of(System.getProperty("user.home"), ".platformio");
        File script = home.resolve("packages/tool-esptoolpy/esptool.py").toFile();
        File python = isWindows()
                ? home.resolve("penv/Scripts/python.exe").toFile()
                : home.resolve("penv/bin/python").toFile();
        if (script.isFile() && python.isFile()) {
            List<String> cmd = List.of(python.getAbsolutePath(), script.getAbsolutePath());
            // A PlatformIO install can be missing esptool's Python modules (e.g. intelhex), in which case
            // it can't run at all. Only use it if it starts; otherwise fall back to the standalone build.
            try {
                List<String> check = new ArrayList<>(cmd);
                check.add("version");
                Process p = new ProcessBuilder(check).redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                if (p.waitFor() == 0) {
                    return cmd;
                }
            } catch (IOException e) {
                // fall through to the standalone build
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    private static File findExecutable(File dir) {
        if (!dir.isDirectory()) {
            return null;
        }
        String name = isWindows() ? "esptool.exe" : "esptool";
        try (var stream = Files.walk(dir.toPath())) {
            return stream.map(Path::toFile)
                    .filter(f -> f.isFile() && f.getName().equals(name))
                    .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static Asset assetForPlatform() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        if (os.contains("win")) {
            return WINDOWS;
        } else if (os.contains("mac")) {
            return arm ? MAC_ARM64 : MAC_AMD64;
        } else if (os.contains("linux")) {
            return arm ? LINUX_ARM64 : LINUX_AMD64;
        }
        throw new IOException("No esptool build available for " + os + " " + arch);
    }

    private static void download(File dir, Consumer<String> status) throws IOException {
        Asset asset = assetForPlatform();
        Files.createDirectories(dir.toPath());
        File archive = new File(dir.getParentFile(), asset.name());
        status.accept("Downloading flashing tool (one time, about 50 MB)...");
        FirmwareRelease.downloadFile(RELEASE_URL + asset.name(), archive);
        try {
            if (!FirmwareRelease.sha256(archive).equalsIgnoreCase(asset.sha256())) {
                throw new IOException("esptool download failed its checksum check");
            }
            status.accept("Unpacking flashing tool...");
            if (asset.name().endsWith(".zip")) {
                unzip(archive, dir);
            } else {
                Process p = new ProcessBuilder("tar", "-xzf", archive.getAbsolutePath(), "-C", dir.getAbsolutePath())
                        .redirectErrorStream(true).start();
                if (p.waitFor() != 0) {
                    throw new IOException("Couldn't unpack esptool: " + new String(p.getInputStream().readAllBytes()));
                }
            }
            File exe = findExecutable(dir);
            if (exe != null && !isWindows()) {
                exe.setExecutable(true);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while unpacking esptool", e);
        } catch (IOException e) {
            FirmwareRelease.deleteRecursively(dir);
            throw e;
        } finally {
            archive.delete();
        }
    }

    private static void unzip(File zip, File dir) throws IOException {
        Path root = dir.toPath().toAbsolutePath().normalize();
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip.toPath())))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) {
                    throw new IOException("Unsafe path in esptool archive: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Flashes a release to one receiver. Only the app, bootloader, and partition table regions are written,
     * so the unit's stored data (board ID, label, show) is kept.
     *
     * @param progress receives 0-100 for the firmware image
     * @param log      receives every line esptool prints
     * @return true if esptool reported success
     */
    public static boolean flash(String systemPortName, FirmwareRelease release,
                                Consumer<Integer> progress, Consumer<String> log) throws IOException {
        List<String> cmd = new ArrayList<>(command(log));
        cmd.addAll(List.of("--chip", "esp32s3", "--port", portPath(systemPortName), "--baud", "921600",
                "--before", "default_reset", "--after", "hard_reset", "write_flash"));
        for (FirmwareRelease.Part part : release.parts()) {
            cmd.add(String.format("0x%x", part.offset));
            cmd.add(release.file(part).getAbsolutePath());
        }
        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();

        // Progress is weighted by size; the firmware image is nearly all of the bytes
        List<FirmwareRelease.Part> parts = release.parts();
        long[] sizes = parts.stream().mapToLong(p -> release.file(p).length()).toArray();
        long totalBytes = java.util.Arrays.stream(sizes).sum();
        long doneBytes = 0;

        // esptool redraws its progress with '\r', so split on both line endings
        int partIndex = 0;
        boolean sawHashVerified = false;
        try (InputStream in = process.getInputStream()) {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\r' || b == '\n') {
                    String text = line.toString(StandardCharsets.UTF_8).trim();
                    line.reset();
                    if (text.isEmpty()) {
                        continue;
                    }
                    log.accept(text);
                    Matcher m = PROGRESS.matcher(text);
                    if (text.startsWith("Writing at") && m.find() && partIndex < sizes.length) {
                        long current = sizes[partIndex] * Integer.parseInt(m.group(1)) / 100;
                        progress.accept((int) Math.min(99, (doneBytes + current) * 100 / Math.max(1, totalBytes)));
                    } else if (text.startsWith("Hash of data verified") && partIndex < sizes.length) {
                        doneBytes += sizes[partIndex];
                        partIndex++;
                        sawHashVerified = true;
                        progress.accept((int) Math.min(99, doneBytes * 100 / Math.max(1, totalBytes)));
                    }
                } else {
                    line.write(b);
                }
            }
        }
        try {
            int exit = process.waitFor();
            boolean ok = exit == 0 && sawHashVerified;
            if (ok) {
                progress.accept(100);
            }
            return ok;
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** esptool wants a device path on macOS/Linux (e.g. /dev/cu.usbserial-0001) and the plain name on Windows. */
    private static String portPath(String systemPortName) {
        if (isWindows() || systemPortName.startsWith("/")) {
            return systemPortName;
        }
        return "/dev/" + systemPortName;
    }
}
