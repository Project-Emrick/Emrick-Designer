package org.emrick.project.actions;

import org.emrick.project.LEDConfigurationGUI;

import java.util.ArrayList;

public class UpdateConfigsAction implements UndoableAction {
    private ArrayList<PerformerConfigMap> performerConfigMaps;

    public UpdateConfigsAction(ArrayList<PerformerConfigMap> performerConfigMaps) {
        this.performerConfigMaps = performerConfigMaps;
    }

    @Override
    public void execute() {
        for (PerformerConfigMap performerConfigMap : performerConfigMaps) {
            LEDConfigurationGUI.PerformerConfigPanel performerConfigPanel = performerConfigMap.getPerformerConfigPanel();
            performerConfigPanel.pasteConfig(performerConfigMap.getNewPerformerConfig());
        }
    }

    @Override
    public void undo() {
        for (PerformerConfigMap performerConfigMap : performerConfigMaps) {
            LEDConfigurationGUI.PerformerConfigPanel performerConfigPanel = performerConfigMap.getPerformerConfigPanel();
            performerConfigPanel.pasteConfig(performerConfigMap.getOldPerformerConfig());
        }
    }

    @Override
    public void redo() {
        execute();
    }
}
