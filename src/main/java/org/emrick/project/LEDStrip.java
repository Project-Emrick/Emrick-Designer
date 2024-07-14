package org.emrick.project;

import org.emrick.project.effect.Effect;

import java.util.ArrayList;
import java.util.Comparator;

public class LEDStrip {
    private int id;
    private int LEDCount;
    private String label;
    private ArrayList<Effect> effects;

    public LEDStrip(int id, int LEDCount, String label, ArrayList<Effect> effects) {
        this.id = id;
        this.LEDCount = LEDCount;
        this.label = label;
        this.effects = effects;
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
