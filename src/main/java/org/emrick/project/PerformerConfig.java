package org.emrick.project;

import org.emrick.project.actions.LEDConfig;

import java.util.ArrayList;

public class PerformerConfig {
    private ArrayList<LEDConfig> ledConfigs;

    public PerformerConfig(ArrayList<LEDConfig> ledConfigs) {
        this.ledConfigs = ledConfigs;
    }

    public ArrayList<LEDConfig> getLedConfigs() {
        return ledConfigs;
    }

    public void setLedConfigs(ArrayList<LEDConfig> ledConfigs) {
        this.ledConfigs = ledConfigs;
    }
}
