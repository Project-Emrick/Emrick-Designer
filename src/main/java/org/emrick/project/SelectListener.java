package org.emrick.project;

import java.util.ArrayList;
import java.util.HashSet;

public interface SelectListener {
    void onMultiSelect(HashSet<Integer> labels, HashSet<String> symbols);
    void onGroupSelection(LEDStrip[] ledStrips);
    LEDStrip[] onSaveGroup();
    void onUpdateGroup();
    void ctrlGroupSelection(LEDStrip[] ledStrips);
}
