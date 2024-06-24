package org.emrick.project.effect;

import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.Duration;
import java.util.Objects;

public class Effect implements Cloneable, TimelineEvent {

    // Application
    private final long startTimeMSec; // Based on position of scrub bar cursor when user first creates the effect
    private long endTimeMSec; // Calculated from start time, delay, duration, and timeout
    private GeneratedEffect generatedEffect;

    // Main Parameters
    private Color startColor;
    private Color endColor;
    private Duration delay;
    private Duration duration;
    private Duration timeout;
    private double speed;
    private double angle;

    // Bitflags
    private boolean TIME_GRADIENT;
    private boolean SET_TIMEOUT;
    private boolean DO_DELAY;
    private boolean INSTANT_COLOR;
    private boolean upOrSide;
    private boolean direction;
    private EffectList effectType;
    private int id;

    public Effect(long startTimeMSec) {
        this.startTimeMSec = startTimeMSec;
        this.startColor = Color.black;
        this.endColor = Color.black;
        this.delay = Duration.ZERO;
        this.duration = Duration.ZERO;
        this.timeout = Duration.ZERO;
        this.TIME_GRADIENT = true;
        this.SET_TIMEOUT = false;
        this.DO_DELAY = false;
        this.INSTANT_COLOR = true;
        this.upOrSide = false;
        this.direction = false;
        this.speed = 1;
        this.angle = 0;
        this.effectType = EffectList.HIDE_GROUPS;
        this.id = -1;
        calculateEndTimeMSec();
    }

    public Effect(long startTimeMSec,
                  Color startColor, Color endColor, Duration delay, Duration duration, Duration timeout,
                  boolean TIME_GRADIENT, boolean SET_TIMEOUT, boolean DO_DELAY, boolean INSTANT_COLOR, int id) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.delay = delay;
        this.duration = duration;
        this.timeout = timeout;
        this.TIME_GRADIENT = TIME_GRADIENT;
        this.SET_TIMEOUT = SET_TIMEOUT;
        this.DO_DELAY = DO_DELAY;
        this.INSTANT_COLOR = INSTANT_COLOR;
        this.upOrSide = false;
        this.direction = false;
        this.speed = 1;
        this.angle = 0;
        this.startTimeMSec = startTimeMSec;
        this.effectType = EffectList.HIDE_GROUPS;
        this.id = id;
        calculateEndTimeMSec();
    }

    public void calculateEndTimeMSec() {

        // endTimeMSec depends on startTimeMSec, delay, duration, timeout, and bitflags
        this.endTimeMSec = startTimeMSec;
        if (DO_DELAY) this.endTimeMSec += delay.toMillis();
        if (TIME_GRADIENT) this.endTimeMSec += duration.toMillis();
        if (SET_TIMEOUT) this.endTimeMSec += timeout.toMillis();
    }

    public GeneratedEffect getGeneratedEffect() {
        if (generatedEffect == null && effectType != EffectList.HIDE_GROUPS) {
            switch (effectType) {
                case GENERATED_FADE: generatedEffect = GeneratedEffectLoader.generateFadeEffectFromEffect(this); break;
                case STATIC_COLOR: generatedEffect = GeneratedEffectLoader.generateStaticColorEffectFromEffect(this); break;
                case WAVE: generatedEffect = GeneratedEffectLoader.generateWaveEffectFromEffect(this); break;
                case ALTERNATING_COLOR: generatedEffect = GeneratedEffectLoader.generateAlternatingColorEffectFromEffect(this); break;
                case RIPPLE: generatedEffect = GeneratedEffectLoader.generateRippleEffectFromEffect(this); break;
                case CIRCLE_CHASE: generatedEffect = GeneratedEffectLoader.generateCircleChaseEffectFromEffect(this); break;
            }
        }
        return generatedEffect;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void setGeneratedEffect(GeneratedEffect generatedEffect) {
        this.generatedEffect = generatedEffect;
    }

    public void setEndTimeMSec(long endTimeMSec) {
        this.endTimeMSec = endTimeMSec;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public EffectList getEffectType() {
        return effectType;
    }

    public void setEffectType(EffectList effectType) {
        this.effectType = effectType;
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

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public boolean isUpOrSide() {
        return upOrSide;
    }

    public void setUpOrSide(boolean upOrSide) {
        this.upOrSide = upOrSide;
    }

    public boolean isDirection() {
        return direction;
    }

    public void setDirection(boolean direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "Effect{" +
                "startTimeMSec=" + startTimeMSec +
                ", endTimeMSec=" + endTimeMSec +
                ", delay=" + delay.toMillis() +
                ", duration=" + duration.toMillis() +
                ", id=" + id +
                '}';
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
        return id == effect.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public JPanel getTimelineWidget() {
        Border outerBorder = BorderFactory.createLineBorder(Color.lightGray);
        Border innerBorder = BorderFactory.createEmptyBorder(2,2,2,2);

        JPanel widgetPanel = new JPanel(new GridLayout(5,1));
        widgetPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        String timeLineLabel;
        switch(effectType) {
            case GENERATED_FADE: timeLineLabel = "Fade"; break;
            case STATIC_COLOR: timeLineLabel = "Static Color"; break;
            case ALTERNATING_COLOR: timeLineLabel = "Alternating Color"; break;
            case RIPPLE: timeLineLabel = "Ripple"; break;
            case WAVE: timeLineLabel = "Wave"; break;
            case CIRCLE_CHASE: timeLineLabel = "Circle Chase"; break;
            default : timeLineLabel = "Default Pattern"; break;
        }

        JLabel titleLabel = new JLabel("<html><b>" + timeLineLabel + "</b></html>");
        JLabel startTimeLabel = new JLabel("Start: " + TimeManager.getFormattedTime(startTimeMSec));
        JLabel endTimeLabel = new JLabel("End: " + TimeManager.getFormattedTime(endTimeMSec));

        JPanel startColorPanel = new JPanel();
        startColorPanel.setPreferredSize(new Dimension(10, 10));
        startColorPanel.setBackground(startColor);

        JPanel endColorPanel = new JPanel();
        endColorPanel.setPreferredSize(new Dimension(10, 10));
        if (effectType == EffectList.STATIC_COLOR) {
            endColorPanel.setBackground(null);
        } else {
            endColorPanel.setBackground(endColor);
        }

        widgetPanel.add(titleLabel);
        widgetPanel.add(startTimeLabel);
        widgetPanel.add(endTimeLabel);
        widgetPanel.add(startColorPanel);
        widgetPanel.add(endColorPanel);

        widgetPanel.setPreferredSize(new Dimension(110, 85));

        return widgetPanel;
    }
}
