package org.emrick.project.effect;

import org.emrick.project.FootballFieldPanel;
import org.emrick.project.Performer;
import org.emrick.project.TimeManager;
import org.emrick.project.actions.*;

import javax.swing.*;
import java.time.Duration;
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

    /**
     * Checks if an effect can be validly created. The effect should not overlap with another effect on the given
     * performer, and its start and end times should be in different sets.
     * @param effect The effect to check.
     * @param performer The performer on which the effect is to be applied.
     * @return True if valid, false if invalid.
     */
    public boolean isValid(Effect effect, Performer performer) {
        if (effect == null) return false;

        // The effect should not overlap with another effect on the given performer
        long startMSec = effect.getStartTimeMSec();
        long endMSec = effect.getEndTimeMSec();
        for (Effect exist : performer.getEffects()) {
            if (exist.getEndTimeMSec() < startMSec || endMSec < exist.getStartTimeMSec()) {
                continue;
            }
            return false;
        }

        // Update: can be in different sets, can't overrun an RF trigger (although effects can start or end on triggers)
        for (Map.Entry<Integer, RFTrigger> entry : count2RFTrigger.entrySet()) {
            long tsMSec = timeManager.getCount2MSec().get(entry.getKey());
            if (effect.getStartTimeMSec() < tsMSec && tsMSec < effect.getEndTimeMSec()) {
                return false;
            }
        }
        return true;

//        // The effect's start and end times should not be in different sets
//        String startTimeSet = "0";
//        String endTimeSet = "0";
//        for (Map.Entry<String, Long> entry : timeManager.getSet2MSec()) {
//            String set = entry.getKey();
//            long setMSec = entry.getValue();
//
//            if (startMSec >= setMSec) startTimeSet = set;
//            if (endMSec >= setMSec) endTimeSet = set;
//        }
//        return startTimeSet.equals(endTimeSet);
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

        for (Performer performer : footballFieldPanel.drill.performers) {
            for (Effect effect : performer.getEffects()) {
                if (effect.getEndTimeMSec() <= tsMSec || tsMSec <= effect.getStartTimeMSec()) {
                    continue;
                }
                showAddRFTriggerErrorDialog(performer);
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

    public boolean addEffect(Effect effect, Performer performer) {
        if (!isValid(effect, performer)) {
            showAddEffectErrorDialog(performer);
            return false;
        }
        UndoableAction createEffectAction = new CreateEffectAction(effect, performer);
        createEffectAction.execute();
        undoStack.push(createEffectAction);
        redoStack.clear();
        return true;
    }

    private void addEffect(Effect effect, List<Performer> performers) {
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        for (Performer p : performers) {
            map.add(new EffectPerformerMap(effect, p));
        }
        UndoableAction createEffectsAction = new CreateEffectsAction(map);
        createEffectsAction.execute();
        undoStack.push(createEffectsAction);
        redoStack.clear();
    }

    /**
     * Initial idea was that an effect can only be created for one (selected) performer at a time.
     * addEffectToSelectedPerformer accomplishes exactly this. May become deprecated soon.
     * @param effect The effect to add to the currently selected performer.
     * @return true if added successfully, false otherwise.
     */
    public boolean addEffectToSelectedPerformer(Effect effect) {
        Performer performer = getSelectedPerformer();
        if (performer == null)
            return false;

        boolean successful = addEffect(effect, performer);
        return successful;
    }

    public boolean addEffectToSelectedPerformers(Effect effect) {
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
        ArrayList<Performer> selectedPerformers = getSelectedPerformers();

        // Verify ability to add the effect to all selected performers, to avoid adding for some then error-ing out.
        for (Performer performer : selectedPerformers) {
            if (!isValid(effect, performer)) {
                showAddEffectErrorDialog(performer);
                return false;
            }
        }
        ids.add(id);
        ArrayList<EffectPerformerMap> map = effect.getGeneratedEffect().generateEffects(selectedPerformers);
        UndoableAction e = new CreateEffectsAction(map);
        e.execute();
        undoStack.add(e);
        redoStack.clear();
        return true;
    }

    private void showAddRFTriggerErrorDialog(Performer performer) {
        JOptionPane.showMessageDialog(null,
                "RF trigger could not be added. Please check for collision with effect(s) on performer " + performer.getIdentifier() + ".",
                "Create RF Trigger: Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showAddEffectErrorDialog(Performer performer) {
        JOptionPane.showMessageDialog(null,
                "Effect could not be applied to performer " + performer.getIdentifier() +
                        ". Please check for possible collision with an RF trigger or the performer's other effects.",
                "Apply Effect: Error", JOptionPane.ERROR_MESSAGE);
    }

    public void removeEffect(Effect effect, Performer performer) {
        UndoableAction removeEffectAction = new RemoveEffectAction(effect, performer);
        removeEffectAction.execute();
        undoStack.push(removeEffectAction);
        redoStack.clear();
    }

    public void removeEffectFromSelectedPerformers(Effect effect) {
        ArrayList<Performer> performers = getSelectedPerformers();
        if (performers == null) return;
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        for (Performer performer : performers) {
            map.add(new EffectPerformerMap(effect, performer));
        }
        UndoableAction action = new RemoveEffectsAction(map);
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    public void removeAllEffects(Performer performer) {
        performer.getEffects().clear();
    }

    /**
     * The difference between removeAllEffectsFromSelectedPerformer() and removeAllEffectsFromSelectedPerformers() is
     * that the former only works if there is exactly one performer selected. The latter works for any number of
     * selected performers. Use based on desired behavior.
     */
    public void removeAllEffectsFromSelectedPerformer() {
        Performer performer = getSelectedPerformer();
        if (performer == null) return;
        removeAllEffects(performer);
    }

    public void removeAllEffectsFromSelectedPerformers() {
        ArrayList<Performer> selectedPerformers = getSelectedPerformers();
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        for (Performer performer : selectedPerformers) {
            for (Effect effect : performer.getEffects()) {
                map.add(new EffectPerformerMap(effect, performer));
            }
        }
        UndoableAction action = new RemoveEffectsAction(map);
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    public void removeAllEffectsFromAllPerformers() {
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        for (Performer performer : this.footballFieldPanel.drill.performers) {
            for (Effect e : performer.getEffects()) {
                map.add(new EffectPerformerMap(e, performer));
            }
        }
        UndoableAction action = new RemoveEffectsAction(map);
        action.execute();
        undoStack.push(action);
        redoStack.clear();
    }

    /**
     * Pseudo-update functionality for effects. Instead of updating the same object, just replace it with an updated version.
     * For now, when an effect is updated, the start time should not change. This is handled implicitly by EffectGUI.
     * @param oldEffect The effect to be replaced.
     * @param newEffect The effect that replaces.
     * @param performer The associated performer.
     */
    public boolean replaceEffect(Effect oldEffect, Effect newEffect, Performer performer) {
        performer.getEffects().remove(oldEffect);

        // Check that replacement with new effect is still valid
        if (!isValid(newEffect, performer)) {
            showAddEffectErrorDialog(performer);
            performer.getEffects().add(oldEffect); // Put it back
            return false;
        }
        performer.getEffects().add(oldEffect); // Put it back
        //performer.getEffects().add(newEffect);
        UndoableAction replaceEffectAction = new ReplaceEffectAction(oldEffect, newEffect, performer);
        replaceEffectAction.execute();
        undoStack.push(replaceEffectAction);
        redoStack.clear();
        return true;
    }

    public void replaceEffectForSelectedPerformers(Effect oldEffect, Effect newEffect) {
        ArrayList<Performer> performers = getSelectedPerformers();
        if (performers == null) return;
        ArrayList<Performer> allPerformers = footballFieldPanel.drill.performers;
        for (int i = 0; i < allPerformers.size(); i++) {
            for (int j = 0; j < allPerformers.get(i).getEffects().size(); j++) {
                if (allPerformers.get(i).getEffects().get(j).equals(oldEffect)) {
                    if (!performers.contains(allPerformers.get(i))) {
                        performers.add(allPerformers.get(i));
                    }
                    break;
                }
            }
        }

        ArrayList<EffectPerformerMap> map = newEffect.getGeneratedEffect().generateEffects(performers);
        for (EffectPerformerMap m : map) {
            m.setOldEffect(oldEffect);
        }

        UndoableAction replaceEffectsAction = new ReplaceEffectsAction(map);
        replaceEffectsAction.execute();
        undoStack.push(replaceEffectsAction);
        redoStack.clear();
    }

    /**
     * Get the effect based on current count as specified by attribute of FootballFieldPanel.
     * @param performer The performer of interest.
     * @return The effect at the current count for the given performer. If there is none, return null.
     */
    public Effect getEffect(Performer performer, long time) {
        long currentMSec = time;

        // Find effect in performer where current millis falls in range of effect start and end time
        for (Effect effect : performer.getEffects()) {
            if (effect.getStartTimeMSec() <= currentMSec && currentMSec <= effect.getEndTimeMSec()) {
                return effect;
            }
        }
        return null;
    }

    public Effect getEffectFromSelectedPerformer(long time) {
        Performer performer = getSelectedPerformer();
        if (performer == null) return null;
        return getEffect(performer, time);
    }

    public Effect getEffectsFromSelectedPerformers(long time) {
        Effect e = null;
        for (int i = 0; i < footballFieldPanel.selectedPerformers.values().size(); i++) {
            if (e == null && i == 0) {
                e = getEffect((Performer) footballFieldPanel.selectedPerformers.values().toArray()[i], time);
            } else {
                Effect f = getEffect((Performer) footballFieldPanel.selectedPerformers.values().toArray()[i], time);
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

    /**
     * For now, assume only one performer is selected. Can change for future.
     * @return The single selected performer
     */
    private Performer getSelectedPerformer() {
        if (footballFieldPanel.selectedPerformers.size() == 1) {

            // Only works because we know the map has one entry. Also, why are we using a hashmap and not a set for selectedPerformers?
            Map.Entry<String, Performer> entry = footballFieldPanel.selectedPerformers.entrySet().iterator().next();
            return entry.getValue();
        }
        return null;
    }



    public ArrayList<Performer> getSelectedPerformers() {
        ArrayList<Performer> selectedPerformers = new ArrayList<>();
        if (footballFieldPanel.selectedPerformers.isEmpty()) {
            return selectedPerformers;
        }

        for (Map.Entry<String, Performer> entry : footballFieldPanel.selectedPerformers.entrySet()) {
            selectedPerformers.add(entry.getValue());
        }
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