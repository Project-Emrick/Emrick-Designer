package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.actions.EffectLEDStripMap;

import java.util.ArrayList;

public interface GeneratedEffect {
    EffectList getEffectType();
    Effect generateEffectObj();
    ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips);
}
