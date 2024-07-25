package org.emrick.project.actions;

import org.emrick.project.LEDConfigurationGUI;
import org.emrick.project.Performer;
import org.emrick.project.PerformerConfig;

public class PerformerConfigMap {
    private PerformerConfig oldPerformerConfig;
    private PerformerConfig newPerformerConfig;
    private LEDConfigurationGUI.PerformerConfigPanel performerConfigPanel;

    public PerformerConfigMap(PerformerConfig oldPerformerConfig, PerformerConfig newPerformerConfig, LEDConfigurationGUI.PerformerConfigPanel performerConfigPanel) {
        this.oldPerformerConfig = oldPerformerConfig;
        this.newPerformerConfig = newPerformerConfig;
        this.performerConfigPanel = performerConfigPanel;
    }

    public PerformerConfig getOldPerformerConfig() {
        return oldPerformerConfig;
    }

    public void setOldPerformerConfig(PerformerConfig oldPerformerConfig) {
        this.oldPerformerConfig = oldPerformerConfig;
    }

    public PerformerConfig getNewPerformerConfig() {
        return newPerformerConfig;
    }

    public void setNewPerformerConfig(PerformerConfig newPerformerConfig) {
        this.newPerformerConfig = newPerformerConfig;
    }

    public LEDConfigurationGUI.PerformerConfigPanel getPerformerConfigPanel() {
        return performerConfigPanel;
    }

    public void setPerformerConfigPanel(LEDConfigurationGUI.PerformerConfigPanel performerConfigPanel) {
        this.performerConfigPanel = performerConfigPanel;
    }
}
