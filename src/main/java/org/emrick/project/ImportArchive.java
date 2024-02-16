package org.emrick.project;

import java.io.File;
import java.io.FileNotFoundException;
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

    public static void fullImport(String archiveSrc) {

        // ! NOTE ! Assume Working Directory is Emrick-Designer/
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        // Unzip archive into resources/unzip/ with same archive name w/o .3dz extension.
        File archiveFile = new File(archiveSrc);
        String fileNameNoExt = archiveFile.getName().replaceFirst("[.][^.]+$", "");
        String unzipPath = "./src/main/resources/unzip/" + fileNameNoExt;
        Unzip.unzip(archiveSrc, unzipPath);

        // Parse package.ini file
        File iniFile = new File(unzipPath + File.separator + "package.ini");
        Map<String, Map<String, String>> iniData = new HashMap<>();

        try {
            Scanner iniReader = new Scanner(iniFile);
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

        // See package.ini. Import available files
        //  Current support: floorCover, audio
        for (Map.Entry<String, String> entry : iniData.get("Files").entrySet()) {

            // File missing
            if (entry.getValue().isEmpty()) {
                continue;
            }

            String componentPath = unzipPath + File.separator + entry.getValue();

            // Import floorCover
            if (entry.getKey().equals("floorCover")) {
                importFloorCover(componentPath);
            }
            // Import audio
            else if (entry.getKey().equals("audio")) {
                importAudio(componentPath);
            }
        }
    }

    private static void importFloorCover(String path) {
        System.out.println("Importing floor cover..." + path);
    }

    private static void importAudio(String path) {
        System.out.println("Importing audio..." + path);
    }

    // For Testing
    public static void main(String[] args) {

        ImportArchive.fullImport("./src/test/java/org/emrick/project/Purdue23-1-1aint_no_mountain_high_enough.3dz");
    }
}
