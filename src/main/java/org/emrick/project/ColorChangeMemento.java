package org.emrick.project;

import java.awt.*;

/**
 * Stores the state of a performer's coordinate color change
 */
class ColorChangeMemento {

    private final Coordinate coordinate;
    private final Color newColor;
    private final Color oldColor;

    public ColorChangeMemento(Coordinate coordinate, Color newColor, Color oldColor) {
        this.coordinate = coordinate;
        this.newColor = newColor;
        this.oldColor = oldColor;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public Color getNewColor() {
        return newColor;
    }

    public Color getOldColor() {
        return oldColor;
    }
}