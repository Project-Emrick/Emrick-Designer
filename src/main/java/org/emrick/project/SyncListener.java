package org.emrick.project;

import java.util.ArrayList;

public interface SyncListener {
    void onSync(ArrayList<SyncTimeGUI.Pair> times, int startDelay);
}
