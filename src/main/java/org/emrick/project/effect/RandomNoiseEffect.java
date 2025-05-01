package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Random;

public class RandomNoiseEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private Duration duration;
    private boolean varyBrightness;
    private boolean varyColor;
    private boolean varyTime;
    private boolean fade;
    private float colorVariance;
    private float minBrightness;
    private float maxBrightness;
    private long maxTime;
    private long minTime;
    private Color color;
    private int id;

    public RandomNoiseEffect(long startTime, long endTime, Duration duration, boolean varyBrightness,
                             boolean varyColor, boolean varyTime, boolean fade, float colorVariance,
                             float minBrightness, float maxBrightness, long maxTime, long minTime, Color color, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.varyBrightness = varyBrightness;
        this.varyColor = varyColor;
        this.varyTime = varyTime;
        this.fade = fade;
        this.colorVariance = colorVariance;
        this.minBrightness = minBrightness;
        this.maxBrightness = maxBrightness;
        this.maxTime = maxTime;
        this.minTime = minTime;
        this.color = color;
        this.id = id;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.NOISE;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public Effect generateEffectObj() {
        Effect e = new Effect(startTime);
        e.setEndTimeMSec(endTime);
        e.setId(id);
        e.setEffectType(getEffectType());
        e.setDuration(duration);
        e.setVaryBrightness(varyBrightness);
        e.setVaryColor(varyColor);
        e.setVaryTime(varyTime);
        e.setFade(fade);
        e.setColorVariance(colorVariance);
        e.setMinBrightness(minBrightness);
        e.setMaxBrightness(maxBrightness);
        e.setMaxTime(maxTime);
        e.setMinTime(minTime);
        e.setFunction(LightingDisplay.Function.NOISE);
        e.setStartColor(color);
        return e;
    }

    @Override
    public ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips) {
        Random rand = new Random();
        ArrayList<EffectLEDStripMap> effects = new ArrayList<>();
        for (LEDStrip ledStrip : ledStrips) {
            ArrayList<Checkpoint> checkpoints = new ArrayList<>();
            if (varyTime) {
                int count = 0;
                ArrayList<Long> times = new ArrayList<>();
                long averageTime = (maxTime - minTime) / 2;
                long sum = 0;
                while (sum < duration.toMillis() - averageTime) {
                    long time = rand.nextLong(minTime, maxTime);
                    sum += time;
                    count++;
                    times.add(time);
                }
                times.add(duration.toMillis() - sum);
                count++;
                if (fade) {
                    count++;
                }
                float[] brightnesses = new float[count];
                if (varyBrightness) {
                    for (int i = 0; i < count; i++) {
                        brightnesses[i] = rand.nextFloat(minBrightness, maxBrightness);
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        brightnesses[i] = maxBrightness;
                    }
                }
                Color[] colors = new Color[count];
                if (varyColor) {
                    for (int i = 0; i < count; i++) {
                        int r, g, b;
                        r = rand.nextInt(color.getRed() - (int) (255.0 * colorVariance), color.getRed() + (int) (255.0 * colorVariance));
                        g = rand.nextInt(color.getGreen() - (int) (255.0 * colorVariance), color.getGreen() + (int) (255.0 * colorVariance));
                        b = rand.nextInt(color.getBlue() - (int) (255.0 * colorVariance), color.getBlue() + (int) (255.0 * colorVariance));
                        if (r < 0) {
                            r = 0;
                        }
                        if (g < 0) {
                            g = 0;
                        }
                        if (b < 0) {
                            b = 0;
                        }
                        if (r > 255) {
                            r = 255;
                        }
                        if (g > 255) {
                            g = 255;
                        }
                        if (b > 255) {
                            b = 255;
                        }
                        colors[i] = new Color(r, g, b);
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        colors[i] = color;
                    }
                }
                if (fade) {
                    count--;
                }
                for (int i = 0; i < count; i++) {
                    checkpoints.add(new Checkpoint(times.get(i), colors[i], brightnesses[i]));
                }
                if (fade) {
                    checkpoints.add(new Checkpoint(colors[count], brightnesses[count]));
                }
            } else {
                int count = (int) (duration.toMillis() / maxTime);
                long shortenLast = 0;
                if (maxTime * count < duration.toMillis()) {
                    shortenLast = duration.toMillis() - maxTime * count;
                    count++;
                }
                long[] times = new long[count];
                for (int i = 0; i < count; i++) {
                    times[i] = maxTime;
                }
                if (shortenLast > 0) {
                    times[count - 1] = maxTime - shortenLast;
                }
                if (fade) {
                    count++;
                }
                float[] brightnesses = new float[count];
                if (varyBrightness) {
                    for (int i = 0; i < count; i++) {
                        brightnesses[i] = rand.nextFloat(minBrightness, maxBrightness);
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        brightnesses[i] = maxBrightness;
                    }
                }
                Color[] colors = new Color[count];
                if (varyColor) {
                    for (int i = 0; i < count; i++) {
                        int r, g, b;
                        r = rand.nextInt(color.getRed() - (int) (255.0 * colorVariance), color.getRed() + (int) (255.0 * colorVariance));
                        g = rand.nextInt(color.getGreen() - (int) (255.0 * colorVariance), color.getGreen() + (int) (255.0 * colorVariance));
                        b = rand.nextInt(color.getBlue() - (int) (255.0 * colorVariance), color.getBlue() + (int) (255.0 * colorVariance));
                        if (r < 0) {
                            r = 0;
                        }
                        if (g < 0) {
                            g = 0;
                        }
                        if (b < 0) {
                            b = 0;
                        }
                        if (r > 255) {
                            r = 255;
                        }
                        if (g > 255) {
                            g = 255;
                        }
                        if (b > 255) {
                            b = 255;
                        }
                        colors[i] = new Color(r, g, b);
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        colors[i] = color;
                    }
                }
                if (fade) {
                    count--;
                }
                for (int i = 0; i < count; i++) {
                    checkpoints.add(new Checkpoint(times[i], colors[i], brightnesses[i]));
                }
                if (fade) {
                    checkpoints.add(new Checkpoint(colors[count], brightnesses[count]));
                }
            }
            Effect e = new Effect(startTime);
            e.setEndTimeMSec(endTime);
            e.setId(id);
            e.setEffectType(getEffectType());
            e.setDuration(duration);
            e.setVaryBrightness(varyBrightness);
            e.setVaryColor(varyColor);
            e.setVaryTime(varyTime);
            e.setFade(fade);
            e.setColorVariance(colorVariance);
            e.setMinBrightness(minBrightness);
            e.setMaxBrightness(maxBrightness);
            e.setMaxTime(maxTime);
            e.setMinTime(minTime);
            e.setFunction(LightingDisplay.Function.NOISE);
            e.setStartColor(color);
            e.setNoiseCheckpoints(checkpoints);
            effects.add(new EffectLEDStripMap(e, ledStrip));
        }

        return effects;
    }
}
