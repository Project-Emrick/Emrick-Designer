package org.emrick.project.effect;

import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.Duration;
import java.util.Objects;

public class Effect implements Cloneable, TimelineEvent {

    // Application
    private long startTimeMSec; // Based on position of scrub bar cursor when user first creates the effect
    private long endTimeMSec; // Calculated from start time, delay, duration, and timeout

    // Main Parameters
    private Color startColor;
    private Color endColor;
    private Duration delay;
    private Duration duration;
    private Duration timeout;

    // Bitflags
    private boolean TIME_GRADIENT;
    private boolean SET_TIMEOUT;
    private boolean DO_DELAY;
    private boolean INSTANT_COLOR;

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
        calculateEndTimeMSec();
    }

    public void calculateEndTimeMSec() {

        // endTimeMSec depends on startTimeMSec, delay, duration, timeout, and bitflags
        this.endTimeMSec = startTimeMSec;
        if (DO_DELAY) this.endTimeMSec += delay.toMillis();
        if (TIME_GRADIENT) this.endTimeMSec += duration.toMillis();
        if (SET_TIMEOUT) this.endTimeMSec += timeout.toMillis();
    }

    public long getStartTimeMSec() {
        return startTimeMSec;
    }

    public long getEndTimeMSec() {
        return endTimeMSec;
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
        calculateEndTimeMSec();
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
        calculateEndTimeMSec();
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
        calculateEndTimeMSec();
    }

    public boolean isTIME_GRADIENT() {
        return TIME_GRADIENT;
    }

    public void setTIME_GRADIENT(boolean TIME_GRADIENT) {
        this.TIME_GRADIENT = TIME_GRADIENT;
        calculateEndTimeMSec();
    }

    public boolean isSET_TIMEOUT() {
        return SET_TIMEOUT;
    }

    public void setSET_TIMEOUT(boolean SET_TIMEOUT) {
        this.SET_TIMEOUT = SET_TIMEOUT;
        calculateEndTimeMSec();
    }

    public boolean isDO_DELAY() {
        return DO_DELAY;
    }

    public void setDO_DELAY(boolean DO_DELAY) {
        this.DO_DELAY = DO_DELAY;
        calculateEndTimeMSec();
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

    @Override
    public JPanel getTimelineWidget() {
        Border outerBorder = BorderFactory.createLineBorder(Color.lightGray);
        Border innerBorder = BorderFactory.createEmptyBorder(2,2,2,2);

        JPanel widgetPanel = new JPanel(new GridLayout(5,1));
        widgetPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        JLabel titleLabel = new JLabel("<html><b>Effect</b></html>");
        JLabel startTimeLabel = new JLabel("Start: " + TimeManager.getFormattedTime(startTimeMSec));
        JLabel endTimeLabel = new JLabel("End: " + TimeManager.getFormattedTime(endTimeMSec));

        JPanel startColorPanel = new JPanel();
        startColorPanel.setPreferredSize(new Dimension(10, 10));
        startColorPanel.setBackground(startColor);

        JPanel endColorPanel = new JPanel();
        endColorPanel.setPreferredSize(new Dimension(10, 10));
        endColorPanel.setBackground(endColor);

        widgetPanel.add(titleLabel);
        widgetPanel.add(startTimeLabel);
        widgetPanel.add(endTimeLabel);
        widgetPanel.add(startColorPanel);
        widgetPanel.add(endColorPanel);

        widgetPanel.setPreferredSize(new Dimension(100, 85));

        return widgetPanel;
    }
}
