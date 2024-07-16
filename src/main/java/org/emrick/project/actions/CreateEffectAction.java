package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

public class CreateEffectAction implements UndoableAction {

    private final Effect effect;
    private final LEDStrip ledStrip;

    public CreateEffectAction(Effect effect, LEDStrip ledStrip) {
        this.effect = effect;
        this.ledStrip = ledStrip;
    }

    @Override
    public void execute() {
        ledStrip.getEffects().add(effect);
    }

    @Override
    public void undo() {
        ledStrip.getEffects().remove(effect);
    }

    @Override
    public void redo() {
//        if (!performer.getEffects().contains(effect)) { // Do we need this check?
        execute();
//        }
    }
}