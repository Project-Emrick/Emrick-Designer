package org.emrick.project.effect;

import org.emrick.project.Performer;
import org.emrick.project.actions.EffectPerformerMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class StaticColorEffect implements GeneratedEffect {

    private long startTime;
    private long endTime;
    private Color staticColor;
    private Duration duration;
    private int id;

    public StaticColorEffect(long startTime, long endTime, Color staticColor, Duration duration, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.staticColor = staticColor;
        this.duration = duration;
        this.id = id;
    }

    public static StaticColorEffect getGeneratedEffectFromEffect(Effect e) {
        return new StaticColorEffect(e.getStartTimeMSec(), e.getEndTimeMSec(), e.getStartColor(), e.getDuration(), e.getId());
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


    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getEffectType() {
        return 2;
    }

    @Override
    public Effect generateEffectObj() {
        Effect effect = new Effect(this.getStartTime());
        effect.setEndTimeMSec(this.getEndTime());
        effect.setStartColor(this.getStaticColor());
        effect.setEndColor(null);
        effect.setDuration(this.getDuration());
        effect.setEffectType(EffectGUI.STATIC_COLOR);
        effect.setId(this.getId());
        return effect;
    }

    @Override
    public ArrayList<EffectPerformerMap> generateEffects(ArrayList<Performer> performers, Effect effect) {
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        effect.setGeneratedEffect(StaticColorEffect.getGeneratedEffectFromEffect(effect));
        for (Performer p : performers) {
            map.add(new EffectPerformerMap(effect, p));
        }
        return map;
    }
}
