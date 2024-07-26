package org.emrick.project.actions;

import org.emrick.project.LEDConfigurationGUI;

import java.util.ArrayList;

public class UpdateConfigsAction implements UndoableAction {
    private ArrayList<PerformerConfigMap> performerConfigMaps;
    private LEDConfigurationGUI ledConfigurationGUI;

    public UpdateConfigsAction(ArrayList<PerformerConfigMap> performerConfigMaps, LEDConfigurationGUI ledConfigurationGUI) {
        this.performerConfigMaps = performerConfigMaps;
        this.ledConfigurationGUI = ledConfigurationGUI;
    }

    @Override
    public void execute() {
        for (PerformerConfigMap performerConfigMap : performerConfigMaps) {
            LEDConfigurationGUI.PerformerConfigPanel performerConfigPanel = performerConfigMap.getPerformerConfigPanel();
            performerConfigPanel.pasteConfig(performerConfigMap.getNewPerformerConfig());
        }
        if (ledConfigurationGUI != null) {
            ledConfigurationGUI.reinitializeLEDConfigPanel();
        }
    }

    @Override
    public void undo() {
        for (PerformerConfigMap performerConfigMap : performerConfigMaps) {
            LEDConfigurationGUI.PerformerConfigPanel performerConfigPanel = performerConfigMap.getPerformerConfigPanel();
            performerConfigPanel.pasteConfig(performerConfigMap.getOldPerformerConfig());
        }
        if (ledConfigurationGUI != null) {
            ledConfigurationGUI.reinitializeLEDConfigPanel();
        }
    }

    @Override
    public void redo() {
        execute();
    }
}
