package org.emrick.project;

public class Set {
    public String label;
    public int index;
    public int duration;

    public Set(String label, int index, int duration) {
        this.label = label;
        this.index = index;
        this.duration = duration;
    }

    public int compareTo(Set s) {
        String[] thisComponents = label.split("-");
        String[] thatComponents = s.label.split("-");
        if (thisComponents.length != thatComponents.length) {
            if (thisComponents.length < thatComponents.length) {
                return -1;
            } else {
                return 1;
            }
        }
        int thisSetIndex;
        int thatSetIndex;
        if (thisComponents.length > 1) {
            int thisMovementIndex = Integer.parseInt(thisComponents[0]);
            int thatMovementIndex = Integer.parseInt(thatComponents[0]);
            if (thisMovementIndex != thatMovementIndex) {
                return thisMovementIndex - thatMovementIndex;
            }
            String thisSetLabel = thisComponents[1].replaceAll("[^0-9.]", "");
            String thatSetLabel = thatComponents[1].replaceAll("[^0-9.]", "");
            thisSetIndex = Integer.parseInt(thisSetLabel);
            thatSetIndex = Integer.parseInt(thatSetLabel);
            if (thisSetIndex != thatSetIndex) {
                return thisSetIndex - thatSetIndex;
            } else {
                return thisSetLabel.compareTo(thatSetLabel);
            }
        }
        String thisSetLabel = thisComponents[0].replaceAll("[^0-9.]", "");
        String thatSetLabel = thatComponents[0].replaceAll("[^0-9.]", "");
        thisSetIndex = Integer.parseInt(thisSetLabel);
        thatSetIndex = Integer.parseInt(thatSetLabel);
        if (thisSetIndex != thatSetIndex) {
            return thisSetIndex - thatSetIndex;
        } else {
            return thisSetLabel.compareTo(thatSetLabel);
        }
    }

    public boolean equalsString(String obj) {
        if (obj.equals(label)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Set s) {
            return this.label.equals(s.label);
        } else {
            return false;
        }
    }

    public String toString() {
        return label;
    }
}
