package org.emrick.project;

import java.util.Map;

public interface SyncListener {
    void onSync(Map<String, Integer> times);
}
