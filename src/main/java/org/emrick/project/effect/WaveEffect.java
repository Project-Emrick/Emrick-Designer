package org.emrick.project.effect;

import org.emrick.project.Performer;
import org.emrick.project.actions.EffectPerformerMap;

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
    public int getEffectType() {
        return 5;
    }

    public ArrayList<EffectPerformerMap> generateEffects(ArrayList<Performer> performers, Effect effect) {
        WaveEffect waveEffect = new WaveEffect(effect.getStartTimeMSec(), effect.getEndTimeMSec(), effect.getStartColor(), effect.getEndColor(), effect.getDuration(), effect.getSpeed(), effect.isUpOrSide(), effect.isDirection(), effect.getId());
        int id = effect.getId();
        double startExtreme;
        double endExtreme;
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        if (effect.isUpOrSide()) {
            startExtreme = performers.get(0).currentLocation.getY();
            endExtreme = performers.get(0).currentLocation.getY();
        } else {
            startExtreme = performers.get(0).currentLocation.getX();
            endExtreme = performers.get(0).currentLocation.getX();
        }
        for (Performer p : performers) {
            if (effect.isUpOrSide()) {
                if (effect.isDirection()) { // down
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
                if (effect.isDirection()) { // right
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
        long wavePeriod = (long) (1.0/(1.0+effect.getSpeed()) * (double) effect.getDuration().toMillis());
        for (Performer p : performers) {
            long waveStartTime = 0;
            double extremeDiff = endExtreme - startExtreme;
            if (effect.isUpOrSide()) {
                double startDiff = p.currentLocation.getY() - startExtreme;
                double relativePosition = Math.abs(startDiff / extremeDiff);
                waveStartTime = (long) ((float) (effect.getDuration().toMillis() - wavePeriod) * relativePosition);
            } else {
                double startDiff = p.currentLocation.getX() - startExtreme;
                double relativePosition = Math.abs(startDiff / extremeDiff);
                waveStartTime = (long) ((float) (effect.getDuration().toMillis() - wavePeriod) * relativePosition);
            }
            Effect s1 = null;
            Effect w1 = null;
            Effect w2 = null;
            Effect s2 = null;
            if (waveStartTime != 0) {
                s1 = new Effect(effect.getStartTimeMSec());
                s1.setStartColor(effect.getStartColor());
                s1.setEndColor(effect.getStartColor());
                s1.setDuration(Duration.ofMillis(waveStartTime - 1));
            }
            long waveHalfDuration = wavePeriod / 2;
            w1 = new Effect(effect.getStartTimeMSec() + waveStartTime);
            w1.setStartColor(effect.getStartColor());
            w1.setEndColor(effect.getEndColor());
            w1.setDuration(Duration.ofMillis(waveHalfDuration-1));
            w2 = new Effect(effect.getStartTimeMSec() + waveStartTime + waveHalfDuration);
            w2.setStartColor(effect.getEndColor());
            w2.setEndColor(effect.getStartColor());
            w2.setDuration(Duration.ofMillis(waveHalfDuration-1));
            if (waveStartTime + 2 * waveHalfDuration < effect.getEndTimeMSec()) {
                s2 = new Effect(effect.getStartTimeMSec() + waveStartTime + waveHalfDuration * 2);
                s2.setStartColor(effect.getStartColor());
                s2.setEndColor(effect.getStartColor());
                s2.setDuration(Duration.ofMillis((effect.getEndTimeMSec() - w2.getEndTimeMSec())));

            } else {
                s1.setDuration(Duration.ofMillis(waveStartTime));
            }
            if (s1 != null) {
                s1.setId(id);
                s1.setEffectType(EffectGUI.WAVE);
                s1.setGeneratedEffect(waveEffect);
                map.add(new EffectPerformerMap(s1, p));
            }
            w1.setId(id);
            w1.setEffectType(EffectGUI.WAVE);
            w1.setGeneratedEffect(waveEffect);
            map.add(new EffectPerformerMap(w1, p));
            w2.setId(id);
            w2.setEffectType(EffectGUI.WAVE);
            w2.setGeneratedEffect(waveEffect);
            map.add(new EffectPerformerMap(w2, p));
            if (s2 != null) {
                s2.setId(id);
                s2.setEffectType(EffectGUI.WAVE);
                s2.setGeneratedEffect(waveEffect);
                map.add(new EffectPerformerMap(s2, p));
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
        effect.setEffectType(EffectGUI.WAVE);
        effect.setId(this.getId());
        return effect;
    }
}
