package org.emrick.project;

import java.awt.*;
import java.io.File;
import java.net.URI;

public interface ImportListener {
    void onBeginImport();
    void onImport();
    void onFileSelect(File archivePath, File drillPath, File csvFile);
    void onFloorCoverImport(Image image);
    void onSurfaceImport(Image image);
    void onAudioImport(File audioFile);
    void onDrillImport(String drill);
}
