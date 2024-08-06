package org.emrick.project.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.emrick.project.effect.*;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

public class GeneratedEffectAdapter extends TypeAdapter<GeneratedEffect> {
    @Override
    public void write(JsonWriter writer, GeneratedEffect generatedEffect) throws IOException {
        if (generatedEffect == null) {
            // color isn't defined so we'll ignore
            writer.nullValue();
            return;
        }

        if (generatedEffect.getEffectType() == EffectList.WAVE) {
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
            writer.value(waveEffect.getEffectType().ordinal());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectList.GENERATED_FADE) {
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
            writer.value(fadeEffect.getEffectType().ordinal());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectList.STATIC_COLOR) {
            StaticColorEffect staticColorEffect = (StaticColorEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(staticColorEffect.getStartTime());
            writer.name("endTime");
            writer.value(staticColorEffect.getEndTime());
            writer.name("staticColorR");
            writer.value(staticColorEffect.getStaticColor().getRed());
            writer.name("staticColorG");
            writer.value(staticColorEffect.getStaticColor().getGreen());
            writer.name("staticColorB");
            writer.value(staticColorEffect.getStaticColor().getBlue());
            writer.name("duration");
            writer.value(staticColorEffect.getDuration().toMillis());
            writer.name("id");
            writer.value(staticColorEffect.getId());
            writer.name("effectType");
            writer.value(staticColorEffect.getEffectType().ordinal());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectList.ALTERNATING_COLOR) {
            AlternatingColorEffect alternatingColorEffect = (AlternatingColorEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(alternatingColorEffect.getStartTime());
            writer.name("endTime");
            writer.value(alternatingColorEffect.getEndTime());
            writer.name("color1R");
            writer.value(alternatingColorEffect.getColor1().getRed());
            writer.name("color1G");
            writer.value(alternatingColorEffect.getColor1().getGreen());
            writer.name("color1B");
            writer.value(alternatingColorEffect.getColor1().getBlue());
            writer.name("color2R");
            writer.value(alternatingColorEffect.getColor2().getRed());
            writer.name("color2G");
            writer.value(alternatingColorEffect.getColor2().getGreen());
            writer.name("color2B");
            writer.value(alternatingColorEffect.getColor2().getBlue());
            writer.name("duration");
            writer.value(alternatingColorEffect.getDuration().toMillis());
            writer.name("rate");
            writer.value(alternatingColorEffect.getRate());
            writer.name("id");
            writer.value(alternatingColorEffect.getId());
            writer.name("effectType");
            writer.value(alternatingColorEffect.getEffectType().ordinal());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectList.RIPPLE) {
            RippleEffect rippleEffect = (RippleEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(rippleEffect.getStartTime());
            writer.name("endTime");
            writer.value(rippleEffect.getEndTime());
            writer.name("staticColorR");
            writer.value(rippleEffect.getStaticColor().getRed());
            writer.name("staticColorG");
            writer.value(rippleEffect.getStaticColor().getGreen());
            writer.name("staticColorB");
            writer.value(rippleEffect.getStaticColor().getBlue());
            writer.name("waveColorR");
            writer.value(rippleEffect.getWaveColor().getRed());
            writer.name("waveColorG");
            writer.value(rippleEffect.getWaveColor().getGreen());
            writer.name("waveColorB");
            writer.value(rippleEffect.getWaveColor().getBlue());
            writer.name("duration");
            writer.value(rippleEffect.getDuration().toMillis());
            writer.name("speed");
            writer.value(rippleEffect.getSpeed());
            writer.name("vertical");
            writer.value(rippleEffect.isVertical());
            writer.name("upRight");
            writer.value(rippleEffect.isUpRight());
            writer.name("id");
            writer.value(rippleEffect.getId());
            writer.name("effectType");
            writer.value(rippleEffect.getEffectType().ordinal());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectList.CIRCLE_CHASE) {
            CircleChaseEffect circleChaseEffect = (CircleChaseEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(circleChaseEffect.getStartTime());
            writer.name("endTime");
            writer.value(circleChaseEffect.getEndTime());
            writer.name("startColorR");
            writer.value(circleChaseEffect.getStartColor().getRed());
            writer.name("startColorG");
            writer.value(circleChaseEffect.getStartColor().getGreen());
            writer.name("startColorB");
            writer.value(circleChaseEffect.getStartColor().getBlue());
            writer.name("endColorR");
            writer.value(circleChaseEffect.getEndColor().getRed());
            writer.name("endColorG");
            writer.value(circleChaseEffect.getEndColor().getGreen());
            writer.name("endColorB");
            writer.value(circleChaseEffect.getEndColor().getBlue());
            writer.name("duration");
            writer.value(circleChaseEffect.getDuration().toMillis());
            writer.name("speed");
            writer.value(circleChaseEffect.getSpeed());
            writer.name("angle");
            writer.value(circleChaseEffect.getStartAngle());
            writer.name("clockwise");
            writer.value(circleChaseEffect.isClockwise());
            writer.name("id");
            writer.value(circleChaseEffect.getId());
            writer.name("effectType");
            writer.value(circleChaseEffect.getEffectType().ordinal());
            writer.endObject();
        } else if (generatedEffect.getEffectType() == EffectList.CHASE) {
            ChaseEffect chaseEffect = (ChaseEffect) generatedEffect;
            writer.beginObject();
            writer.name("startTime");
            writer.value(chaseEffect.getStartTime());
            writer.name("endTime");
            writer.value(chaseEffect.getEndTime());
            for (int i = 0; i < chaseEffect.getChaseSequence().size(); i++) {
                writer.name("colorR" + i);
                writer.value(chaseEffect.getChaseSequence().get(i).getRed());
                writer.name("colorG" + i);
                writer.value(chaseEffect.getChaseSequence().get(i).getGreen());
                writer.name("colorB" + i);
                writer.value(chaseEffect.getChaseSequence().get(i).getBlue());
            }
            writer.name("duration");
            writer.value(chaseEffect.getDuration().toMillis());
            writer.name("speed");
            writer.value(chaseEffect.getSpeed());
            writer.name("clockwise");
            writer.value(chaseEffect.isClockwise());
            writer.name("id");
            writer.value(chaseEffect.getId());
            writer.name("effectType");
            writer.value(chaseEffect.getEffectType().ordinal());
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
        Integer color1R = null;
        Integer color1G = null;
        Integer color1B = null;
        Integer color2R = null;
        Integer color2G = null;
        Integer color2B = null;
        Boolean clockwise = null;
        Double rate = null;
        Double angle = null;
        Long duration = null;
        Double speed = null;
        Boolean vertical = null;
        Boolean upRight = null;
        Integer id = null;
        String fieldname = null;
        Integer effectType = null;
        ArrayList<Color> chaseSequence = null;
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
            } else if ("color1R".equals(fieldname)) {
                color1R = Integer.valueOf(reader.nextInt());
            } else if ("color1G".equals(fieldname)) {
                color1G = Integer.valueOf(reader.nextInt());
            } else if ("color1B".equals(fieldname)) {
                color1B = Integer.valueOf(reader.nextInt());
            } else if ("color2R".equals(fieldname)) {
                color2R = Integer.valueOf(reader.nextInt());
            } else if ("color2G".equals(fieldname)) {
                color2G = Integer.valueOf(reader.nextInt());
            } else if ("color2B".equals(fieldname)) {
                color2B = Integer.valueOf(reader.nextInt());
            } else if ("rate".equals(fieldname)) {
                rate = Double.valueOf(reader.nextDouble());
            } else if ("clockwise".equals(fieldname)) {
                clockwise = Boolean.valueOf(reader.nextBoolean());
            } else if ("angle".equals(fieldname)) {
                angle = Double.valueOf(reader.nextDouble());
            } else if (isChaseSequenceMember(fieldname)) {
                Integer data = Integer.valueOf(reader.nextInt());
                int index = Integer.parseInt(fieldname.substring(6));
                if (chaseSequence == null) {
                    chaseSequence = new ArrayList<>();
                }
                while (chaseSequence.size() <= index) {
                    chaseSequence.add(new Color(0,0,0));
                }
                switch (fieldname.charAt(5)) {
                    case 'R': chaseSequence.set(index, new Color(data, chaseSequence.get(index).getGreen(), chaseSequence.get(index).getBlue())); break;
                    case 'G': chaseSequence.set(index, new Color(chaseSequence.get(index).getRed(), data, chaseSequence.get(index).getBlue())); break;
                    case 'B': chaseSequence.set(index, new Color(chaseSequence.get(index).getRed(), chaseSequence.get(index).getGreen(), data)); break;
                }
            }
        }
        reader.endObject();
        if (effectType != null) {
            if (effectType == EffectList.WAVE.ordinal()) {

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
            } else if (effectType == EffectList.GENERATED_FADE.ordinal()) {
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
            } else if (effectType == EffectList.STATIC_COLOR.ordinal()) {
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
            } else if (effectType == EffectList.ALTERNATING_COLOR.ordinal()) {
                if (startTime == null) {
                    throw new IOException("failed to get startTime component for GeneratedEffect");
                } else if (endTime == null) {
                    throw new IOException("failed to get endTime component for GeneratedEffect");
                } else if (color1R == null) {
                    throw new IOException("failed to get color1R component for GeneratedEffect");
                } else if (color1G == null) {
                    throw new IOException("failed to get color1G component for GeneratedEffect");
                } else if (color1B == null) {
                    throw new IOException("failed to get color1B component for GeneratedEffect");
                } else if (color2R == null) {
                    throw new IOException("failed to get color2R component for GeneratedEffect");
                } else if (color2G == null) {
                    throw new IOException("failed to get color2G component for GeneratedEffect");
                } else if (color2B == null) {
                    throw new IOException("failed to get color2B component for GeneratedEffect");
                } else if (duration == null) {
                    throw new IOException("failed to get duration component for GeneratedEffect");
                } else if (id == null) {
                    throw new IOException("failed to get id component for GeneratedEffect");
                } else if (rate == null) {
                    throw new IOException("failed to get rate component for GeneratedEffect");
                }

                return new AlternatingColorEffect(startTime, endTime,
                        new Color(color1R, color1G, color1B),
                        new Color(color2R, color2G, color2B),
                        Duration.ofMillis(duration), rate, id);
            } else if (effectType == EffectList.RIPPLE.ordinal()) {
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

                return new RippleEffect(startTime, endTime,
                        new Color(staticColorR, staticColorG, staticColorB),
                        new Color(waveColorR, waveColorG, waveColorB),
                        Duration.ofMillis(duration), speed, vertical, upRight, id);
            } else if (effectType == EffectList.CIRCLE_CHASE.ordinal()) {
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
                } else if (clockwise == null) {
                    throw new IOException("failed to get clockwise component for GeneratedEffect");
                } else if (id == null) {
                    throw new IOException("failed to get id component for GeneratedEffect");
                } else if (speed == null) {
                    throw new IOException("failed to get speed component for GeneratedEffect");
                }

                return new CircleChaseEffect(startTime, endTime,
                        new Color(startColorR, startColorG, startColorB),
                        new Color(endColorR, endColorG, endColorB),
                        Duration.ofMillis(duration), clockwise, angle, speed, id);
            } else if (effectType == EffectList.CHASE.ordinal()) {
                if (startTime == null) {
                    throw new IOException("failed to get startTime component for GeneratedEffect");
                } else if (endTime == null) {
                    throw new IOException("failed to get endTime component for GeneratedEffect");
                } else if (duration == null) {
                    throw new IOException("failed to get duration component for GeneratedEffect");
                } else if (clockwise == null) {
                    throw new IOException("failed to get clockwise component for GeneratedEffect");
                } else if (id == null) {
                    throw new IOException("failed to get id component for GeneratedEffect");
                } else if (speed == null) {
                    throw new IOException("failed to get speed component for GeneratedEffect");
                } else if (chaseSequence == null) {
                    throw new IOException("failed to get chase sequence component for GeneratedEffect");
                }
                return new ChaseEffect(startTime, endTime, chaseSequence, Duration.ofMillis(duration), clockwise, speed, id);
            }
        }  else {
            throw new IOException("failed to get effectType component for GeneratedEffect");
        }
        return null;
    }

    private boolean isChaseSequenceMember(String str) {
        if (str == null) {
            return false;
        }

        if (str.startsWith("colorR") || str.startsWith("colorG") || str.startsWith("colorB")) {
            return Integer.parseInt(str.substring(6)) >= 0;
        }
        return false;
    }
}
