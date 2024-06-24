package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectGUI;
import org.emrick.project.effect.EffectManager;

import java.util.*;

public class ReplaceEffectsAction implements UndoableAction {
    private final ArrayList<EffectPerformerMap> map;

    public ReplaceEffectsAction(ArrayList<EffectPerformerMap> map) {
        this.map = map;
    }

    @Override
    public void execute() {
        // TODO: Rewrite to remove all effects that match id and then add new effects at the index of first old match
        ArrayList<Performer> performers = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            if (performers.size() > 0) {
                if (!map.get(i-1).getPerformer().equals(map.get(i).getPerformer())) {
                    performers.add(map.get(i).getPerformer());
                }
            } else {
                performers.add(map.get(i).getPerformer());
            }
        }
        addEffects(performers, map);
    }

    @Override
    public void undo() {
        ArrayList<Performer> performers = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            if (performers.size() > 0) {
                if (!map.get(i-1).getPerformer().equals(map.get(i).getPerformer())) {
                    performers.add(map.get(i).getPerformer());
                }
            } else {
                performers.add(map.get(i).getPerformer());
            }
        }

        ArrayList<EffectPerformerMap> undoMap = map.get(0).getOldEffect().getGeneratedEffect().generateEffects(performers);

        addEffects(performers, undoMap);
    }

    private void addEffects(ArrayList<Performer> performers, ArrayList<EffectPerformerMap> undoMap) {
        int[] addIndexes = new int[performers.size()];
        int index = 0;
        for (Performer p : performers) {

            Effect removeEffect = null;
            for (int i = 0; i < p.getEffects().size(); i++) {
                if (p.getEffects().get(i).getId() == undoMap.get(0).getEffect().getId()) {
                    addIndexes[index] = i;
                    removeEffect = p.getEffects().get(i);
                    break;
                }
            }
            while (p.getEffects().remove(removeEffect)){}

            index++;
        }
        for (EffectPerformerMap m : undoMap) {
            int i = performers.indexOf(m.getPerformer());
            m.getPerformer().getEffects().add(addIndexes[i], m.getEffect());
            addIndexes[i]++;
        }
    }

    @Override
    public void redo() {
        execute();
    }
}