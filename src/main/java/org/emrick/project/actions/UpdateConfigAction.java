package org.emrick.project.actions;

import java.util.ArrayList;

public class UpdateConfigAction implements UndoableAction {
    private ArrayList<LEDConfigLEDStripMap> LEDConfigLEDStripMaps;

    public UpdateConfigAction(ArrayList<LEDConfigLEDStripMap> LEDConfigLEDStripMaps) {
        this.LEDConfigLEDStripMaps = LEDConfigLEDStripMaps;
    }

    @Override
    public void execute() {
        for (LEDConfigLEDStripMap LEDConfigLEDStripMap : LEDConfigLEDStripMaps) {
            LEDConfigLEDStripMap.getLedStrip().setLedConfig(LEDConfigLEDStripMap.getNewConfig());

        }
    }

    @Override
    public void undo() {
        for (LEDConfigLEDStripMap LEDConfigLEDStripMap : LEDConfigLEDStripMaps) {
            LEDConfigLEDStripMap.getLedStrip().setLedConfig(LEDConfigLEDStripMap.getOldConfig());
        }
    }

    @Override
    public void redo() {
        execute();
    }
}
