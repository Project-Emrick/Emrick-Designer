package org.emrick.project.effect;

import org.emrick.project.LEDStrip;

import java.awt.*;
import java.util.HashSet;

public class GridShape {
    private boolean[][] shape;
    private Point startPos;
    private Point endPos;
    private Color color;
    private Point movement = new Point(0, 0);
    private HashSet<LEDStrip> ledStrips;

    public GridShape(boolean[][] shape, Point startPos, Point endPos, Color color) {
        this.shape = shape;
        this.startPos = startPos;
        this.endPos = endPos;
        this.color = color;
        ledStrips = new HashSet<>();
    }

    public HashSet<LEDStrip> getLedStrips() {
        return ledStrips;
    }

    public void setLedStrips(HashSet<LEDStrip> ledStrips) {
        this.ledStrips = ledStrips;
    }

    public Point getMovement() {
        return movement;
    }

    public void setMovement(Point movement) {
        this.movement = movement;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean[][] getShape() {
        return shape;
    }

    public void setShape(boolean[][] shape) {
        this.shape = shape;
    }

    public Point getStartPos() {
        return startPos;
    }

    public void setStartPos(Point startPos) {
        this.startPos = startPos;
    }

    public Point getEndPos() {
        return endPos;
    }

    public void setEndPos(Point endPos) {
        this.endPos = endPos;
    }
}
