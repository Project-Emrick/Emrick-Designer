package org.emrick.project.effect;

import org.emrick.project.Performer;
import org.emrick.project.actions.EffectPerformerMap;

import java.util.ArrayList;

public interface GeneratedEffect {
    int getEffectType();
    Effect generateEffectObj();
    ArrayList<EffectPerformerMap> generateEffects(ArrayList<Performer> performers, Effect effect);
}
