package org.emrick.project;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

        // Unzip archive into resources/unzip/ with same archive name w/o .3dz extension.
        File archiveFile = new File(archiveSrc);
        String fileNameNoExt = archiveFile.getName().replaceFirst("[.][^.]+$", "");
        String unzipPath = System.getProperty("user.home") + "/AppData/Local/Emrick Designer/src/main/resources/unzip/" + fileNameNoExt;
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
            String componentPath = unzipPath + "/" + entry.getValue();

            // General-purpose callback
            importListener.onImport();

            // Import floorCover
            if (entry.getKey().equals("floorCover")) {
                importFloorCover(componentPath);
            }

            // Import surface
            else if (entry.getKey().equals("surface")) {
                importSurface(componentPath);
            }

            // Import audio
            else if (entry.getKey().equals("audio")) {
                importAudio(componentPath);
            }
        }

        // Import drill
        importDrill(drillSrc);
    }

    private void importFloorCover(String path) {
        System.out.println("Importing floor cover..." + path);
        importListener.onFloorCoverImport(loadImage(path));
    }

    private void importSurface(String path) {
        System.out.println("Importing surface..." + path);
        BufferedImage image = (BufferedImage) loadImage(path);
        // Write cropped image to file?
        //  Crop obtained through trial and error, may change.
        BufferedImage cropped = image.getSubimage(1102, 578, 2196, 1157);
        importListener.onSurfaceImport(cropped);
    }

    private void importAudio(String path) {
        System.out.println("Importing audio..." + path);
        importListener.onAudioImport(new File(path));
    }

    private void importDrill(String path) {
        System.out.println("Importing drill..." + path);
        importListener.onDrillImport(path);
    }

    // Return Image object for images, e.g., field floorCover, surface
    public static Image loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("ImportArchive.loadImage() " + e.getMessage());
        }
        return null;
    }

    // For Testing
//    public static void main(String[] args) {
//        ImportListener importListener = new ImportListener() {
//            @Override
//            public void onImport() {
//                System.out.println("onImport called.");
//            }
//
//            @Override
//            public void onFloorCoverImport(Image image) {
//                System.out.println("onFloorCoverImport called.");
//            }
//
//            @Override
//            public void onSurfaceImport(Image image) {
//                System.out.println("onSurfaceImport called.");
//            }
//
//            @Override
//            public void onAudioImport(File audioFile) {
//                System.out.println("onAudioImport called.");
//            }
//
//            @Override
//            public void onDrillImport(String drill) {
//                System.out.println("onDrillImport called.");
//            }
//        };
//
//        ImportArchive importArchive = new ImportArchive(importListener);
//        importArchive.fullImport("./src/test/java/org/emrick/project/Purdue23-1-1aint_no_mountain_high_enough.3dz", "./src/test/java/org/emrick/project/DrillExample.pdf");
//    }
}
