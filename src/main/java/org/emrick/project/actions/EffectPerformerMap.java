package org.emrick.project.actions;

import org.emrick.project.Performer;
import org.emrick.project.effect.Effect;

public class EffectPerformerMap {
    private Effect oldEffect;
    private Effect effect;
    private Performer performer;

    public EffectPerformerMap(Effect effect, Performer performer) {
        this.effect = effect;
        this.performer = performer;
    }

    public EffectPerformerMap(Effect oldEffect, Effect effect, Performer performer) {
        this.oldEffect = oldEffect;
        this.effect = effect;
        this.performer = performer;
    }

    public Effect getOldEffect() {
        return oldEffect;
    }

    public void setOldEffect(Effect oldEffect) {
        this.oldEffect = oldEffect;
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
