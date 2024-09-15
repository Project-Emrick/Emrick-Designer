package org.emrick.project;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * This class handles importing the zipped Pyware archive (.3dz) contents into the project for use.
 *
 * This means unzipping the specified archive file, reading 'package.ini' within the archive, and retrieving
 * the necessary files (e.g., audio, surface, ground).
 */
public class ImportArchive {

    private final ImportListener importListener;

    public ImportArchive(ImportListener importListener) {
        this.importListener = importListener;
    }

    public void fullImport(ArrayList<File> archiveFiles, String drillSrc) {
        importListener.onBeginImport();

        ArrayList<String> absoluteArchivePaths = new ArrayList<>();
        for (File f : archiveFiles) {
            absoluteArchivePaths.add(f.getAbsolutePath());
        }
        ArrayList<File> copyArchiveFiles = new ArrayList<>();
        for (String s : absoluteArchivePaths) {
            copyArchiveFiles.add(new File(s));
        }

        // ! NOTE ! Assume Working Directory is Emrick-Designer/
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        // Unzip into application resources/unzip/ subfolder. Within "AppData" on Windows, "Applications" on Mac

        ArrayList<String> fileNamesNoExt = new ArrayList<>();

        for (File f : copyArchiveFiles) {
            fileNamesNoExt.add(f.getName().replaceFirst("[.][^.]+$", ""));
        }

        ArrayList<String> unzipPaths = new ArrayList<>();

        for (String s : fileNamesNoExt) {
            unzipPaths.add(PathConverter.pathConverter("show_data/" + s, false));
        }
        //continue here
        Unzip.unzip(absoluteArchivePaths, unzipPaths);

        // Parse package.ini file
        ArrayList<File> iniFile = new ArrayList<>();
        for (String s : unzipPaths) {
            iniFile.add(new File(s + File.separator + "package.ini"));
        }
        Map<String, Map<String, String>> iniData = new HashMap<>();
        for (File f : iniFile) {
            try {
                Scanner iniReader = new Scanner(f);
                String currentSection = null;
                while (iniReader.hasNextLine()) {
                    String line = iniReader.nextLine().trim();
                    // System.out.println(line);

                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                        continue;
                    }

                    // Section headers
                    if (line.startsWith("[") && line.endsWith("]")) {
                        currentSection = line.substring(1, line.length() - 1).trim();
                        iniData.putIfAbsent(currentSection, new HashMap<>());
                    }
                    // Key-value pairs
                    else if (line.contains("=") && currentSection != null) {
                        String[] parts = line.split("=", 2);
                        String key = parts[0].trim();
                        String value = parts.length > 1 ? parts[1].trim() : ""; // value may be empty
                        iniData.get(currentSection).put(key, value);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // See package.ini. Import available files
        //  Current support:  audio
        for (Map.Entry<String, String> entry : iniData.get("Files").entrySet()) {

            // File missing
            if (entry.getValue().isEmpty()) {
                continue;
            }
            ArrayList<String> componentPaths = new ArrayList<>();
            for (String s : unzipPaths) {
                componentPaths.add(s + "/" + entry.getValue());
            }

            // General-purpose callback
            importListener.onImport();

            // Import audio
            if (entry.getKey().equals("audio")) {
                importAudio(componentPaths);
            }
        }

        // Import drill
        if (drillSrc != null) {
            importDrill(drillSrc);
        }
    }

    private void importAudio(ArrayList<String> paths) {
        ArrayList<File> audioFiles = new ArrayList<>();
        for (String path : paths) {
            System.out.println("Importing audio..." + path);
            audioFiles.add(new File(path));
        }
        importListener.onAudioImport(audioFiles);
    }

    private void importDrill(String path) {
        System.out.println("Importing drill..." + path);
        importListener.onDrillImport(path);
    }

    public void concatImport(ArrayList<File> archiveSrc, String drillSrc) {

        // ! NOTE ! Assume Working Directory is Emrick-Designer/
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        ArrayList<String> archivePaths = new ArrayList<>();
        for (File f : archiveSrc) {
            archivePaths.add(f.getAbsolutePath());
        }
        // Unzip into application resources/unzip/ subfolder. Within "AppData" on Windows, "Applications" on Mac
        ArrayList<File> copyArchiveSrc = new ArrayList<>();
        for (String s : archivePaths) {
            copyArchiveSrc.add(new File(s));
        }

        ArrayList<String> fileNameNoExt = new ArrayList<>();
        for (File f : copyArchiveSrc) {
            fileNameNoExt.add(f.getName().replaceFirst("[.][^.]+$", ""));
        }

        ArrayList<String> unzipPaths = new ArrayList<>();
        for (String s : fileNameNoExt) {
            unzipPaths.add(PathConverter.pathConverter("show_data/" + s, false));
        }

        Unzip.unzip(archivePaths, unzipPaths);

        // Parse package.ini file
        ArrayList<File> iniFile = new ArrayList<>();
        for (String s : unzipPaths) {
            iniFile.add(new File(s + File.separator + "package.ini"));
        }
        Map<String, Map<String, String>> iniData = new HashMap<>();

        for (File f : iniFile) {
            try {
                Scanner iniReader = new Scanner(f);
                String currentSection = null;
                while (iniReader.hasNextLine()) {
                    String line = iniReader.nextLine().trim();
                    // System.out.println(line);

                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                        continue;
                    }

                    // Section headers
                    if (line.startsWith("[") && line.endsWith("]")) {
                        currentSection = line.substring(1, line.length() - 1).trim();
                        iniData.putIfAbsent(currentSection, new HashMap<>());
                    }
                    // Key-value pairs
                    else if (line.contains("=") && currentSection != null) {
                        String[] parts = line.split("=", 2);
                        String key = parts[0].trim();
                        String value = parts.length > 1 ? parts[1].trim() : ""; // value may be empty
                        iniData.get(currentSection).put(key, value);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // See package.ini. Import available files
        //  Current support:  audio
        for (Map.Entry<String, String> entry : iniData.get("Files").entrySet()) {

            // File missing
            if (entry.getValue().isEmpty()) {
                continue;
            }
            ArrayList<String> componentPaths = new ArrayList<>();
            for (String s : unzipPaths) {
                componentPaths.add(s + "/" + entry.getValue());
            }

            // General-purpose callback
            importListener.onImport();

            // Import audio
            if (entry.getKey().equals("audio")) {
                concatImportAudio(componentPaths);
            }
        }

        // Import drill
        if (drillSrc != null) {
            importDrill(drillSrc);
        }
    }
    private void concatImportAudio(ArrayList<String> paths) {
        ArrayList<File> importFiles = new ArrayList<>();
        for (String s : paths) {
            System.out.println("Importing audio..." + s);
            importFiles.add(new File(s));
        }
        importListener.onConcatAudioImport(importFiles);
    }
}
