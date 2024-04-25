package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

import java.util.*;

public class CreateEffectsAction implements UndoableAction {

    private final Effect effect;
    private final List<Performer> performers;

    public CreateEffectsAction(Effect effect, List<Performer> performers) {
        this.effect = effect;
        this.performers = new ArrayList<>(performers);
    }

    @Override
    public void execute() {
        //performers.getEffects().add(effect);
        performers.forEach(performer -> performer.getEffects().add(effect));
    }

    @Override
    public void undo() {
        //performers.getEffects().remove(effect);
        performers.forEach(performer -> performer.getEffects().remove(effect));
    }

    @Override
    public void redo() {
//        if (!performers.getEffects().contains(effect)) {
//            execute();
//        }
        execute();
    }
}