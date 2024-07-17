package org.emrick.project;

import org.emrick.project.effect.Effect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class LEDStrip {
    private int id;
    private int LEDCount;
    private String label;
    private ArrayList<Effect> effects;
    private Performer performer;
    private int performerID;
    private int hOffset;
    private int vOffset;
    private int height;
    private int width;

    public LEDStrip(int id, int LEDCount, String label, Performer performer, int hOffset, int vOffset, int height, int width) {
        this.id = id;
        this.LEDCount = LEDCount;
        this.label = label;
        this.effects = new ArrayList<>();
        this.performer = performer;
        performerID = performer.getPerformerID();
        this.hOffset = hOffset;
        this.vOffset = vOffset;
        this.height = height;
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int gethOffset() {
        return hOffset;
    }

    public void sethOffset(int hOffset) {
        this.hOffset = hOffset;
    }

    public int getvOffset() {
        return vOffset;
    }

    public void setvOffset(int vOffset) {
        this.vOffset = vOffset;
    }

    public int getPerformerID() {
        return performerID;
    }

    public void setPerformerID(int performerID) {
        this.performerID = performerID;
    }

    public Performer getPerformer() {
        return performer;
    }

    public void setPerformer(Performer performer) {
        this.performer = performer;
        if (performer != null) {
            performerID = performer.getPerformerID();
        }
    }

    public void sortEffects() {
        effects.sort(Comparator.comparingLong(Effect::getStartTimeMSec));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLEDCount() {
        return LEDCount;
    }

    public void setLEDCount(int LEDCount) {
        this.LEDCount = LEDCount;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ArrayList<Effect> getEffects() {
        return effects;
    }

    public void setEffects(ArrayList<Effect> effects) {
        this.effects = effects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LEDStrip ledStrip)) return false;
        return id == ledStrip.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "LEDStrip: " + label + ", " + id;
    }
}
