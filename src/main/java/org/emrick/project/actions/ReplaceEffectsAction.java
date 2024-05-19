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
        for (EffectPerformerMap m : map) {
            if (!performers.contains(m.getPerformer())) {
                performers.add(m.getPerformer());
            }
        }

        for (Performer p : performers) {
            int addIndex = -1;
            Effect removeEffect = null;
            for (int i = 0; i < p.getEffects().size(); i++) {
                if (p.getEffects().get(i).getId() == map.get(0).getEffect().getId()) {
                    addIndex = i;
                    removeEffect = p.getEffects().get(i);
                    break;
                }
            }
            while (p.getEffects().remove(removeEffect)){}
            for (EffectPerformerMap m : map) {
                if (m.getPerformer().equals(p)) {
                    p.getEffects().add(addIndex, m.getEffect());
                    addIndex++;
                }
            }
        }
    }

    @Override
    public void undo() {
        ArrayList<Performer> performers = new ArrayList<>();
        for (EffectPerformerMap m : map) {
            if (!performers.contains(m.getPerformer())) {
                performers.add(m.getPerformer());
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

        for (Performer p : performers) {
            int addIndex = -1;
            Effect removeEffect = null;
            for (int i = 0; i < p.getEffects().size(); i++) {
                if (p.getEffects().get(i).getId() == undoMap.get(0).getEffect().getId()) {
                    addIndex = i;
                    removeEffect = p.getEffects().get(i);
                    break;
                }
            }
            while (p.getEffects().remove(removeEffect)){}
            for (EffectPerformerMap m : undoMap) {
                if (m.getPerformer().equals(p)) {
                    p.getEffects().add(addIndex, m.getEffect());
                    addIndex++;
                }
            }
        }
        undone = true;
    }

    @Override
    public void redo() {
        execute();
    }
}