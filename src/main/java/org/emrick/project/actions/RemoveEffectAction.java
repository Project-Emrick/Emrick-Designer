package org.emrick.project.actions;

import org.emrick.project.*;

public class RemoveEffectAction implements UndoableAction {

    private final Effect effect;
    private final Performer performer;

    public RemoveEffectAction(Effect effect, Performer performer) {
        this.effect = effect;
        this.performer = performer;
    }

    @Override
    public void execute() {
        performer.getEffects().remove(effect);
    }

    @Override
    public void undo() {
        performer.getEffects().add(effect);
    }

    @Override
    public void redo() {
        execute();
    }
}