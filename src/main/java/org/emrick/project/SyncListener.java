package org.emrick.project;

import java.util.ArrayList;

public interface SyncListener {
    void onSync(ArrayList<SyncTimeGUI.Pair> times, float startDelay);
    void onAutoSync(ArrayList<SyncTimeGUI.PairCountMS> counts, float startDelay);
}
