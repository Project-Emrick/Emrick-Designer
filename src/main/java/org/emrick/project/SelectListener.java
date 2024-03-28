package org.emrick.project;

import java.util.HashSet;

public interface SelectListener {
    void onMultiSelect(HashSet<Integer> labels, HashSet<String> symbols);
}
