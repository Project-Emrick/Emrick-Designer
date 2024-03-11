package org.emrick.project;

import java.util.ArrayList;
import java.util.Map;

public interface SyncListener {
    void onSync(ArrayList<Map.Entry<String, Integer>> times);
}
