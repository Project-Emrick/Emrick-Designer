package org.emrick.project;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TimeManager {

    private HashMap<Integer, Long> count2MSec;                      // Count : The milliseconds it takes to get there

    // Additional
    private final Map<String, Integer> set2Count;                   // Set : The count it begins on
    private ArrayList<Map.Entry<String, Integer>> set2CountSorted;  // Set : The count it begins on
    private ArrayList<Map.Entry<String, Integer>> set2NumCounts;    // Set : The number of counts in the set
    private ArrayList<Map.Entry<String, Long>> set2MSec;            // Set : The milliseconds it takes to get there

    public TimeManager(Map<String, Integer> set2Count, ArrayList<SyncTimeGUI.Pair> timeSync, float startDelay) {
        this.set2Count = set2Count;
        buildCount2MSec(set2Count, timeSync, startDelay);
    }

    public void buildCount2MSec(Map<String, Integer> set2Count, ArrayList<SyncTimeGUI.Pair> timeSync, float startDelay) {

        // Create time sync map for convenience
        HashMap<String, Float> timeSyncMap = new HashMap<>();
        for (SyncTimeGUI.Pair pair : timeSync) {
            timeSyncMap.put(pair.getKey(), pair.getValue());
        }

        count2MSec = new HashMap<>();

        // Get a mapping of set to number of counts in the set, which is more helpful
        set2CountSorted = (ArrayList<Map.Entry<String, Integer>>) ScrubBarGUI.sortMap(set2Count);

        set2NumCounts = new ArrayList<>();
        for (int i = 0; i < set2CountSorted.size() - 1; i++) {
            Map.Entry<String, Integer> current = set2CountSorted.get(i);
            Map.Entry<String, Integer> next = set2CountSorted.get(i + 1);

            int difference = next.getValue() - current.getValue();
            set2NumCounts.add(new AbstractMap.SimpleEntry<>(current.getKey(), difference));
        }

        // Calculate count2MSec
        int currentCount = 0;
        long prevCountMSec = (long) (startDelay * 1000); // Count 0 will begin at startDelay seconds
        for (Map.Entry<String, Integer> set2NumCountsEntry : set2NumCounts) {
            String set = set2NumCountsEntry.getKey();
            int numCounts = set2NumCountsEntry.getValue();
            long durationPerCount = (long) (timeSyncMap.get(set) / numCounts * 1000);

            // Account for set 1, which also contains count 0. Need to iterate an additional time
            if (set.equals("1")) numCounts += 1;

            // Create count2MSec entries for counts of this set
            for (int i = 0; i < numCounts; i++) {

                // Account for count 0 which does not use durationPerCount, but only startDelay
                if (set.equals("1") && i == 0)
                    count2MSec.put(currentCount++, prevCountMSec);
                else {
                    prevCountMSec += durationPerCount;
                    count2MSec.put(currentCount++, prevCountMSec);
                }
            }
        }

        // Build additional attributes for utility
        buildSet2MSec(set2CountSorted, count2MSec);
    }

    private void buildSet2MSec(ArrayList<Map.Entry<String, Integer>> set2CountSorted, HashMap<Integer, Long> count2MSec) {
        set2MSec = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : set2CountSorted) {
            set2MSec.add(new AbstractMap.SimpleEntry<>(entry.getKey(), count2MSec.get(entry.getValue())));
        }
    }

    public HashMap<Integer, Long> getCount2MSec() {
        return count2MSec;
    }

    public Map<String, Integer> getSet2Count() {
        return set2Count;
    }

    public ArrayList<Map.Entry<String, Integer>> getSet2NumCounts() {
        return set2NumCounts;
    }

    public ArrayList<Map.Entry<String, Integer>> getSet2CountSorted() {
        return set2CountSorted;
    }

    public ArrayList<Map.Entry<String, Long>> getSet2MSec() {
        return set2MSec;
    }

    public static void main(String[] args) {
        HashMap<String, Integer> set2CountDummy = new HashMap<>();
        set2CountDummy.put("1", 0);
        set2CountDummy.put("2", 16);
        set2CountDummy.put("3", 48);

        ArrayList<SyncTimeGUI.Pair> timeSyncDummy = new ArrayList<>();
        timeSyncDummy.add(new SyncTimeGUI.Pair("1", (float) 5));
        timeSyncDummy.add(new SyncTimeGUI.Pair("2", (float) 15));
        timeSyncDummy.add(new SyncTimeGUI.Pair("3", (float) 0));

        float startDelayDummy = (float) 0.5;

        TimeManager timeManagerDummy = new TimeManager(set2CountDummy, timeSyncDummy, startDelayDummy);
        System.out.println(timeManagerDummy.set2NumCounts);
        System.out.println(timeManagerDummy.count2MSec);
    }

}
