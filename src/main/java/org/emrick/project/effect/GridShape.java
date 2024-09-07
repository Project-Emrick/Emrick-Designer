package org.emrick.project.effect;

import org.emrick.project.LEDStrip;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class GridShape {
    private boolean[][] shape;
    private Point startPos;
    private Color color;
    private int speed;
    private Point movement = new Point(0, 0);
    private HashSet<LEDStrip> ledStrips;
    private String recoveryString = "";

    public GridShape() {
        this.shape = new boolean[1][1];
        this.startPos = new Point(0, 0);
        this.color = Color.BLACK;
        this.ledStrips = new HashSet<>();
        this.speed = 1;
        this.movement = new Point(0, 0);
    }

    public GridShape(boolean[][] shape, Point startPos, int speed, Color color) {
        this.shape = shape;
        this.startPos = startPos;
        this.color = color;
        this.speed = speed;
        ledStrips = new HashSet<>();
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String generateRecoveryString() {
        Iterator<LEDStrip> ledStripIterator = ledStrips.iterator();
        String str = Integer.toString(ledStripIterator.next().getId());
        while (ledStripIterator.hasNext()) {
            str += "," + ledStripIterator.next().getId();
        }
        recoveryString = str;
        return str;
    }

    public void setRecoveryString(String recoveryString) {
        this.recoveryString = recoveryString;
    }

    public void recoverLEDStrips(ArrayList<LEDStrip> strips) {
        ArrayList<String> ids = new ArrayList<>(List.of(recoveryString.split(",")));
        for (int i = 0; i < strips.size(); i++) {
            if (ids.contains(Integer.toString(strips.get(i).getId()))) {
                ledStrips.add(strips.get(i));
            }
        }
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
}
