package org.emrick.project;

import java.io.File;

public interface ReplaceFilesListener {
    public boolean onNewFileSelect(File drill, File archive);
}
