package org.emrick.project.effect;

import org.emrick.project.Performer;
import org.emrick.project.actions.EffectPerformerMap;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

public class AlternatingColorEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private Color color1;
    private Color color2;
    private Duration duration;
    private double rate;
    private int id;

    public AlternatingColorEffect(long startTime, long endTime, Color color1, Color color2, Duration duration, double rate, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.color1 = color1;
        this.color2 = color2;
        this.duration = duration;
        this.rate = rate;
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

    public Color getColor1() {
        return color1;
    }

    public void setColor1(Color color1) {
        this.color1 = color1;
    }

    public Color getColor2() {
        return color2;
    }

    public void setColor2(Color color2) {
        this.color2 = color2;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.ALTERNATING_COLOR;
    }

    @Override
    public Effect generateEffectObj() {
        Effect e = new Effect(startTime);
        e.setEndTimeMSec(endTime);
        e.setStartColor(color1);
        e.setEndColor(color2);
        e.setDuration(duration);
        e.setSpeed(rate);
        e.setEffectType(EffectList.ALTERNATING_COLOR);
        e.setId(id);
        return e;
    }

    @Override
    public ArrayList<EffectPerformerMap> generateEffects(ArrayList<Performer> performers) {
        ArrayList<EffectPerformerMap> map = new ArrayList<>();
        double doubleRate = rate * 2;
        double count = (double) duration.toMillis() * doubleRate / 1000;
        int fullLengthCount = (int) count;
        long packetLength = (long) (1 / doubleRate * 1000);
        long lengthLastPacket = this.getDuration().toMillis() - (fullLengthCount * packetLength);
        boolean useLastPacket = lengthLastPacket > 0;
        ArrayList<Effect> packets = new ArrayList<>();
        long nextStartTime = this.getStartTime();
        boolean even = true;
        for (int i = 0; i < fullLengthCount; i++) {
            Effect e = new Effect(nextStartTime);
            e.setDO_DELAY(true);
            e.setDelay(Duration.ofMillis(packetLength));
            nextStartTime += packetLength;
            if (even) {
                e.setStartColor(color1);
            } else {
                e.setStartColor(color2);
            }
            e.setEffectType(EffectList.ALTERNATING_COLOR);
            e.setId(id);
            e.setGeneratedEffect(this);
            packets.add(e);
            even = !even;
        }
        if (useLastPacket) {
            Effect e = new Effect(nextStartTime);
            e.setDO_DELAY(true);
            e.setDelay(Duration.ofMillis(lengthLastPacket));
            if (even) {
                e.setStartColor(color1);
            } else {
                e.setStartColor(color2);
            }
            e.setEffectType(EffectList.ALTERNATING_COLOR);
            e.setId(id);
            e.setGeneratedEffect(this);
            packets.add(e);
        }
        for (Performer p : performers) {
            for (Effect e : packets) {
                map.add(new EffectPerformerMap(e, p));
            }
        }
        return map;
    }
}
