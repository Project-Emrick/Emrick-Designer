package org.emrick.project;

import org.emrick.project.actions.LEDConfig;
import org.emrick.project.effect.Effect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class LEDStrip {
    private int id;
    private ArrayList<Effect> effects;
    private Performer performer;
    private int performerID;
    private LEDConfig ledConfig;

    public LEDStrip(int id,  Performer performer, LEDConfig ledConfig) {
        this.id = id;
        this.effects = new ArrayList<>();
        this.performer = performer;
        this.ledConfig = ledConfig;
        performerID = performer.getPerformerID();
    }

    public LEDConfig getLedConfig() {
        return ledConfig;
    }

    public void setLedConfig(LEDConfig ledConfig) {
        this.ledConfig = ledConfig;
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

    public String getLabel() {
        return performer.getIdentifier() + ledConfig.getLabel();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<Effect> getEffects() {
        return effects;
    }

    public void setEffects(ArrayList<Effect> effects) {
        this.effects = effects;
    }
    public void addEffect(Effect e) {
        this.effects.add(e);
    }

    public void addEffect(Effect effect) {
        this.effects.add(effect);
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
        return "LEDStrip: " + getLabel() + ", " + id;
    }
}
