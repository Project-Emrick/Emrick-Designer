package org.emrick.project.effect;

import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class  Effect implements Cloneable, TimelineEvent {

    public static EffectListener effectListener;

    // Application
    public long startTimeMSec; // Based on position of scrub bar cursor when user first creates the effect
    public long endTimeMSec; // Calculated from start time, delay, duration, and timeout
    private GeneratedEffect generatedEffect;

    // Main Parameters
    private Color startColor;
    private Color endColor;
    private Duration delay;
    private Duration duration;
    private Duration timeout;
    private double speed;
    private double angle;
    private EffectList effectType;
    public int id;
    private LightingDisplay.Function function;
    private int size;
    private ArrayList<Color> chaseSequence;
    private int height;
    private int width;
    private GridShape[] shapes;
    private boolean varyBrightness;
    private boolean varyColor;
    private boolean varyTime;
    private boolean fade;
    private float colorVariance;
    private float minBrightness;
    private float maxBrightness;
    private long maxTime;
    private long minTime;
    private ArrayList<Checkpoint> noiseCheckpoints;

    // Bitflags
    private boolean USE_DURATION;
    private boolean SET_TIMEOUT;
    private boolean DO_DELAY;
    private boolean INSTANT_COLOR;
    private boolean upOrSide;
    private boolean direction;

    public Effect(long startTimeMSec) {
        this.startTimeMSec = startTimeMSec;
        this.startColor = Color.black;
        this.endColor = Color.black;
        this.delay = Duration.ZERO;
        this.duration = Duration.ZERO;
        this.timeout = Duration.ZERO;
        this.function = LightingDisplay.Function.DEFAULT;
        this.size = 0;
        this.USE_DURATION = true;
        this.SET_TIMEOUT = false;
        this.DO_DELAY = false;
        this.INSTANT_COLOR = true;
        this.upOrSide = false;
        this.direction = false;
        this.speed = 1;
        this.angle = 0;
        this.effectType = EffectList.HIDE_GROUPS;
        this.id = -1;
        this.chaseSequence = new ArrayList<>();
        this.height = 0;
        this.width = 0;
        this.varyBrightness = false;
        this.varyColor = false;
        this.varyTime = false;
        this.fade = false;
        this.colorVariance = 0;
        this.minBrightness = 0;
        this.maxBrightness = 0;
        this.maxTime = 0;
        this.minTime = 0;
        this.noiseCheckpoints = new ArrayList<>();
        calculateEndTimeMSec();
    }

    public Effect(long startTimeMSec,
                  Color startColor, Color endColor, Duration delay, Duration duration, Duration timeout,
                  boolean USE_DURATION, boolean SET_TIMEOUT, boolean DO_DELAY, boolean INSTANT_COLOR, int id) {
        this.startColor = startColor;
        this.endColor = endColor;
        this.delay = delay;
        this.duration = duration;
        this.timeout = timeout;
        this.USE_DURATION = USE_DURATION;
        this.SET_TIMEOUT = SET_TIMEOUT;
        this.DO_DELAY = DO_DELAY;
        this.INSTANT_COLOR = INSTANT_COLOR;
        this.function = LightingDisplay.Function.DEFAULT;
        this.size = 0;
        this.upOrSide = false;
        this.direction = false;
        this.speed = 1;
        this.angle = 0;
        this.startTimeMSec = startTimeMSec;
        this.effectType = EffectList.HIDE_GROUPS;
        this.id = id;
        this.chaseSequence = new ArrayList<>();
        this.height = 0;
        this.width = 0;
        this.varyBrightness = false;
        this.varyColor = false;
        this.varyTime = false;
        this.fade = false;
        this.colorVariance = 0;
        this.minBrightness = 0;
        this.maxBrightness = 0;
        this.maxTime = 0;
        this.minTime = 0;
        this.noiseCheckpoints = new ArrayList<>();
        calculateEndTimeMSec();
    }

    public boolean isVaryBrightness() {
        return varyBrightness;
    }

    public void setVaryBrightness(boolean varyBrightness) {
        this.varyBrightness = varyBrightness;
    }

    public boolean isVaryColor() {
        return varyColor;
    }

    public void setVaryColor(boolean varyColor) {
        this.varyColor = varyColor;
    }

    public boolean isVaryTime() {
        return varyTime;
    }

    public void setVaryTime(boolean varyTime) {
        this.varyTime = varyTime;
    }

    public boolean isFade() {
        return fade;
    }

    public void setFade(boolean fade) {
        this.fade = fade;
    }

    public float getColorVariance() {
        return colorVariance;
    }

    public void setColorVariance(float colorVariance) {
        this.colorVariance = colorVariance;
    }

    public float getMinBrightness() {
        return minBrightness;
    }

    public void setMinBrightness(float minBrightness) {
        this.minBrightness = minBrightness;
    }

    public float getMaxBrightness() {
        return maxBrightness;
    }

    public void setMaxBrightness(float maxBrightness) {
        this.maxBrightness = maxBrightness;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public void setMinTime(long minTime) {
        this.minTime = minTime;
    }

    public ArrayList<Checkpoint> getNoiseCheckpoints() {
        return noiseCheckpoints;
    }

    public void setNoiseCheckpoints(ArrayList<Checkpoint> noiseCheckpoints) {
        this.noiseCheckpoints = noiseCheckpoints;
    }

    public void setStartTimeMSec(long startTimeMSec) {
        this.startTimeMSec = startTimeMSec;
    }

    public GridShape[] getShapes() {
        return shapes;
    }

    public void setShapes(GridShape[] shapes) {
        this.shapes = shapes;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public LightingDisplay.Function getFunction() {
        return function;
    }

    public void setFunction(LightingDisplay.Function function) {
        this.function = function;
    }

    public void calculateEndTimeMSec() {

        // endTimeMSec depends on startTimeMSec, delay, duration, timeout, and bitflags
        this.endTimeMSec = startTimeMSec;
        if (DO_DELAY) this.endTimeMSec += delay.toMillis();
        if (USE_DURATION) this.endTimeMSec += duration.toMillis();
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
                case CHASE: generatedEffect = GeneratedEffectLoader.generateChaseEffectFromEffect(this); break;
                case GRID: generatedEffect = GeneratedEffectLoader.generateGridEffectFromEffect(this); break;
                case NOISE : generatedEffect = GeneratedEffectLoader.generateRandomNoiseEffectFromEffect(this); break;
            }
        }
        return generatedEffect;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public ArrayList<Color> getChaseSequence() {
        return chaseSequence;
    }

    public void setChaseSequence(ArrayList<Color> chaseSequence) {
        this.chaseSequence = chaseSequence;
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

    public boolean isUSE_DURATION() {
        return USE_DURATION;
    }

    public void setUSE_DURATION(boolean USE_DURATION) {
        this.USE_DURATION = USE_DURATION;
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

    public Effect makeDeepCopy() {
        Effect deepCopy = new Effect(this.getStartTimeMSec(),
                this.getStartColor(),
                this.getEndColor(),
                this.getDelay(),
                this.getDuration(),
                this.getTimeout(),
                this.isUSE_DURATION(),
                this.isSET_TIMEOUT(),
                this.isDO_DELAY(),
                this.isINSTANT_COLOR(),
                this.getId());
        deepCopy.setSpeed(this.getSpeed());
        deepCopy.setAngle(this.getAngle());
        deepCopy.setEffectType(this.getEffectType());
        deepCopy.setEndTimeMSec(this.getEndTimeMSec());
        deepCopy.setGeneratedEffect(this.getGeneratedEffect());
        deepCopy.setFunction(this.getFunction());
        deepCopy.setSize(this.size);
        deepCopy.setChaseSequence(this.getChaseSequence());
        deepCopy.setHeight(this.getHeight());
        deepCopy.setWidth(this.getWidth());
        deepCopy.setShapes(this.getShapes());
        deepCopy.setUpOrSide(this.upOrSide);
        deepCopy.setDirection(this.direction);
        deepCopy.setVaryBrightness(this.varyBrightness);
        deepCopy.setVaryColor(this.varyColor);
        deepCopy.setVaryTime(this.varyTime);
        deepCopy.setFade(this.fade);
        deepCopy.setColorVariance(this.colorVariance);
        deepCopy.setMaxBrightness(this.maxBrightness);
        deepCopy.setMinBrightness(this.minBrightness);
        deepCopy.setMaxTime(this.maxTime);
        deepCopy.setMinTime(this.minTime);
        deepCopy.setNoiseCheckpoints(this.noiseCheckpoints);

        return deepCopy;
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
            case CHASE: timeLineLabel = "Chase"; break;
            case GRID: timeLineLabel = "Grid"; break;
            case NOISE: timeLineLabel = "Random Noise"; break;
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

        widgetPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                widgetPanel.setBorder
                    (BorderFactory.createCompoundBorder(outerBorder, BorderFactory.createLineBorder(Color.blue, 2, true)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                widgetPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // signals scrub to this rf trigger on press
                effectListener.onPressEffect(Effect.this);
                widgetPanel.setBorder(BorderFactory.createLineBorder(Color.red, 2, true));
            }
        });

        return widgetPanel;
    }
}
