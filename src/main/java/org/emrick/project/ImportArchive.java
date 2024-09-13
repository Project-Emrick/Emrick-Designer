package org.emrick.project;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
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

    public void fullImport(String archiveSrc, String drillSrc) {
        importListener.onBeginImport();

        // ! NOTE ! Assume Working Directory is Emrick-Designer/
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        // Unzip into application resources/unzip/ subfolder. Within "AppData" on Windows, "Applications" on Mac
        File archiveFile = new File(archiveSrc);
        String fileNameNoExt = archiveFile.getName().replaceFirst("[.][^.]+$", "");
        String unzipPath = PathConverter.pathConverter("show_data/" + fileNameNoExt, false);

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
        //  Current support:  audio
        for (Map.Entry<String, String> entry : iniData.get("Files").entrySet()) {

            // File missing
            if (entry.getValue().isEmpty()) {
                continue;
            }
            String componentPath = unzipPath + "/" + entry.getValue();

            // General-purpose callback
            importListener.onImport();

            // Import audio
            if (entry.getKey().equals("audio")) {
                importAudio(componentPath);
            }
        }

        // Import drill
        if (drillSrc != null) {
            importDrill(drillSrc);
        }
    }

    private void importAudio(String path) {
        System.out.println("Importing audio..." + path);
        importListener.onAudioImport(new File(path));
    }

    private void importDrill(String path) {
        System.out.println("Importing drill..." + path);
        importListener.onDrillImport(path);
    }
}
