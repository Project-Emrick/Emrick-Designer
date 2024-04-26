package org.emrick.project.actions;

import org.emrick.project.Performer;
import org.emrick.project.effect.Effect;

public class EffectPerformerMap {
    private Effect effect;
    private Performer performer;

    public EffectPerformerMap(Effect effect, Performer performer) {
        this.effect = effect;
        this.performer = performer;
    }

    public Effect getEffect() {
        return effect;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
    }

    public Performer getPerformer() {
        return performer;
    }

    public void setPerformer(Performer performer) {
        this.performer = performer;
    }
}
