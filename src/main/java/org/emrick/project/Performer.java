package org.emrick.project;

import java.awt.geom.*;
import java.util.*;

public class Performer {

    public Point2D currentLocation;
    private String symbol;
    private int label;
    private ArrayList<Coordinate> coordinates;
    private int performerID;
    private ArrayList<Integer> ledStrips;

    public Performer() {
        symbol = "";
        label = 0;
        coordinates = new ArrayList<>();
        ledStrips = new ArrayList<>();
        currentLocation = new Point2D.Double(0, 0);
    }

    public Performer(String symbol, int label, int id) {
        this.symbol = symbol;
        this.label = label;
        ledStrips = new ArrayList<>();
        coordinates = new ArrayList<>();
        currentLocation = new Point2D.Double(0, 0);
        this.performerID = id;
    }

    public ArrayList<Integer> getLedStrips() {
        return ledStrips;
    }

    public void setLedStrips(ArrayList<Integer> ledStrips) {
        this.ledStrips = ledStrips;
    }

    public void addLEDStrip(int ledStrip) {
        ledStrips.add(ledStrip);
    }


    public int getPerformerID() {
        return performerID;
    }

    void setPerformerID(int performerID) {
        this.performerID = performerID;
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

    public void addSet(Coordinate coordinate) {
        coordinates.add(coordinate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Performer performer = (Performer) o;
        return performerID == performer.getPerformerID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, label);
    }

    public String toString() {
        String out = symbol + label + "\r\n";
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