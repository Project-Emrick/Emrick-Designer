package org.emrick.project;

import org.emrick.project.effect.Effect;

import java.util.ArrayList;
import java.util.Comparator;

public class LEDStrip {
    private int id;
    private int LEDCount;
    private String label;
    private ArrayList<Effect> effects;
    private Performer performer;
    private int performerID;
    private boolean vertical;
    private int hOffset;
    private int vOffset;

    public LEDStrip(int id, int LEDCount, String label, Performer performer, boolean vertical, int hOffset, int vOffset) {
        this.id = id;
        this.LEDCount = LEDCount;
        this.label = label;
        this.effects = new ArrayList<>();
        this.performer = performer;
        performerID = performer.getPerformerID();
        this.vertical = vertical;
        this.hOffset = hOffset;
        this.vOffset = vOffset;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
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
}
