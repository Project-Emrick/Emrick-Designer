package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

import java.util.*;

public class ReplaceEffectAction implements UndoableAction {

    private final Effect oldEffect;
    private final Effect newEffect;
    private final Performer performer;

    public ReplaceEffectAction(Effect oldEffect, Effect newEffect, Performer performer) {
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
        this.performer = performer;
    }

    @Override
    public void execute() {
        int indexToReplace = -1;

        for (Effect effect : performer.getEffects()) {
            int index = performer.getEffects().indexOf(effect);
            if (Objects.equals(oldEffect, effect)) {
                indexToReplace = index;
            }
        }

        if (indexToReplace != -1) {
            performer.getEffects().set(indexToReplace, newEffect);
        }
    }

    @Override
    public void undo() {
        int indexToReplace = -1;

        for (Effect effect : performer.getEffects()) {
            int index = performer.getEffects().indexOf(effect);
            if (Objects.equals(newEffect, effect)) {
                indexToReplace = index;
            }
        }

        if (indexToReplace != -1) {
            performer.getEffects().set(indexToReplace, oldEffect);
        }
    }

    @Override
    public void redo() {
        execute();
    }
}