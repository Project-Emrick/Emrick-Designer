package org.emrick.project;

import org.emrick.project.effect.Effect;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class Performer {

    public Point2D currentLocation;
    private String symbol;
    private int label;
    private ArrayList<Coordinate> coordinates;
    private ArrayList<Effect> effects;
    private String deviceId;
    //private ArrayList<LEDStrip> ledStrips;

    public Performer() {
        symbol = "";
        label = 0;
        coordinates = new ArrayList<>();
        //ledStrips = new ArrayList<>();
        currentLocation = new Point2D.Double(0, 0);
        this.effects = new ArrayList<>();
    }

    public Performer(String symbol, int label, int id) {
        this.symbol = symbol;
        this.label = label;
        //ledStrips = new ArrayList<>();
        coordinates = new ArrayList<>();
        currentLocation = new Point2D.Double(0, 0);
        this.effects = new ArrayList<>();
        this.deviceId = Integer.toString(id);
    }

    public void sortEffects() {
        effects.sort(Comparator.comparingLong(Effect::getStartTimeMSec));
    }

    String getDeviceId() {
        return deviceId;
    }

    void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void loadCoordinates(ArrayList<Coordinate> coordinates) {
        this.coordinates = new ArrayList<>();
        for (Coordinate c : coordinates) {
            if (c.id.equals(this.toString())) {
                this.coordinates.add(c);
            }
        }
    }

    public ArrayList<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(ArrayList<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }

    public Coordinate getCoordinateFromSet(String set) {
        for (Coordinate c : coordinates) {
            if (c.set.equals(set)) {
                return c;
            }
        }
        return null;
    }

    public ArrayList<Effect> getEffects() {
        return effects;
    }

    public void addSet(Coordinate coordinate) {
        coordinates.add(coordinate);
    }

    /**
     * MUST BE CALLED BEFORE WRITING PERFORMERS TO FILE
     */
    public void deconstructCoordinates() {
        coordinates = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Performer performer = (Performer) o;
        return label == performer.label && Objects.equals(symbol, performer.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, label);
    }

    public String toString() {
        String out = symbol + label + "\r\n";
        for (Effect e  : effects) {
            out += "\t" + e.toString() + "\r\n";
        }
        return out;
    }

    public String getIdentifier() {
        return getSymbol() + getLabel();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

}