package org.emrick.project.actions;

import org.emrick.project.LEDStrip;

import java.io.ObjectInputFilter;
import java.util.ArrayList;

public class UpdateConfigsAction implements UndoableAction {
    private ArrayList<ConfigLEDStripMap> configLEDStripMaps;

    public UpdateConfigsAction(ArrayList<ConfigLEDStripMap> configLEDStripMaps) {
        this.configLEDStripMaps = configLEDStripMaps;
    }

    @Override
    public void execute() {
        for (ConfigLEDStripMap configLEDStripMap : configLEDStripMaps) {
            configLEDStripMap.getLedStrip().setLedConfig(configLEDStripMap.getNewConfig());

        }
    }

    @Override
    public void undo() {
        for (ConfigLEDStripMap configLEDStripMap : configLEDStripMaps) {
            configLEDStripMap.getLedStrip().setLedConfig(configLEDStripMap.getOldConfig());
        }
    }

    @Override
    public void redo() {
        execute();
    }
}
