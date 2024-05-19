package org.emrick.project.actions;

import org.emrick.project.effect.Effect;
import org.emrick.project.Performer;

import java.util.ArrayList;
import java.util.List;

public class RemoveEffectsAction implements UndoableAction {
    private final ArrayList<EffectPerformerMap> map;

    public RemoveEffectsAction(ArrayList<EffectPerformerMap> map) {
        this.map = map;
    }

    @Override
    public void execute() {
        map.forEach(m -> m.getPerformer().getEffects().remove(m.getEffect()));
    }

    @Override
    public void undo() {
        map.forEach(m -> m.getPerformer().getEffects().add(m.getEffect()));
    }

    @Override
    public void redo() {
        execute();
    }
}
