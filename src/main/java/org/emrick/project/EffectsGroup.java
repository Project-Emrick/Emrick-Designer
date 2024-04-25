package org.emrick.project;

import java.util.*;

public record EffectsGroup(String title, String description, Collection<String> performerIds) {

    @Override
    public String toString() {
        return title;
    }
}