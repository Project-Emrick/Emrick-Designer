package org.emrick.project.serde;

import org.emrick.project.Drill;
import org.emrick.project.SelectionGroupGUI;
import org.emrick.project.SyncTimeGUI;
import org.emrick.project.effect.RFTrigger;

import java.util.ArrayList;
import java.util.HashMap;

public class ProjectFile {
    public final ArrayList<SyncTimeGUI.Pair> timeSync;
    public final Drill drill;
    public final ArrayList<String> archiveNames;
    public final Float startDelay;
    public final HashMap<Integer, RFTrigger> count2RFTrigger;
    public final ArrayList<Integer> ids;
    public final ArrayList<SelectionGroupGUI.SelectionGroup> selectionGroups;

    public ProjectFile(Drill drill, ArrayList<String> archiveNames, ArrayList<SyncTimeGUI.Pair> timeSync, Float startDelay, HashMap<Integer, RFTrigger> count2RFTrigger, ArrayList<Integer> ids, ArrayList<SelectionGroupGUI.SelectionGroup> selectionGroups) {
        this.drill = drill;
        this.archiveNames = archiveNames;
        this.timeSync = timeSync;
        this.startDelay = startDelay;
        this.count2RFTrigger = count2RFTrigger;
        this.ids = ids;
        this.selectionGroups = selectionGroups;
    }
}
