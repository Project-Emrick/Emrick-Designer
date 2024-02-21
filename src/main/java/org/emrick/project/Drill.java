package org.emrick.project;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class Drill {
    public ArrayList<Performer> performers;
    public ArrayList<Coordinate> coordinates;
    public ArrayList<Set> sets;
    public Drill() {
        performers = new ArrayList<>();
        coordinates = new ArrayList<>();
        sets = new ArrayList<>();
    }

    public void saveDrill(String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename + "Performers.json");
            deconstructPerformers();
            Type perfType = new TypeToken<ArrayList<Performer>>(){}.getType();
            fos.write(new Gson().toJson(performers, perfType).getBytes());
            fos.flush();
            fos.close();
            fos = new FileOutputStream(filename + "Coordinates.json");
            Type coordType = new TypeToken<ArrayList<Coordinate>>(){}.getType();
            fos.write(new Gson().toJson(coordinates, coordType).getBytes());
            fos.flush();
            fos.close();
        }
        catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void deconstructPerformers() {
        for (Performer p : performers) {
            p.deconstructCoordinates();
        }
    }

    public void loadAllPerformers() {
        for (Performer p : performers) {
            p.loadCoordinates(coordinates);
        }

    }

    public void loadSets() {
        ArrayList<Coordinate> c = performers.get(0).getCoordinates();
        sets = new ArrayList<>();
        for (int i = 0; i < c.size(); i++) {
            sets.add(new Set(c.get(i).set, i, c.get(i).duration));
        }
    }

    public void addSet(Coordinate coordinate) {
        coordinates.add(coordinate);
    }

    public void addPerformer(Performer performer) {
        performers.add(performer);
    }


    /**
     * Retrieves the positions of all performers at a specific timestamp.
     * @param timestamp The specific timestamp to query.
     * @return A list of strings representing the performers and their positions at the timestamp.
     */
    public ArrayList<String> getPositionsAtTimestamp(String timestamp) {
        ArrayList<String> results = new ArrayList<>();
        for (Performer performer : this.performers) {
            for (Coordinate coordinate : performer.getCoordinates()) {
                if (coordinate.getSet().equals(timestamp)) {
                    String result = String.format("(Performer %s%s, %f, %f)",
                            performer.getSymbol(), performer.getLabel(), coordinate.getX(), coordinate.getY());
                    results.add(result);
                    break; // Assuming only one coordinate per performer per timestamp
                }
            }
        }
        return results;
    }

    public String toString() {
        String out = "";
        for (Performer p : performers) {
            out += p;
        }
        return out;
    }
}
