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
        return this.label.compareTo(s.label);
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
