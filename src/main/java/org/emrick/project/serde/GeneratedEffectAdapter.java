package org.emrick.project.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.emrick.project.effect.*;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;

public class GeneratedEffectAdapter extends TypeAdapter<GeneratedEffect> {
    @Override
    public void write(JsonWriter writer, GeneratedEffect generatedEffect) throws IOException {
        if (generatedEffect == null) {
            // color isn't defined so we'll ignore
            writer.nullValue();
            return;
        }

        if (generatedEffect.getEffectType() == EffectGUI.WAVE) {
            WaveEffect waveEffect = (WaveEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(waveEffect.getStartTime());
            writer.name("endTime");
            writer.value(waveEffect.getEndTime());
            writer.name("staticColorR");
            writer.value(waveEffect.getStaticColor().getRed());
            writer.name("staticColorG");
            writer.value(waveEffect.getStaticColor().getGreen());
            writer.name("staticColorB");
            writer.value(waveEffect.getStaticColor().getBlue());
            writer.name("waveColorR");
            writer.value(waveEffect.getWaveColor().getRed());
            writer.name("waveColorG");
            writer.value(waveEffect.getWaveColor().getGreen());
            writer.name("waveColorB");
            writer.value(waveEffect.getWaveColor().getBlue());
            writer.name("duration");
            writer.value(waveEffect.getDuration().toMillis());
            writer.name("speed");
            writer.value(waveEffect.getSpeed());
            writer.name("vertical");
            writer.value(waveEffect.isVertical());
            writer.name("upRight");
            writer.value(waveEffect.isUpRight());
            writer.name("id");
            writer.value(waveEffect.getId());
            writer.name("effectType");
            writer.value(waveEffect.getEffectType());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectGUI.GENERATED_FADE) {
            FadeEffect fadeEffect = (FadeEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(fadeEffect.getStartTime());
            writer.name("endTime");
            writer.value(fadeEffect.getEndTime());
            writer.name("startColorR");
            writer.value(fadeEffect.getStartColor().getRed());
            writer.name("startColorG");
            writer.value(fadeEffect.getStartColor().getGreen());
            writer.name("startColorB");
            writer.value(fadeEffect.getStartColor().getBlue());
            writer.name("endColorR");
            writer.value(fadeEffect.getEndColor().getRed());
            writer.name("endColorG");
            writer.value(fadeEffect.getEndColor().getGreen());
            writer.name("endColorB");
            writer.value(fadeEffect.getEndColor().getBlue());
            writer.name("duration");
            writer.value(fadeEffect.getDuration().toMillis());
            writer.name("id");
            writer.value(fadeEffect.getId());
            writer.name("effectType");
            writer.value(fadeEffect.getEffectType());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectGUI.STATIC_COLOR) {
            StaticColorEffect staticColorEffect = (StaticColorEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(staticColorEffect.getStartTime());
            writer.name("endTime");
            writer.value(staticColorEffect.getEndTime());
            writer.name("startColorR");
            writer.value(staticColorEffect.getStaticColor().getRed());
            writer.name("startColorG");
            writer.value(staticColorEffect.getStaticColor().getGreen());
            writer.name("startColorB");
            writer.value(staticColorEffect.getStaticColor().getBlue());
            writer.name("duration");
            writer.value(staticColorEffect.getDuration().toMillis());
            writer.name("id");
            writer.value(staticColorEffect.getId());
            writer.name("effectType");
            writer.value(staticColorEffect.getEffectType());
            writer.endObject();
        } else {
            writer.nullValue();
        }
    }

    @Override
    public GeneratedEffect read(JsonReader reader) throws IOException {
        Long startTime = null;
        Long endTime = null;
        Integer staticColorR = null;
        Integer staticColorG = null;
        Integer staticColorB = null;
        Integer waveColorR = null;
        Integer waveColorG = null;
        Integer waveColorB = null;
        Integer startColorR = null;
        Integer startColorG = null;
        Integer startColorB = null;
        Integer endColorR = null;
        Integer endColorG = null;
        Integer endColorB = null;
        Long duration = null;
        Double speed = null;
        Boolean vertical = null;
        Boolean upRight = null;
        Integer id = null;
        String fieldname = null;
        Integer effectType = null;
        if (reader.peek().equals(JsonToken.NULL)) {
            reader.nextNull();
            return null;
        }

        reader.beginObject();
        while (reader.hasNext()) {
            JsonToken token = reader.peek();

            if (token.equals(JsonToken.NAME)) {
                fieldname = reader.nextName();
            }

            if ("startTime".equals(fieldname)) {
                startTime = reader.nextLong();
            } else if ("endTime".equals(fieldname)) {
                endTime = reader.nextLong();
            } else if ("staticColorR".equals(fieldname)) {
                staticColorR = Integer.valueOf(reader.nextInt());
            } else if ("staticColorG".equals(fieldname)) {
                staticColorG = Integer.valueOf(reader.nextInt());
            } else if ("staticColorB".equals(fieldname)) {
                staticColorB = Integer.valueOf(reader.nextInt());
            } else if ("waveColorR".equals(fieldname)) {
                waveColorR = Integer.valueOf(reader.nextInt());
            } else if ("waveColorG".equals(fieldname)) {
                waveColorG = Integer.valueOf(reader.nextInt());
            } else if ("waveColorB".equals(fieldname)) {
                waveColorB = Integer.valueOf(reader.nextInt());
            } else if ("startColorR".equals(fieldname)) {
                startColorR = Integer.valueOf(reader.nextInt());
            } else if ("startColorG".equals(fieldname)) {
                startColorG = Integer.valueOf(reader.nextInt());
            } else if ("startColorB".equals(fieldname)) {
                startColorB = Integer.valueOf(reader.nextInt());
            } else if ("endColorR".equals(fieldname)) {
                endColorR = Integer.valueOf(reader.nextInt());
            } else if ("endColorG".equals(fieldname)) {
                endColorG = Integer.valueOf(reader.nextInt());
            } else if ("endColorB".equals(fieldname)) {
                endColorB = Integer.valueOf(reader.nextInt());
            } else if ("duration".equals(fieldname)) {
                duration = Long.valueOf(reader.nextLong());
            } else if ("speed".equals(fieldname)) {
                speed = Double.valueOf(reader.nextDouble());
            } else if ("vertical".equals(fieldname)) {
                vertical = Boolean.valueOf(reader.nextBoolean());
            } else if ("upRight".equals(fieldname)) {
                upRight = Boolean.valueOf(reader.nextBoolean());
            } else if ("id".equals(fieldname)) {
                id = Integer.valueOf(reader.nextInt());
            } else if ("effectType".equals(fieldname)) {
                effectType = Integer.valueOf(reader.nextInt());
            }
        }
        reader.endObject();
        if (effectType != null) {
            if (effectType == EffectGUI.WAVE) {

                if (startTime == null) {
                    throw new IOException("failed to get startTime component for GeneratedEffect");
                } else if (endTime == null) {
                    throw new IOException("failed to get endTime component for GeneratedEffect");
                } else if (staticColorR == null) {
                    throw new IOException("failed to get staticColorR component for GeneratedEffect");
                } else if (staticColorG == null) {
                    throw new IOException("failed to get staticColorG component for GeneratedEffect");
                } else if (staticColorB == null) {
                    throw new IOException("failed to get staticColorB component for GeneratedEffect");
                } else if (waveColorR == null) {
                    throw new IOException("failed to get waveColorR component for GeneratedEffect");
                } else if (waveColorG == null) {
                    throw new IOException("failed to get waveColorG component for GeneratedEffect");
                } else if (waveColorB == null) {
                    throw new IOException("failed to get waveColorB component for GeneratedEffect");
                } else if (duration == null) {
                    throw new IOException("failed to get duration component for GeneratedEffect");
                } else if (vertical == null) {
                    throw new IOException("failed to get vertical component for GeneratedEffect");
                } else if (upRight == null) {
                    throw new IOException("failed to get upRight component for GeneratedEffect");
                } else if (id == null) {
                    throw new IOException("failed to get id component for GeneratedEffect");
                } else if (speed == null) {
                    throw new IOException("failed to get speed component for GeneratedEffect");
                }

                return new WaveEffect(startTime, endTime,
                                    new Color(staticColorR, staticColorG, staticColorB),
                                    new Color(waveColorR, waveColorG, waveColorB),
                                    Duration.ofMillis(duration), speed, vertical, upRight, id);
            }
        } else if (effectType == EffectGUI.GENERATED_FADE) {
            if (startTime == null) {
                throw new IOException("failed to get startTime component for GeneratedEffect");
            } else if (endTime == null) {
                throw new IOException("failed to get endTime component for GeneratedEffect");
            } else if (startColorR == null) {
                throw new IOException("failed to get staticColorR component for GeneratedEffect");
            } else if (startColorG == null) {
                throw new IOException("failed to get staticColorG component for GeneratedEffect");
            } else if (startColorB == null) {
                throw new IOException("failed to get staticColorB component for GeneratedEffect");
            } else if (endColorR == null) {
                throw new IOException("failed to get waveColorR component for GeneratedEffect");
            } else if (endColorG == null) {
                throw new IOException("failed to get waveColorG component for GeneratedEffect");
            } else if (endColorB == null) {
                throw new IOException("failed to get waveColorB component for GeneratedEffect");
            } else if (duration == null) {
                throw new IOException("failed to get duration component for GeneratedEffect");
            } else if (id == null) {
                throw new IOException("failed to get id component for GeneratedEffect");
            }

            return new FadeEffect(startTime, endTime,
                    new Color(startColorR, startColorG, startColorB),
                    new Color(endColorR, endColorG, endColorB),
                    Duration.ofMillis(duration), id);
        } else if (effectType == EffectGUI.STATIC_COLOR) {
            if (startTime == null) {
                throw new IOException("failed to get startTime component for GeneratedEffect");
            } else if (endTime == null) {
                throw new IOException("failed to get endTime component for GeneratedEffect");
            } else if (staticColorR == null) {
                throw new IOException("failed to get staticColorR component for GeneratedEffect");
            } else if (staticColorG == null) {
                throw new IOException("failed to get staticColorG component for GeneratedEffect");
            } else if (staticColorB == null) {
                throw new IOException("failed to get staticColorB component for GeneratedEffect");
            } else if (duration == null) {
                throw new IOException("failed to get duration component for GeneratedEffect");
            } else if (id == null) {
                throw new IOException("failed to get id component for GeneratedEffect");
            }

            return new StaticColorEffect(startTime, endTime,
                    new Color(staticColorR, staticColorG, staticColorB),
                    Duration.ofMillis(duration), id);
        } else {
            throw new IOException("failed to get effectType component for GeneratedEffect");
        }
        return null;
    }
}
