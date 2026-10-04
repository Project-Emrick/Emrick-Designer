package org.emrick.project.dev;

import org.emrick.project.LEDStrip;

import javax.swing.*;
import java.util.List;

/** What Developer Mode needs from the main Emrick Designer window. */
public interface DevModeHost {
    JFrame getFrame();

    /** @return the open project's LED strips (board ID, label, LED count), or an empty list if no project is open */
    List<LEDStrip> getLedStrips();

    /** Leaves Developer Mode, lets the user open a project, and comes back to Developer Mode once it's loaded. */
    void openProjectForDevMode();

    void exitDeveloperMode();
}
