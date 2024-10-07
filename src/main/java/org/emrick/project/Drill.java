package org.emrick.project;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Drill {
    public ArrayList<Performer> performers;
    public ArrayList<Coordinate> coordinates;
    public ArrayList<Set> sets;
    public ArrayList<LEDStrip> ledStrips;
    public Drill() {
        performers = new ArrayList<>();
        coordinates = new ArrayList<>();
        sets = new ArrayList<>();
        ledStrips = new ArrayList<>();
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
        Collections.sort(sets, new Comparator<Set>() {
            @Override
            public int compare(Set o1, Set o2) {
                return o1.compareTo(o2);
            }
        });
        for (int i = 0; i < sets.size(); i++) {
            sets.get(i).index = i;
        }
    }

    public ArrayList<LEDStrip> getLEDStrips() {
        return ledStrips;
    }

    public void addSet(Coordinate coordinate) {
        coordinates.add(coordinate);
    }

    public void addLedStrip(LEDStrip ledStrip) {
        ledStrips.add(ledStrip);
    }

    public void addPerformer(Performer performer) {
        performers.add(performer);
    }
    public ArrayList<LEDStrip> getLedStrips() {
        return this.ledStrips;
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
