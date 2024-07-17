package org.emrick.project.actions;

import java.util.*;

public class CreateEffectsAction implements UndoableAction {

    private final ArrayList<EffectLEDStripMap> effects;

    public CreateEffectsAction(ArrayList<EffectLEDStripMap> effects) {
        this.effects = effects;

    }

    @Override
    public void execute() {
        //performers.getEffects().add(effect);
        effects.forEach(map -> map.getLedStrip().getEffects().add(map.getEffect()));
    }

    @Override
    public void undo() {
        //performers.getEffects().remove(effect);
        effects.forEach(map -> map.getLedStrip().getEffects().remove(map.getEffect()));
    }

    @Override
    public void redo() {
        execute();
    }
}