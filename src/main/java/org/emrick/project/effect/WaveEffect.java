package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class WaveEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private Color staticColor;
    private Color waveColor;
    private Duration duration;
    private double speed;
    private boolean vertical;
    private boolean upRight;
    private int id;

    public WaveEffect(long startTime, long endTime, Color staticColor, Color waveColor, Duration duration, double speed, boolean vertical, boolean upRight, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.staticColor = staticColor;
        this.waveColor = waveColor;
        this.duration = duration;
        this.speed = speed;
        this.vertical = vertical;
        this.upRight = upRight;
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

    public Color getStaticColor() {
        return staticColor;
    }

    public void setStaticColor(Color staticColor) {
        this.staticColor = staticColor;
    }

    public Color getWaveColor() {
        return waveColor;
    }

    public void setWaveColor(Color waveColor) {
        this.waveColor = waveColor;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public boolean isUpRight() {
        return upRight;
    }

    public void setUpRight(boolean upRight) {
        this.upRight = upRight;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.WAVE;
    }

    public ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips) {
        int id = this.getId();
        double startExtreme;
        double endExtreme;
        ArrayList<EffectLEDStripMap> map = new ArrayList<>();
        ArrayList<Performer> performers = new ArrayList<>();
        for (LEDStrip l : ledStrips) {
            if (!performers.contains(l.getPerformer())) {
                performers.add(l.getPerformer());
            }
        }
        if (this.isVertical()) {
            startExtreme = performers.get(0).currentLocation.getY();
            endExtreme = performers.get(0).currentLocation.getY();
        } else {
            startExtreme = performers.get(0).currentLocation.getX();
            endExtreme = performers.get(0).currentLocation.getX();
        }
        for (Performer p : performers) {
            if (this.isVertical()) {
                if (this.isUpRight()) { // down
                    if (p.currentLocation.getY() > startExtreme) {
                        startExtreme = p.currentLocation.getY();
                    }
                    if (p.currentLocation.getY() < endExtreme) {
                        endExtreme = p.currentLocation.getY();
                    }
                } else { // up
                    if (p.currentLocation.getY() < startExtreme) {
                        startExtreme = p.currentLocation.getY();
                    }
                    if (p.currentLocation.getY() > endExtreme) {
                        endExtreme = p.currentLocation.getY();
                    }
                }
            } else {
                if (this.isUpRight()) { // right
                    if (p.currentLocation.getX() < startExtreme) {
                        startExtreme = p.currentLocation.getX();
                    }
                    if (p.currentLocation.getX() > endExtreme) {
                        endExtreme = p.currentLocation.getX();
                    }
                } else { // left
                    if (p.currentLocation.getX() > startExtreme) {
                        startExtreme = p.currentLocation.getX();
                    }
                    if (p.currentLocation.getX() < endExtreme) {
                        endExtreme = p.currentLocation.getX();
                    }
                }
            }
        }
        long wavePeriod = (long) (1.0/(1.0+this.getSpeed()) * (double) this.getDuration().toMillis());
        for (LEDStrip l : ledStrips) {
            Performer p = l.getPerformer();
            long waveStartTime = 0;
            double extremeDiff = endExtreme - startExtreme;
            if (this.isVertical()) {
                double startDiff = p.currentLocation.getY() - startExtreme;
                double relativePosition = Math.abs(startDiff / extremeDiff);
                waveStartTime = (long) ((float) (this.getDuration().toMillis() - wavePeriod) * relativePosition);
            } else {
                double startDiff = p.currentLocation.getX() - startExtreme;
                double relativePosition = Math.abs(startDiff / extremeDiff);
                waveStartTime = (long) ((float) (this.getDuration().toMillis() - wavePeriod) * relativePosition);
            }
            Effect s1 = null;
            Effect w1 = null;
            Effect w2 = null;
            Effect s2 = null;
            if (waveStartTime != 0) {
                s1 = new Effect(this.getStartTime());
                s1.setStartColor(this.getStaticColor());
                s1.setEndColor(this.getStaticColor());
                s1.setDuration(Duration.ofMillis(waveStartTime));
            }
            long waveHalfDuration = wavePeriod / 2;
            w1 = new Effect(this.getStartTime() + waveStartTime);
            w1.setStartColor(this.getStaticColor());
            w1.setEndColor(this.getWaveColor());
            w1.setDuration(Duration.ofMillis(waveHalfDuration));
            w2 = new Effect(this.getStartTime() + waveStartTime + waveHalfDuration);
            w2.setStartColor(this.getWaveColor());
            w2.setEndColor(this.getStaticColor());
            w2.setDuration(Duration.ofMillis(waveHalfDuration));
            if (this.getStartTime() + waveStartTime + 2 * waveHalfDuration < this.getEndTime()) {
                s2 = new Effect(this.getStartTime() + waveStartTime + waveHalfDuration * 2);
                s2.setStartColor(this.getStaticColor());
                s2.setEndColor(this.getStaticColor());
                s2.setDuration(Duration.ofMillis((this.getEndTime() - w2.getEndTimeMSec())));

            }
            if (s1 != null) {
                s1.setId(id);
                s1.setEffectType(EffectList.WAVE);
                s1.setGeneratedEffect(this);
                map.add(new EffectLEDStripMap(s1, l));
            }
            w1.setId(id);
            w1.setEffectType(EffectList.WAVE);
            w1.setGeneratedEffect(this);
            map.add(new EffectLEDStripMap(w1, l));
            w2.setId(id);
            w2.setEffectType(EffectList.WAVE);
            w2.setGeneratedEffect(this);
            map.add(new EffectLEDStripMap(w2, l));
            if (s2 != null) {
                s2.setId(id);
                s2.setEffectType(EffectList.WAVE);
                s2.setGeneratedEffect(this);
                map.add(new EffectLEDStripMap(s2, l));
            }
        }
        return map;
    }

    @Override
    public Effect generateEffectObj() {
        Effect effect = new Effect(this.getStartTime());
        effect.setEndTimeMSec(this.getEndTime());
        effect.setStartColor(this.getStaticColor());
        effect.setEndColor(this.getWaveColor());
        effect.setDuration(this.getDuration());
        effect.setSpeed(this.getSpeed());
        effect.setUpOrSide(this.isVertical());
        effect.setDirection(this.isUpRight());
        effect.setEffectType(EffectList.WAVE);
        effect.setId(this.getId());
        return effect;
    }
}
