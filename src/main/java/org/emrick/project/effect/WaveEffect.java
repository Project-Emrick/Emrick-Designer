package org.emrick.project.effect;

import java.awt.*;
import java.time.Duration;

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
        return 0;
    }
}
