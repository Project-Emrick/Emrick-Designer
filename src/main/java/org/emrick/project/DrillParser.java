package org.emrick.project;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class DrillParser {
    /**
     * Takes in a pdf filename and returns the raw text from the document
     * @param fileName  PDF file path
     * @return          Raw text from the pdf
     */
    public static String extractText(String fileName) {
        File f = new File(fileName);
        String text = "";
        try {
            PDDocument document = Loader.loadPDF(f);
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
            document.close();
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
        return text;
    }

    /**
     * Takes in a file name, reads from the 2 files with that name
     * and creates a drill object from the files
     * @param filename      Prefix for the 2 files
     *                      (for input "test" files would be
     *                      testPerformers.json and testCoordinates.json)
     * @return              Drill object
     * TODO: Modify this function and Drill.saveDrill to write a single compressed rather than 2 files
     */
    public static Drill importDrill(String filename) {
        Drill drill = new Drill();
        try {
            FileInputStream fis = new FileInputStream(filename + "Performers.json");
            Type perfType = new TypeToken<ArrayList<Performer>>(){}.getType();
            drill.performers = new Gson().fromJson(new String(fis.readAllBytes()), perfType);
            fis.close();
            fis = new FileInputStream(filename + "Coordinates.json");
            Type coordType = new TypeToken<ArrayList<Coordinate>>(){}.getType();
            drill.coordinates = new Gson().fromJson(new String(fis.readAllBytes()), coordType);
            fis.close();
            drill.loadAllPerformers();
        }
        catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return drill;
    }

    /**
     * Takes in the text from an entire drill file and returns the
     * data from this file in a single drill object
     * @param text      raw text drill data
     * @return          Drill object
     */
    public static Drill parseWholeDrill(String text) {
        Drill drill = new Drill();
        String[] performerCharts = text.split("Set");
        performerCharts[0] = "";
        for (String t : performerCharts) {
            if (!t.equals("")) {
                drill.addPerformer(parseDrill(t, drill));
            }
        }
        return drill;
    }

    /**
     * Takes in the raw text data for the drill of a single performer,
     * loads the coordinated into the drill object, and returns a
     * performer object.
     * @param text      Raw text drill data for a single performer
     * @param drill     Drill object
     * @return          newly created Performer object
     */
    public static Performer parseDrill(String text, Drill drill) {
        String symbol = text.split("Symbol: ")[1].split(" ")[0];
        int label = Integer.parseInt(text.split("Label: ")[1].split(" ")[0]);
        Performer performer = new Performer(symbol, label);
        String[] lines = text.split("\n");
        for (int i = 1; i < lines.length-2; i++) {
            String set = lines[i].split(" ")[0];
            int duration;
            if (lines[i].split(" ")[4].equals("")) {
                duration = Integer.parseInt(lines[i].split(" ")[5]);
            } else {
                duration = Integer.parseInt(lines[i].split(" ")[4]);
            }
            double x = getX(lines[i]);
            double y = getY(lines[i]);
            Coordinate c = new Coordinate(x, y, set, duration, symbol + label);
            drill.addSet(c);
            performer.addSet(c);
        }
        return performer;
    }

    /**
     * Takes in a line of drill data and returns the specified
     * x-coordinate
     * @param line      1 line of raw text drill data
     * @return          x-coordinate
     */
    private static double getX(String line) {
        double x;
        String xLine = line.split(" yd ln")[0];
        if (!line.contains("On 50 yd ln")) {
            String[] tmp = xLine.split(" ");
            x = 50 - (double) Integer.parseInt(tmp[tmp.length-1]);
            if (xLine.contains("Side 1")) {
                x *= -1;
            }
            if (!xLine.contains("On")) {
                double offset = Double.parseDouble(xLine.split(": ")[1].split(" ")[0]);
                if (xLine.contains("inside")) {
                    offset *= -1;
                }
                x += offset;
            }
        } else {
            x = 0.0;
        }
        return x;
    }

    /**
     * Takes in a line of drill data and returns the specified
     * y-coordinate
     * @param line      1 line of raw text drill data
     * @return          y-coordinate
     */
    private static double getY(String line) {
        double y;
        String yLine = line.split("yd ln ")[1];
        while (yLine.charAt(0) == ' ') {
            yLine = yLine.substring(1);
        }
        if (yLine.contains("side")) {
            if (yLine.contains("Back")) {
                y = 84.0;
            } else {
                y = 0.0;
            }
        } else {
            if (yLine.contains("Back")) {
                y = 52.0;
            } else {
                y = 32.0;
            }
        }
        if (!yLine.contains("On")) {
            double offset = Double.parseDouble(yLine.split(" ")[0]);
            if (yLine.contains("in front")) {
                offset *= -1;
            }
            y += offset;
        }
        return y;
    }
}
