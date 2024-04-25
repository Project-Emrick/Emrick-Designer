package org.emrick.project.effect;

import org.emrick.project.FootballFieldPanel;
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

    public EffectManager(FootballFieldPanel footballFieldPanel, TimeManager timeManager) {
        this.footballFieldPanel = footballFieldPanel;
        this.timeManager = timeManager;
    }

    /**
     * Calls isValid for the currently selected performer.
     * @param effect The effect to check.
     * @return True if valid, false if invalid.
     */
    public boolean isValidForSelectedPerformer(Effect effect) {
        Performer performer = getSelectedPerformer();
        if (performer == null) return false;
        return isValid(effect, performer);
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

        // The effect's start and end times should not be in different sets (update: can be in different sets, can't be
        //  overrun an RF trigger though
        String startTimeSet = "0";
        String endTimeSet = "0";
        for (Map.Entry<String, Long> entry : timeManager.getSet2MSec()) {
            String set = entry.getKey();
            long setMSec = entry.getValue();

            if (startMSec >= setMSec) startTimeSet = set;
            if (endMSec >= setMSec) endTimeSet = set;
        }
        return startTimeSet.equals(endTimeSet);
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
        showAddEffectSuccessDialog();
        UndoableAction createEffectsAction = new CreateEffectsAction(effect, performers);
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
        if (successful)
            showAddEffectSuccessDialog();
        return successful;
    }

    public boolean addEffectToSelectedPerformers(Effect effect) {
        if (effect == null)
            return false;

        ArrayList<Performer> selectedPerformers = getSelectedPerformers();

        // Verify ability to add the effect to all selected performers, to avoid adding for some then error-ing out.
        for (Performer performer : selectedPerformers) {
            if (!isValid(effect, performer)) {
                showAddEffectErrorDialog(performer);
                return false;
            }
        }
        addEffect(effect, selectedPerformers);
        return true;
    }

    private void showAddEffectErrorDialog(Performer performer) {
        JOptionPane.showMessageDialog(null,
                "Effect could not be applied to performer " + performer.getIdentifier() +
                        ". Please check for possible set run-off or overlap with the performer's other effects.",
                "Apply Effect: Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showAddEffectSuccessDialog() {
        JOptionPane.showMessageDialog(null, "Effect applied successfully.",
                "Apply Effect: Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showRemoveEffectSuccessDialog() {
        JOptionPane.showMessageDialog(null, "Effect removed successfully.",
                "Remove Effect: Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public void removeEffect(Effect effect, Performer performer) {
        showRemoveEffectSuccessDialog(); // An effect should always be removable
        UndoableAction removeEffectAction = new RemoveEffectAction(effect, performer);
        removeEffectAction.execute();
        undoStack.push(removeEffectAction);
        redoStack.clear();
    }

    public void removeEffectFromSelectedPerformer(Effect effect) {
        Performer performer = getSelectedPerformer();
        if (performer == null) return;
        removeEffect(effect, performer);
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
        for (Performer performer : selectedPerformers) {
            removeAllEffects(performer);
        }
    }

    public void removeAllEffectsFromAllPerformers() {
        for (Performer performer : this.footballFieldPanel.drill.performers) {
            removeAllEffects(performer);
        }
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

    public void replaceEffectForSelectedPerformer(Effect oldEffect, Effect newEffect) {
        Performer performer = getSelectedPerformer();
        if (performer == null) return;
        boolean successful = replaceEffect(oldEffect, newEffect, performer);
        if (successful) showAddEffectSuccessDialog();
    }

    // TODO: Create ReplaceEffectsAction class (plural)

    /**
     * Get the effect based on current count as specified by attribute of FootballFieldPanel.
     * @param performer The performer of interest.
     * @return The effect at the current count for the given performer. If there is none, return null.
     */
    public Effect getEffect(Performer performer) {
        long currentMSec = timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());

        // Find effect in performer where current millis falls in range of effect start and end time
        for (Effect effect : performer.getEffects()) {
            if (effect.getStartTimeMSec() <= currentMSec && currentMSec <= effect.getEndTimeMSec()) {
                return effect;
            }
        }
        return null;
    }

    public Effect getEffectFromSelectedPerformer() {
        Performer performer = getSelectedPerformer();
        if (performer == null) return null;
        return getEffect(performer);
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

    private ArrayList<Performer> getSelectedPerformers() {
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
}