package org.emrick.project;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

public class EffectManager {

    FootballFieldPanel footballFieldPanel; // For easy access to selection info and performers
    TimeManager timeManager; // Same TimeManager object as in MediaEditorGUI

    public EffectManager(FootballFieldPanel footballFieldPanel, TimeManager timeManager) {
        this.footballFieldPanel = footballFieldPanel;
        this.timeManager = timeManager;
    }

    /**
     * Calls isValid for the currently selected performer.
     * @param effect The effect to check.
     * @return True if valid, false if invalid.
     */
    public boolean isValidForSelected(Effect effect) {
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

        // The effect should not overlap with another effect on the given performer
        long startMSec = effect.getStartTimeMSec();
        long endMSec = effect.getEndTimeMSec();
        for (Effect exist : performer.getEffects()) {
            if (exist.getEndTimeMSec() < startMSec || endMSec < exist.getStartTimeMSec()) {
                continue;
            }
            return false;
        }

        // The effect's start and end times should not be in different sets
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

    public void addEffect(Effect effect, Performer performer) {
        if (isValid(effect, performer)) {
            JOptionPane.showMessageDialog(null,
                    "Effect could not be applied! Check for possible set run-off or overlap with other effects.",
                    "Effect Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        performer.getEffects().add(effect);
    }

    public void addEffectToSelected(Effect effect) {
        Performer performer = getSelectedPerformer();
        if (performer == null) return;
        addEffect(effect, performer);
    }

    public void removeEffect(Effect effect, Performer performer) {
        performer.getEffects().remove(effect);
    }

    public void removeEffectFromSelected(Effect effect) {
        Performer performer = getSelectedPerformer();
        if (performer == null) return;
        removeEffect(effect, performer);
    }

    public void removeAllEffects(Performer performer) {
        performer.getEffects().clear();
    }

    public void removeAllEffectsFromSelected() {
        Performer performer = getSelectedPerformer();
        if (performer == null) return;
        removeAllEffects(performer);
    }

    /**
     * Pseudo-update functionality for effects. Instead of updating the same object, just replace it with an updated version.
     * For now, when an effect is updated, the start time should not change. This is handled implicitly by EffectGUI.
     * @param oldEffect The effect to be replaced.
     * @param newEffect The effect that replaces.
     * @param performer The associated performer.
     */
    public void replaceEffect(Effect oldEffect, Effect newEffect, Performer performer) {
        performer.getEffects().remove(oldEffect);

        // Check that replacement with new effect is still valid
        if (!isValid(newEffect, performer)) {
            performer.getEffects().add(oldEffect); // Put it back
        }
        performer.getEffects().add(newEffect);
    }

    public void replaceEffectForSelected(Effect oldEffect, Effect newEffect) {
        Performer performer = getSelectedPerformer();
        if (performer == null) return;
        replaceEffect(oldEffect, newEffect, performer);
    }

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

    public Effect getEffectFromSelected() {
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

}
