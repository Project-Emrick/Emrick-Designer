package org.emrick.project.effect;

import org.emrick.project.FootballFieldPanel;
import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.TimeManager;
import org.emrick.project.actions.*;

import javax.swing.*;
import java.util.*;

public class EffectManager {
    private final Stack<UndoableAction> undoStack = new Stack<>();
    private final Stack<UndoableAction> redoStack = new Stack<>();
    private FootballFieldPanel footballFieldPanel; // For easy access to selection info and performers
    private TimeManager timeManager; // Same TimeManager object as in MediaEditorGUI
    private HashMap<Integer, RFTrigger> count2RFTrigger; // Please keep EffectManager updated
    private ArrayList<Integer> ids;

    public EffectManager(FootballFieldPanel footballFieldPanel, TimeManager timeManager, HashMap<Integer, RFTrigger> count2RFTrigger) {
        this.footballFieldPanel = footballFieldPanel;
        this.timeManager = timeManager;
        this.count2RFTrigger = count2RFTrigger;
        ids = new ArrayList<>();
    }

    public int nextId() {
        int id = 0;
        if (ids.size() > 0) {
            int prev = ids.get(0);
            for (int i : ids) {
                if (i > prev) {
                    prev = i;
                }
            }
            id = prev + 1;
        }
        return id;
    }

    /**
     * Checks if an effect can be validly created. The effect should not overlap with another effect on the given
     * performer, and its start and end times should be in different sets.
     * @param effect The effect to check.
     * @param ledStrip The ledStrip on which the effect is to be applied.
     * @return True if valid, false if invalid.
     */
    public boolean isValid(Effect effect, LEDStrip ledStrip) {
        return isValid(effect, ledStrip, null);
    }
    public boolean isValid(Effect effect, LEDStrip ledStrip, Effect oldEffect) {
        if (effect == null) return false;

        // The effect should not overlap with another effect on the given performer
        long startMSec = effect.getStartTimeMSec();
        long endMSec = effect.getEndTimeMSec();
        for (Effect exist : ledStrip.getEffects()) {
            if (oldEffect != null && exist.id == oldEffect.id) {
                continue;
            }
            if (exist.getEndTimeMSec() < startMSec || endMSec < exist.getStartTimeMSec()) {
                continue;
            }
            System.out.println("Effect overlap detected on LED strip " + ledStrip.getLabel());
            System.out.println("Either startMSec " + startMSec + " < exist end " + exist.getEndTimeMSec() +
                    " or endMSec " + endMSec + " > exist start " + exist.getStartTimeMSec());
            System.out.println("Existing effect ID: " + exist.getId() + ", New effect ID: " + effect.getId());
            return false;
        }

        // Update: can be in different sets, can't overrun an RF trigger (although effects can start or end on triggers)
        for (Map.Entry<Integer, RFTrigger> entry : count2RFTrigger.entrySet()) {
            long tsMSec = timeManager.getCount2MSec().get(entry.getKey());
            if (effect.getStartTimeMSec() < tsMSec && tsMSec < effect.getEndTimeMSec()) {
                System.out.println("Effect overrun detected with RF trigger at count " + entry.getKey() + " on LED strip " + ledStrip.getLabel());
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if an RF trigger can be validly created. The RF trigger should not be placed where an effect over-runs it
     * (i.e., where it is in the middle of an effect). However, it can be placed where an effect begins or ends.
     * Timestamps (mSec) are checked for validation.
     * @param rfTrigger The RF trigger to be validated.
     */
    public boolean isValid(RFTrigger rfTrigger) {
        if (rfTrigger == null) {
            return false;
        }
        // Get timestamp in milliseconds for where the RF trigger is to be placed
        long tsMSec = timeManager.getCount2MSec().get(rfTrigger.getCount());

        for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
            for (Effect effect : ledStrip.getEffects()) {
                if (effect.getEndTimeMSec() <= tsMSec || tsMSec <= effect.getStartTimeMSec()) {
                    continue;
                }
                showAddRFTriggerErrorDialog(ledStrip);
                return false;
            }
        }
        return true;
    }

    public Stack<UndoableAction> getUndoStack() {
        return undoStack;
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoableAction action = undoStack.pop();
            action.undo();
            redoStack.push(action);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            UndoableAction action = redoStack.pop();
            action.redo();
            undoStack.push(action);
        }
    }

    public boolean addEffectToSelectedLEDStrips(Effect effect) {
        int id = 0;
        if (ids.size() > 0) {
            int prev = ids.get(0);
            for (int i : ids) {
                if (i > prev) {
                    prev = i;
                }
            }
            id = prev + 1;
        }
        if (effect == null)
            return false;
        effect.setId(id);
        ArrayList<LEDStrip> ledStrips = getSelectedLEDStrips();

        // Check if any LED strips are selected
        if (ledStrips.isEmpty()) {
            showNoPerformersSelectedErrorDialog();
            return false;
        }

        // Verify ability to add the effect to all selected performers, to avoid adding for some then error-ing out.
        for (LEDStrip ledStrip : ledStrips) {
            if (!isValid(effect, ledStrip)) {
                showAddEffectErrorDialog(ledStrip);
                return false;
            }
        }
        ids.add(id);
        ArrayList<EffectLEDStripMap> map = effect.getGeneratedEffect().generateEffects(ledStrips);
        UndoableAction e = new CreateEffectsAction(map);
        e.execute();
        undoStack.add(e);
        redoStack.clear();
        return true;
    }

    public void showAddRFTriggerErrorDialog(LEDStrip ledStrip) {
        JOptionPane.showMessageDialog(null,
                "RF trigger could not be added. Please check for collision with effect(s) on LED strip " + ledStrip.getLabel() + ".",
                "Create RF Trigger: Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showAddEffectErrorDialog(LEDStrip ledStrip) {
        JOptionPane.showMessageDialog(null,
                "Effect could not be applied to performer " + ledStrip.getLabel() +
                        ". Please check for possible collision with an RF trigger or the performer's other effects.",
                "Apply Effect: Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showNoPerformersSelectedErrorDialog() {
        JOptionPane.showMessageDialog(null,
                "No performers are selected. Please select one or more performers on the field before creating an effect.",
                "Create Effect: Error", JOptionPane.ERROR_MESSAGE);
    }

    public void removeEffectFromSelectedLEDStrips(Effect effect) {
        ArrayList<LEDStrip> ledStrips = getSelectedLEDStrips();
        if (ledStrips == null) return;
        ArrayList<EffectLEDStripMap> map = new ArrayList<>();

        for (LEDStrip ledStrip : ledStrips) {
            map.add(new EffectLEDStripMap(effect, ledStrip));
        }
        UndoableAction action = new RemoveEffectsAction(map);
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    public void removeAllEffectsFromSelectedLEDStrips() {
        ArrayList<LEDStrip> ledStrips = getSelectedLEDStrips();
        ArrayList<EffectLEDStripMap> map = new ArrayList<>();

        for (LEDStrip ledStrip : ledStrips) {
            for (Effect effect : ledStrip.getEffects()) {
                map.add(new EffectLEDStripMap(effect, ledStrip));
            }
        }
        UndoableAction action = new RemoveEffectsAction(map);
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    public void removeAllEffectsFromAllLEDStrips() {
        ArrayList<EffectLEDStripMap> map = new ArrayList<>();
        for (LEDStrip ledStrip : this.footballFieldPanel.drill.ledStrips) {
            for (Effect e : ledStrip.getEffects()) {
                map.add(new EffectLEDStripMap(e, ledStrip));
            }
        }
        UndoableAction action = new RemoveEffectsAction(map);
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    public void replaceEffectForSelectedLEDStrips(Effect oldEffect, Effect newEffect) {
        ArrayList<LEDStrip> ledStrips = getSelectedLEDStrips();
        if (ledStrips == null) return;
        newEffect.setGeneratedEffect(null);
        ArrayList<EffectLEDStripMap> map = newEffect.getGeneratedEffect().generateEffects(ledStrips);
        for (EffectLEDStripMap m : map) {
            m.setOldEffect(oldEffect);
        }

        UndoableAction replaceEffectsAction = new ReplaceEffectsAction(map);
        replaceEffectsAction.execute();
        undoStack.push(replaceEffectsAction);
        redoStack.clear();
    }

    /**
     * Get the effect based on current count as specified by attribute of FootballFieldPanel.
     * @param ledStrip The ledStrip of interest.
     * @return The effect at the current count for the given performer. If there is none, return null.
     */
    public Effect getEffect(LEDStrip ledStrip, long time) {
        long currentMSec = time;

        // Find effect in performer where current millis falls in range of effect start and end time
        for (Effect effect : ledStrip.getEffects()) {
            if (effect.getStartTimeMSec() <= currentMSec && currentMSec <= effect.getEndTimeMSec()) {
                return effect;
            }
        }
        return null;
    }

    public Effect getEffectsFromSelectedLEDStrips(long time) {
        Effect e = null;
        for (int i = 0; i < footballFieldPanel.selectedLEDStrips.size(); i++) {
            // This section only works as intended under the assumption that all LED strips
            // for 1 performer contain the same effect

            LEDStrip l = (LEDStrip) footballFieldPanel.selectedLEDStrips.toArray()[i];

            if (e == null && i == 0) {
                e = getEffect(l, time);
                break;
            } else {
                Effect f = getEffect(l, time);
                if (e == null) {
                    if (f != null) {
                        return null;
                    }
                } else if (!e.equals(f)) {
                    return null;
                }
            }
        }
        if (e != null) {
            return e;
        } else {
            return new Effect(time);
        }
    }

    public ArrayList<LEDStrip> getLEDStripsWithEffect(Effect e) {
        ArrayList<LEDStrip> ledStrips = new ArrayList<>();
        for (LEDStrip ledStrip : this.footballFieldPanel.drill.ledStrips) {
            if (ledStrip.getEffects().contains(e)) {
                ledStrips.add(ledStrip);
            }
        }
        return ledStrips;
    }

    public ArrayList<LEDStrip> getSelectedLEDStrips() {
        ArrayList<LEDStrip> selectedPerformers = new ArrayList<>();
        if (footballFieldPanel.selectedLEDStrips.isEmpty()) {
            return selectedPerformers;
        }

        selectedPerformers.addAll(footballFieldPanel.selectedLEDStrips);
        return selectedPerformers;
    }

    public FootballFieldPanel getFootballFieldPanel() {
        return footballFieldPanel;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public void setCount2RFTrigger(HashMap<Integer, RFTrigger> count2RFTrigger) {
        this.count2RFTrigger = count2RFTrigger;
    }

    public ArrayList<Integer> getIds() {
        return ids;
    }

    public void setIds(ArrayList<Integer> ids) {
        this.ids = ids;
    }
}