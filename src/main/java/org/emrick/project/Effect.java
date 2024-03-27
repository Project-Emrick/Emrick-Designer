package org.emrick.project;

import java.awt.*;
import java.time.Duration;
import java.util.Objects;

public class Effect implements Cloneable {

    // Application
    long startTimeMSec; // Based on position of scrub bar cursor when user first creates the effect
    long endTimeMSec; // Calculated from start time, delay, duration, and timeout

    // Main Parameters
    Color startColor;
    Color endColor;
    Duration delay;
    Duration duration;
    Duration timeout;

    // Bitflags
    boolean TIME_GRADIENT;
    boolean SET_TIMEOUT;
    boolean DO_DELAY;
    boolean INSTANT_COLOR;

    public Effect(long startTimeMSec,
                  Color startColor, Color endColor, Duration delay, Duration duration, Duration timeout,
                  boolean TIME_GRADIENT, boolean SET_TIMEOUT, boolean DO_DELAY, boolean INSTANT_COLOR) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.delay = delay;
        this.duration = duration;
        this.timeout = timeout;
        this.TIME_GRADIENT = TIME_GRADIENT;
        this.SET_TIMEOUT = SET_TIMEOUT;
        this.DO_DELAY = DO_DELAY;
        this.INSTANT_COLOR = INSTANT_COLOR;

        this.startTimeMSec = startTimeMSec;
        this.endTimeMSec = startTimeMSec;
        if (DO_DELAY) this.endTimeMSec += delay.toMillis();
        if (TIME_GRADIENT) this.endTimeMSec += duration.toMillis();
        if (SET_TIMEOUT) this.endTimeMSec += timeout.toMillis();
    }

    public long getStartTimeMSec() {
        return startTimeMSec;
    }

    public void setStartTimeMSec(long startTimeMSec) {
        this.startTimeMSec = startTimeMSec;
    }

    public long getEndTimeMSec() {
        return endTimeMSec;
    }

    public void setEndTimeMSec(long endTimeMSec) {
        this.endTimeMSec = endTimeMSec;
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

    public Duration getDelay() {
        return delay;
    }

    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isTIME_GRADIENT() {
        return TIME_GRADIENT;
    }

    public void setTIME_GRADIENT(boolean TIME_GRADIENT) {
        this.TIME_GRADIENT = TIME_GRADIENT;
    }

    public boolean isSET_TIMEOUT() {
        return SET_TIMEOUT;
    }

    public void setSET_TIMEOUT(boolean SET_TIMEOUT) {
        this.SET_TIMEOUT = SET_TIMEOUT;
    }

    public boolean isDO_DELAY() {
        return DO_DELAY;
    }

    public void setDO_DELAY(boolean DO_DELAY) {
        this.DO_DELAY = DO_DELAY;
    }

    public boolean isINSTANT_COLOR() {
        return INSTANT_COLOR;
    }

    public void setINSTANT_COLOR(boolean INSTANT_COLOR) {
        this.INSTANT_COLOR = INSTANT_COLOR;
    }

    @Override
    public Effect clone() {
        try {
            // Color and Duration are immutable, so a shallow copy will work
            return (Effect) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Effect effect = (Effect) o;
        return startTimeMSec == effect.startTimeMSec &&
                endTimeMSec == effect.endTimeMSec &&
                TIME_GRADIENT == effect.TIME_GRADIENT &&
                SET_TIMEOUT == effect.SET_TIMEOUT &&
                DO_DELAY == effect.DO_DELAY &&
                INSTANT_COLOR == effect.INSTANT_COLOR &&
                Objects.equals(startColor, effect.startColor) &&
                Objects.equals(endColor, effect.endColor) &&
                Objects.equals(delay, effect.delay) &&
                Objects.equals(duration, effect.duration) &&
                Objects.equals(timeout, effect.timeout);
    }

}
