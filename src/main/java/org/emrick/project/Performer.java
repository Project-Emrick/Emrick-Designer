package org.emrick.project;

import org.emrick.project.effect.Effect;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Performer {
    private Color color;
    private String symbol;
    private int label;
    private ArrayList<Coordinate> coordinates;
    private ArrayList<Effect> effects;
    public Point2D currentLocation;

    public Performer() {
        symbol = "";
        label = 0;
        coordinates = new ArrayList<>();
        currentLocation = new Point2D.Double(0,0);
        this.color = Color.BLACK;
        this.effects = new ArrayList<>();
    }
    public Performer(String symbol, int label) {
        this.color = Color.BLACK;
        this.symbol = symbol;
        this.label = label;
        coordinates = new ArrayList<>();
        currentLocation = new Point2D.Double(0,0);
        this.effects = new ArrayList<>();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setColor(Color color){
        this.color = color;
    }

    public Color getColor(){
        return color;
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

    public void setCoordinates(ArrayList<Coordinate> coordinates) {
        this.coordinates = coordinates;
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

    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            if (this.toString().equals(obj.toString())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String toString() {
        String out = symbol + label + "\r\n";
        for (Coordinate c : coordinates) {
            out += c.toString() + "\r\n";
        }
        return out;
    }

    public String getIdentifier() {
        return getSymbol() + getLabel();
    }

}
