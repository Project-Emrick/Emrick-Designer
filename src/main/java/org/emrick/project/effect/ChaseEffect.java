package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class ChaseEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private ArrayList<Color> chaseSequence;
    private Duration duration;
    private boolean clockwise;
    private double speed;
    private int id;
    private LightingDisplay.Function function = LightingDisplay.Function.CHASE;

    public ChaseEffect(long startTime, long endTime, ArrayList<Color> chaseSequence, Duration duration, boolean clockwise, double speed, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.chaseSequence = chaseSequence;
        this.duration = duration;
        this.clockwise = clockwise;
        this.speed = speed;
        this.id = id;
    }

    public LightingDisplay.Function getFunction() {
        return function;
    }

    public void setFunction(LightingDisplay.Function function) {
        this.function = function;
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

    public ArrayList<Color> getChaseSequence() {
        return chaseSequence;
    }

    public void setChaseSequence(ArrayList<Color> chaseSequence) {
        this.chaseSequence = chaseSequence;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public void setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.CHASE;
    }

    @Override
    public Effect generateEffectObj() {
        Effect e = new Effect(startTime);
        e.setDuration(duration);
        e.setEffectType(EffectList.CHASE);
        e.setChaseSequence(chaseSequence);
        e.setDirection(clockwise);
        e.setSpeed(speed);
        e.setId(id);
        e.setFunction(function);
        e.setSize(chaseSequence.size()*3+2);
        e.setGeneratedEffect(this);
        return e;
    }

    @Override
    public ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips) {
        ArrayList<EffectLEDStripMap> maps = new ArrayList<>();
        for (LEDStrip ledStrip : ledStrips) {
            maps.add(new EffectLEDStripMap(generateEffectObj(), ledStrip));
        }
        return maps;
    }
}
