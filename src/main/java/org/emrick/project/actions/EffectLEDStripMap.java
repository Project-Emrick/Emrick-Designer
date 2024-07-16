package org.emrick.project.actions;

import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.effect.Effect;

public class EffectLEDStripMap {
    private Effect oldEffect;
    private Effect effect;
    private LEDStrip ledStrip;

    public EffectLEDStripMap(Effect effect, LEDStrip ledStrip) {
        this.effect = effect;
        this.ledStrip = ledStrip;
    }

    public EffectLEDStripMap(Effect oldEffect, Effect effect, LEDStrip ledStrip) {
        this.oldEffect = oldEffect;
        this.effect = effect;
        this.ledStrip = ledStrip;
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

    public LEDStrip getLedStrip() {
        return ledStrip;
    }

    public void setLedStrip(LEDStrip ledStrip) {
        this.ledStrip = ledStrip;
    }
}
