package org.emrick.project.actions;

import java.util.ArrayList;

public class RemoveEffectsAction implements UndoableAction {
    private final ArrayList<EffectLEDStripMap> map;

    public RemoveEffectsAction(ArrayList<EffectLEDStripMap> map) {
        this.map = map;
    }

    @Override
    public void execute() {
        map.forEach(m -> {
            while(m.getLedStrip().getEffects().remove(m.getEffect())){}
        });
    }

    @Override
    public void undo() {
        map.forEach(m -> m.getLedStrip().getEffects().add(m.getEffect()));
    }

    @Override
    public void redo() {
        execute();
    }
}
