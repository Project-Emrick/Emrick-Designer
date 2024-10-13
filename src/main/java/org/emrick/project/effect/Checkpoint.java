package org.emrick.project.effect;

import java.awt.*;

public record Checkpoint(long time, Color color, float brightness) {
    public Checkpoint(long time, Color color, float brightness) {
        this.color = color;
        this.brightness = brightness;
        this.time = time;
    }
    public Checkpoint(Color color, float brightness) {
        this(0, color, brightness);
    }
}
