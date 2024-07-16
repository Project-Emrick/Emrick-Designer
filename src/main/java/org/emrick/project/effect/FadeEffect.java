package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class FadeEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private Color startColor;
    private Color endColor;
    private Duration duration;
    private int id;

    public FadeEffect(long startTime, long endTime, Color startColor, Color endColor, Duration duration, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startColor = startColor;
        this.endColor = endColor;
        this.duration = duration;
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Color getStartColor() {
        return startColor;
    }

    public void setStartColor(Color startColor) {
        this.startColor = startColor;
    }

    public Color getEndColor() {
        return endColor;
    }

    public void setEndColor(Color endColor) {
        this.endColor = endColor;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.GENERATED_FADE;
    }

    @Override
    public Effect generateEffectObj() {
        Effect effect = new Effect(this.getStartTime());
        effect.setEndTimeMSec(this.getEndTime());
        effect.setStartColor(this.getStartColor());
        effect.setEndColor(this.getEndColor());
        effect.setDuration(this.getDuration());
        effect.setEffectType(EffectList.GENERATED_FADE);
        effect.setId(this.getId());
        return effect;
    }

    @Override
    public ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips) {
        ArrayList<EffectLEDStripMap> map = new ArrayList<>();

        for (LEDStrip ledStrip : ledStrips) {
            map.add(new EffectLEDStripMap(generateEffectObj(), ledStrip));
        }
        return map;
    }
}
