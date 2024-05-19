package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

import java.util.*;

public class ReplaceEffectsAction implements UndoableAction {
    private final ArrayList<EffectPerformerMap> map;

    public ReplaceEffectsAction(ArrayList<EffectPerformerMap> map) {
        this.map = map;
    }

    @Override
    public void execute() {
        int indexToReplace = -1;
        for (EffectPerformerMap m : map) {
            for (Effect effect : m.getPerformer().getEffects()) {
                int index = m.getPerformer().getEffects().indexOf(effect);
                if (Objects.equals(m.getOldEffect(), effect)) {
                    indexToReplace = index;
                }
            }

            if (indexToReplace != -1) {
                m.getPerformer().getEffects().set(indexToReplace, m.getEffect());
            }
        }
    }

    @Override
    public void undo() {
        for (EffectPerformerMap m : map) {
            int indexToReplace = -1;

            for (Effect effect : m.getPerformer().getEffects()) {
                int index = m.getPerformer().getEffects().indexOf(effect);
                if (Objects.equals(m.getEffect(), effect)) {
                    indexToReplace = index;
                }
            }

            if (indexToReplace != -1) {
                m.getPerformer().getEffects().set(indexToReplace, m.getOldEffect());
            }
        }
    }

    @Override
    public void redo() {
        execute();
    }
}