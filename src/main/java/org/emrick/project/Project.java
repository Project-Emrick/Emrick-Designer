package org.emrick.project;

import org.emrick.project.serde.ProjectFile;

import java.util.ArrayList;
import java.util.HashMap;

import org.emrick.project.effect.RFTrigger;

public class Project {

    public final ArrayList<ArrayList<SyncTimeGUI.Pair>> timeSync;
    public final ArrayList<Drill> drill;
    public final String archivePath;
    public final ArrayList<Float> startDelay;
    ArrayList<HashMap<Integer, RFTrigger>> count2RFTrigger;



}
