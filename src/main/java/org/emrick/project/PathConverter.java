package org.emrick.project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathConverter {

    public static String pathConverter(String path, boolean system) {
        if (!system) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                return System.getProperty("user.home") + "/AppData/Local/Emrick Designer/" + path;
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                return System.getProperty("user.home") + "/Desktop/emrick-designer/" + path;
            } else {
                return "/Applications/Emrick Designer.app/Contents/" + path;
            }
        } else {
            String installedPath;
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                installedPath = System.getenv("PROGRAMFILES") + "/Emrick Designer/" + path;
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                installedPath = "/opt/emrick-designer/" + path;
            } else {
                installedPath = "/Applications/Emrick Designer.app/Contents/" + path;
            }

            File installedFile = new File(installedPath);
            if (installedFile.exists()) {
                return installedPath;
            }

            // Development fallback: allow resources to load when running from source.
            String resourcePath = path.startsWith("res/") ? path.substring(4) : path;
            Path cwd = Paths.get(System.getProperty("user.dir"));
            Path[] candidates = new Path[] {
                    cwd.resolve("src/main/resources").resolve(resourcePath),
                    cwd.resolve("build/resources/main").resolve(resourcePath),
                    cwd.resolve("resources").resolve(resourcePath),
                    cwd.resolve(resourcePath)
            };

            for (Path candidate : candidates) {
                if (candidate.toFile().exists()) {
                    return candidate.toString();
                }
            }

            return installedPath;
        }
    }
}
