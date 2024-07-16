package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.awt.geom.Point2D;
import java.time.Duration;
import java.util.ArrayList;

public class CircleChaseEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private Color startColor;
    private Color endColor;
    private Duration duration;
    private boolean clockwise;
    private double startAngle;
    private double speed;
    private int id;

    public CircleChaseEffect(long startTime, long endTime, Color startColor, Color endColor, Duration duration, boolean clockwise, double startAngle, double speed, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startColor = startColor;
        this.endColor = endColor;
        this.duration = duration;
        this.clockwise = clockwise;
        this.startAngle = startAngle;
        this.speed = speed;
        this.id = id;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(double startAngle) {
        this.startAngle = startAngle;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
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

    public boolean isClockwise() {
        return clockwise;
    }

    public void setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.CIRCLE_CHASE;
    }

    @Override
    public Effect generateEffectObj() {
        Effect e = new Effect(startTime);
        e.setDuration(duration);
        e.setStartColor(startColor);
        e.setEndColor(endColor);
        e.setSpeed(speed);
        e.setDirection(clockwise);
        e.setAngle(startAngle);
        e.setEffectType(EffectList.CIRCLE_CHASE);
        e.setId(id);
        return e;
    }

    @Override
    public String toString() {
        return "CircleChaseEffect{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", startColor=" + startColor +
                ", endColor=" + endColor +
                ", duration=" + duration +
                ", clockwise=" + clockwise +
                ", startAngle=" + startAngle +
                ", speed=" + speed +
                ", id=" + id +
                '}';
    }

    @Override
    public ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips) {
        int id = this.getId();
        ArrayList<Performer> performers = new ArrayList<>();
        for (LEDStrip ledStrip : ledStrips) {
            if (!performers.contains(ledStrip.getPerformer())) {
                performers.add(ledStrip.getPerformer());
            }
        }
        double xExtremeL = performers.get(0).currentLocation.getX();
        double xExtremeR = performers.get(0).currentLocation.getX();
        double yExtremeT = performers.get(0).currentLocation.getY();
        double yExtremeB = performers.get(0).currentLocation.getY();
        ArrayList<EffectLEDStripMap> map = new ArrayList<>();
        for (Performer p : performers) {
            if (p.currentLocation.getX() < xExtremeL) {
                xExtremeL = p.currentLocation.getX();
            }
            if (p.currentLocation.getX() > xExtremeR) {
                xExtremeR = p.currentLocation.getX();
            }
            if (p.currentLocation.getY() < yExtremeT) {
                yExtremeT = p.currentLocation.getY();
            }
            if (p.currentLocation.getY() > yExtremeB) {
                yExtremeB = p.currentLocation.getY();
            }
        }

        Point2D center = new Point2D.Double((xExtremeL + xExtremeR) / 2, (yExtremeT + yExtremeB) / 2);

        long wavePeriod = (long) (1.0/(1.0+this.getSpeed()) * (double) this.getDuration().toMillis());
        for (LEDStrip l : ledStrips) {
            Performer p = l.getPerformer();
            long waveStartTime = 0;
            double xdiff = (p.currentLocation.getX() - center.getX());
            double ydiff = -(p.currentLocation.getY() - center.getY());
            double angle = Math.atan(ydiff / xdiff) * 180 / Math.PI;
            if (xdiff != 0) {
                if (xdiff > 0) {
                    angle = (angle + 360) % 360;
                } else {
                    angle = (angle + 180) % 360;
                }
            } else {
                if (ydiff > 0) {
                    angle = 90;
                } else if (ydiff < 0) {
                    angle = 270;
                } else {
                    angle = startAngle;
                }
            }
            if (this.isClockwise()) {
                double tmp = startAngle;
                if (angle > startAngle) {
                    tmp += 360;
                }
                double relativePosition = (tmp - angle) / 360;
                waveStartTime = (long) ((float) (this.getDuration().toMillis() - wavePeriod) * relativePosition);
            } else {
                if (angle < startAngle) {
                    angle += 360;
                }
                double relativePosition = (angle - startAngle) / 360;
                waveStartTime = (long) ((float) (this.getDuration().toMillis() - wavePeriod) * relativePosition);
            }
            Effect s1 = null;
            Effect w1 = null;
            Effect s2 = null;
            if (waveStartTime != 0) {
                s1 = new Effect(this.getStartTime());
                s1.setStartColor(this.getStartColor());
                s1.setEndColor(this.getStartColor());
                s1.setDuration(Duration.ofMillis(waveStartTime));
            }
            w1 = new Effect(this.getStartTime() + waveStartTime);
            w1.setStartColor(this.getStartColor());
            w1.setEndColor(this.getEndColor());
            w1.setDuration(Duration.ofMillis(wavePeriod));

            if (this.getStartTime() + waveStartTime + wavePeriod < this.getEndTime()) {
                s2 = new Effect(this.getStartTime() + waveStartTime + wavePeriod);
                s2.setStartColor(this.getEndColor());
                s2.setEndColor(this.getEndColor());
                s2.setDuration(Duration.ofMillis((this.getEndTime() - w1.getEndTimeMSec())));

            }
            if (s1 != null) {
                s1.setId(id);
                s1.setEffectType(EffectList.CIRCLE_CHASE);
                s1.setGeneratedEffect(this);
                map.add(new EffectLEDStripMap(s1, l));
            }
            w1.setId(id);
            w1.setEffectType(EffectList.CIRCLE_CHASE);
            w1.setGeneratedEffect(this);
            map.add(new EffectLEDStripMap(w1, l));
            if (s2 != null) {
                s2.setId(id);
                s2.setEffectType(EffectList.CIRCLE_CHASE);
                s2.setGeneratedEffect(this);
                map.add(new EffectLEDStripMap(s2, l));
            }
        }
        return map;
    }
}
