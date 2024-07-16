package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class StaticColorEffect implements GeneratedEffect {

    private long startTime;
    private long endTime;
    private Color staticColor;
    private Duration duration;
    private int id;

    public StaticColorEffect(long startTime, long endTime, Color staticColor, Duration duration, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.staticColor = staticColor;
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

    public Color getStaticColor() {
        return staticColor;
    }

    public void setStaticColor(Color staticColor) {
        this.staticColor = staticColor;
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
        return EffectList.STATIC_COLOR;
    }

    @Override
    public Effect generateEffectObj() {
        Effect effect = new Effect(this.getStartTime());
        effect.setEndTimeMSec(this.getEndTime());
        effect.setStartColor(this.getStaticColor());
        effect.setDelay(this.getDuration());
        effect.setDO_DELAY(true);
        effect.setUSE_DURATION(false);
        effect.setEffectType(EffectList.STATIC_COLOR);
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
