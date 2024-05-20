package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectGUI;
import org.emrick.project.effect.EffectManager;

import java.util.*;

public class ReplaceEffectsAction implements UndoableAction {
    private final ArrayList<EffectPerformerMap> map;
    private boolean undone = false;

    public ReplaceEffectsAction(ArrayList<EffectPerformerMap> map) {
        this.map = map;
    }

    @Override
    public void execute() {
        // TODO: Rewrite to remove all effects that match id and then add new effects at the index of first old match
        if (map.get(0).getEffect().getEffectType() != EffectGUI.WAVE && undone) {
            for (EffectPerformerMap m : map) {
                Effect tmp = m.getEffect();
                m.setEffect(m.getOldEffect());
                m.setOldEffect(tmp);
            }
        }
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
        int[] addIndexes = new int[performers.size()];
        int index = 0;
        for (Performer p : performers) {

            Effect removeEffect = null;
            for (int i = 0; i < p.getEffects().size(); i++) {
                if (p.getEffects().get(i).getId() == map.get(0).getEffect().getId()) {
                    addIndexes[index] = i;
                    removeEffect = p.getEffects().get(i);
                    break;
                }
            }
            while (p.getEffects().remove(removeEffect)){}

            index++;
        }
        for (EffectPerformerMap m : map) {
            int i = performers.indexOf(m.getPerformer());
            m.getPerformer().getEffects().add(addIndexes[i], m.getEffect());
            addIndexes[i]++;
        }
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

        ArrayList<EffectPerformerMap> undoMap;
        if (map.get(0).getEffect().getEffectType() == EffectGUI.WAVE) {
            undoMap = EffectManager.generateWaveEffect(performers, map.get(0).getOldEffect());
        } else {
            undoMap = map;
            for (EffectPerformerMap m : undoMap) {
                Effect tmp = m.getEffect();
                m.setEffect(m.getOldEffect());
                m.setOldEffect(tmp);
            }
        }

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
        undone = true;
    }

    @Override
    public void redo() {
        execute();
    }
}