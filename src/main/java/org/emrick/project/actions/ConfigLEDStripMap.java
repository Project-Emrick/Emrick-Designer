package org.emrick.project.actions;

import org.emrick.project.LEDStrip;

public class ConfigLEDStripMap {
    private LEDStrip ledStrip;
    private LEDConfig newConfig;
    private LEDConfig oldConfig;

    public ConfigLEDStripMap(LEDStrip ledStrip, LEDConfig newConfig, LEDConfig oldConfig) {
        this.ledStrip = ledStrip;
        this.newConfig = newConfig;
        this.oldConfig = oldConfig;
    }

    public LEDStrip getLedStrip() {
        return ledStrip;
    }

    public void setLedStrip(LEDStrip ledStrip) {
        this.ledStrip = ledStrip;
    }

    public LEDConfig getNewConfig() {
        return newConfig;
    }

    public void setNewConfig(LEDConfig newConfig) {
        this.newConfig = newConfig;
    }

    public LEDConfig getOldConfig() {
        return oldConfig;
    }

    public void setOldConfig(LEDConfig oldConfig) {
        this.oldConfig = oldConfig;
    }
}
