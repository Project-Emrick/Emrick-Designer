package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

import java.util.*;
import java.util.Set;

public class CreateEffectsAction implements UndoableAction {

    private final ArrayList<EffectPerformerMap> effects;

    public CreateEffectsAction(ArrayList<EffectPerformerMap> effects) {
        this.effects = effects;

    }

    @Override
    public void execute() {
        //performers.getEffects().add(effect);
        effects.forEach(map -> map.getPerformer().getEffects().add(map.getEffect()));
    }

    @Override
    public void undo() {
        //performers.getEffects().remove(effect);
        effects.forEach(map -> map.getPerformer().getEffects().remove(map.getEffect()));
    }

    @Override
    public void redo() {
//        if (!performers.getEffects().contains(effect)) {
//            execute();
//        }
        execute();
    }
}