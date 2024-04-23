package org.emrick.project.actions;

import org.emrick.project.*;

public class CreateEffectAction implements UndoableAction {

    private final Effect effect;
    private final Performer performer;

    public CreateEffectAction(Effect effect, Performer performer) {
        this.effect = effect;
        this.performer = performer;
    }

    @Override
    public void execute() {
        performer.getEffects().add(effect);
    }

    @Override
    public void undo() {
        performer.getEffects().remove(effect);
    }

    @Override
    public void redo() {
//        if (!performer.getEffects().contains(effect)) { // Do we need this check?
        execute();
//        }
    }
}