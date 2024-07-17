package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

public class RemoveEffectAction implements UndoableAction {

    private final Effect effect;
    private final LEDStrip ledStrip;

    public RemoveEffectAction(Effect effect, LEDStrip ledStrip) {
        this.effect = effect;
        this.ledStrip = ledStrip;
    }

    @Override
    public void execute() {
        ledStrip.getEffects().remove(effect);
    }

    @Override
    public void undo() {
        ledStrip.getEffects().add(effect);
    }

    @Override
    public void redo() {
        execute();
    }
}