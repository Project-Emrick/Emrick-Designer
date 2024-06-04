package org.emrick.project;

import java.util.ArrayList;
import java.util.HashSet;

public interface SelectListener {
    void onMultiSelect(HashSet<Integer> labels, HashSet<String> symbols);
    void onGroupSelection(Performer[] performers);
    Performer[] onSaveGroup();
    void onUpdateGroup();
}
