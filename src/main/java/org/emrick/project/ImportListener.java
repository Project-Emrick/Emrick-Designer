package org.emrick.project;

import java.awt.*;
import java.io.File;

public interface ImportListener {
    void onImport();
    void onFloorCoverImport(Image image);
    void onSurfaceImport(Image image);
    void onAudioImport(File audioFile);
    void onDrillImport(String drill);
}
