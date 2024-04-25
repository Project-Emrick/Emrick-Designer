package org.emrick.project;

import java.awt.*;
import java.util.ArrayList;

public class ColorFade extends GeneratedEffect {
    public Color startColor;
    public Color endColor;

    public ColorFade(long startTimeMsec, long endTimeMsec, Color startColor, Color endColor) {
        super(startTimeMsec, endTimeMsec);
        this.startColor = startColor;
        this.endColor = endColor;
    }

    @Override
    public void generate() {

    }
}
