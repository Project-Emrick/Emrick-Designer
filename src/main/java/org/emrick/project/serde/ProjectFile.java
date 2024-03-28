package org.emrick.project.serde;

import org.emrick.project.Drill;
import org.emrick.project.SyncTimeGUI;

import java.util.ArrayList;

public class ProjectFile {
    public final ArrayList<SyncTimeGUI.Pair> timeSync;
    public final Drill drill;
    public final String archivePath;
    public final String drillPath;
    public final Float startDelay;

    public ProjectFile(Drill drill, String archivePath, String drillPath, ArrayList<SyncTimeGUI.Pair> timeSync, Float startDelay) {
        this.drill = drill;
        this.archivePath = archivePath;
        this.drillPath = drillPath;
        this.timeSync = timeSync;
        this.startDelay = startDelay;
    }
}
