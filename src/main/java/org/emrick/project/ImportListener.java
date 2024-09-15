package org.emrick.project;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

public interface ImportListener {
    void onBeginImport();
    void onImport();
    void onFileSelect(ArrayList<File> archivePaths, File csvFile);
    void onAudioImport(ArrayList<File> audioFiles);
    void onDrillImport(String drill);
    void onConcatAudioImport(ArrayList<File> audioFiles);
}
