package org.emrick.project;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.emrick.project.actions.LEDConfig;

public class CSVLEDWriter {

    private static final int DEFAULT_HEIGHT = 12;
    private static final int DEFAULT_WIDTH = 6;
    private static final int DEFAULT_H_OFFSET_L = 1;
    private static final int DEFAULT_H_OFFSET_R = -6;
    private static final int DEFAULT_V_OFFSET = -6;

    private static final String LEFT = "L";
    private static final String RIGHT = "R";

    private static final String DEFAULT_FILE_PATH = "show_data/ledinfo.csv";

    private static final String[] HEADERS = {
        "Performer Label",
        "LED ID",
        "LED Label",
        "LED Count",
        "Height",
        "Width",
        "Horizontal Offset",
        "Vertical Offset"
    };
    
    private final String delimiter;

    public CSVLEDWriter() {
        this(",");
    }

    public CSVLEDWriter(String delimiter) {
        this.delimiter = delimiter;
    }

    public static File createDefaultCSV(List<Performer> performers) throws IOException {
        return createDefaultCSV(performers, DEFAULT_FILE_PATH);
    }

    public static File createDefaultCSV(List<Performer> performers, String filePath) throws IOException {
        String path = PathConverter.pathConverter(filePath, false);
        
        CSVLEDWriter writer = new CSVLEDWriter();
        writer.write(path, performers);

        System.out.println("Created default CSV file at: " + path);

        return new File(path);
    }

    public void write(String path, List<Performer> performers) throws IOException {
        int ledStripCount = 0;

        // count the total number of LED strips
        // to include at the top of the CSV file
        for (Performer currPerformer : performers) {
            if (!currPerformer.isLeftOnly()) {
                ledStripCount++;
            }

            ledStripCount++;
        }

        // sort performers based on section and label
        // see Performer.compareTo() for more information
        Collections.sort(performers);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writeHeaderRow(writer, ledStripCount);

            int currPerformerId = 0;
            int currLEDStripId  = 0;

            for (Performer currPerformer : performers) {
                currPerformer.setPerformerID(currPerformerId++);

                String performerLabel = currPerformer.getIdentifier();
                writePerformerRow(writer, performerLabel);

                int ledCount = currPerformer.getLEDCount();

                LEDConfig leftLED = new LEDConfig(ledCount, DEFAULT_HEIGHT,
                                                  DEFAULT_WIDTH, DEFAULT_H_OFFSET_L,
                                                  DEFAULT_V_OFFSET, LEFT);
                writeLEDRow(writer, currLEDStripId++, performerLabel, leftLED);

                if (currPerformer.isLeftOnly()) {
                    continue;
                }

                LEDConfig rightLED = new LEDConfig(ledCount, DEFAULT_HEIGHT,
                                                   DEFAULT_WIDTH, DEFAULT_H_OFFSET_R,
                                                   DEFAULT_V_OFFSET, RIGHT);
                writeLEDRow(writer, currLEDStripId++, performerLabel, rightLED);
            }
        }
    }

    private void writeHeaderRow(BufferedWriter writer, int size) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (String currHeader : HEADERS) {
            sb.append(currHeader);
            sb.append(delimiter);
        }

        sb.append(delimiter + "Size:" + delimiter);
        sb.append(size);

        writer.write(sb.toString());
        writer.newLine();
    }

    private void writePerformerRow(BufferedWriter writer, String performerLabel) throws IOException {
        writer.write(performerLabel);
        writer.newLine();
    }

    private void writeLEDRow(BufferedWriter writer, int ledId, String performerLabel, LEDConfig ledConfig) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append(delimiter);
        sb.append(String.valueOf(ledId));

        sb.append(delimiter);
        sb.append(performerLabel + ledConfig.getLabel());

        sb.append(delimiter);
        sb.append(ledConfig.getLEDCount());

        sb.append(delimiter);
        sb.append(ledConfig.getHeight());

        sb.append(delimiter);
        sb.append(ledConfig.getWidth());

        sb.append(delimiter);
        sb.append(ledConfig.gethOffset());

        sb.append(delimiter);
        sb.append(ledConfig.getvOffset());

        writer.write(sb.toString());
        writer.newLine();
    }

}
