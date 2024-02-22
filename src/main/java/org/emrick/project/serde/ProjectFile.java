package org.emrick.project.serde;

import org.emrick.project.Drill;

public class ProjectFile {
    public Drill drill;
    public String archivePath;
    public String drillPath;

    public ProjectFile(Drill drill, String archivePath, String drillPath) {
        this.drill = drill;
        this.archivePath = archivePath;
        this.drillPath = drillPath;
    }
}
