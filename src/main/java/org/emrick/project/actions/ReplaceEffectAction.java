package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

import java.util.*;

public class ReplaceEffectAction implements UndoableAction {

    private final Effect oldEffect;
    private final Effect newEffect;
    private final LEDStrip ledStrip;

    public ReplaceEffectAction(Effect oldEffect, Effect newEffect, LEDStrip ledStrip) {
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
        this.ledStrip = ledStrip;
    }

    @Override
    public void execute() {
        int indexToReplace = -1;

        for (Effect effect : ledStrip.getEffects()) {
            int index = ledStrip.getEffects().indexOf(effect);
            if (Objects.equals(oldEffect, effect)) {
                indexToReplace = index;
            }
        }

        if (indexToReplace != -1) {
            ledStrip.getEffects().set(indexToReplace, newEffect);
        }
    }

    @Override
    public void undo() {
        int indexToReplace = -1;

        for (Effect effect : ledStrip.getEffects()) {
            int index = ledStrip.getEffects().indexOf(effect);
            if (Objects.equals(newEffect, effect)) {
                indexToReplace = index;
            }
        }

        if (indexToReplace != -1) {
            ledStrip.getEffects().set(indexToReplace, oldEffect);
        }
    }

    @Override
    public void redo() {
        execute();
    }
}