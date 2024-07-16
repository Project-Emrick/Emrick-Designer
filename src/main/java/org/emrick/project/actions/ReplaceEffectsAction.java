package org.emrick.project.actions;

import org.emrick.project.*;
import org.emrick.project.effect.Effect;

import java.util.*;

public class ReplaceEffectsAction implements UndoableAction {
    private final ArrayList<EffectLEDStripMap> map;

    public ReplaceEffectsAction(ArrayList<EffectLEDStripMap> map) {
        this.map = map;
    }

    @Override
    public void execute() {
        // TODO: Rewrite to remove all effects that match id and then add new effects at the index of first old match
        ArrayList<LEDStrip> ledStrips = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            if (ledStrips.size() > 0) {
                if (!map.get(i-1).getLedStrip().equals(map.get(i).getLedStrip())) {
                    ledStrips.add(map.get(i).getLedStrip());
                }
            } else {
                ledStrips.add(map.get(i).getLedStrip());
            }
        }
        addEffects(ledStrips, map);
    }

    @Override
    public void undo() {
        ArrayList<LEDStrip> ledStrips = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            if (ledStrips.size() > 0) {
                if (!map.get(i-1).getLedStrip().equals(map.get(i).getLedStrip())) {
                    ledStrips.add(map.get(i).getLedStrip());
                }
            } else {
                ledStrips.add(map.get(i).getLedStrip());
            }
        }

        ArrayList<EffectLEDStripMap> undoMap = map.get(0).getOldEffect().getGeneratedEffect().generateEffects(ledStrips);

        addEffects(ledStrips, undoMap);
    }

    private void addEffects(ArrayList<LEDStrip> ledStrips, ArrayList<EffectLEDStripMap> undoMap) {
        int[] addIndexes = new int[ledStrips.size()];
        int index = 0;
        for (LEDStrip l : ledStrips) {

            Effect removeEffect = null;
            for (int i = 0; i < l.getEffects().size(); i++) {
                if (l.getEffects().get(i).getId() == undoMap.get(0).getEffect().getId()) {
                    addIndexes[index] = i;
                    removeEffect = l.getEffects().get(i);
                    break;
                }
            }
            while (l.getEffects().remove(removeEffect)){}

            index++;
        }
        for (EffectLEDStripMap m : undoMap) {
            int i = ledStrips.indexOf(m.getLedStrip());
            m.getLedStrip().getEffects().add(addIndexes[i], m.getEffect());
            addIndexes[i]++;
        }
    }

    @Override
    public void redo() {
        execute();
    }
}