package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class AlternatingColorEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private Color color1;
    private Color color2;
    private Duration duration;
    private double rate;
    private int id;

    public AlternatingColorEffect(long startTime, long endTime, Color color1, Color color2, Duration duration, double rate, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.color1 = color1;
        this.color2 = color2;
        this.duration = duration;
        this.rate = rate;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public Color getColor1() {
        return color1;
    }

    public void setColor1(Color color1) {
        this.color1 = color1;
    }

    public Color getColor2() {
        return color2;
    }

    public void setColor2(Color color2) {
        this.color2 = color2;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.ALTERNATING_COLOR;
    }

    @Override
    public Effect generateEffectObj() {
        Effect e = new Effect(startTime);
        e.setEndTimeMSec(endTime);
        e.setStartColor(color1);
        e.setEndColor(color2);
        e.setDuration(duration);
        e.setSpeed(rate);
        e.setFunction(LightingDisplay.Function.ALTERNATING_COLOR);
        e.setEffectType(EffectList.ALTERNATING_COLOR);
        e.setId(id);
        e.setSize(1);
        e.setGeneratedEffect(this);
        return e;
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
