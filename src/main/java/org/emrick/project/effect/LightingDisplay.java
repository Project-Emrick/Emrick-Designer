package org.emrick.project.effect;

import org.emrick.project.LEDStrip;

import java.awt.*;
import java.util.ArrayList;

public class LightingDisplay {

    public enum Function {
        DEFAULT,
        ALTERNATING_COLOR,
        CHASE,
        NOISE
    }

    public static Color defaultLEDFunction(Effect e, long setMS, long currMS) {
        if (e.isDO_DELAY()) {
            if (e.getStartTimeMSec() + e.getDelay().toMillis() > currMS) {
                if (e.isINSTANT_COLOR()) {
                    return e.getStartColor();
                } else {
                    return Color.black;
                }
            }
        }
        if (e.isUSE_DURATION()) {
            if (e.getStartTimeMSec() + e.getDelay().toMillis() + e.getDuration().toMillis() >= currMS) {
                long startGradient = e.getStartTimeMSec() + e.getDelay().toMillis();
                float shiftProgress = (float) (currMS - startGradient) / (float) e.getDuration().toMillis();
                float[] hsvs = new float[3];
                Color.RGBtoHSB(e.getStartColor().getRed(), e.getStartColor().getGreen(), e.getStartColor().getBlue(), hsvs);
                hsvs[0] *= 360;
                float startHue = hsvs[0];
                float[] hsve = new float[3];
                Color.RGBtoHSB(e.getEndColor().getRed(), e.getEndColor().getGreen(), e.getEndColor().getBlue(), hsve);
                hsve[0] *= 360;
                float endHue = hsve[0];
                if (e.getStartColor().equals(Color.black)) {
                    hsvs[0] = hsve[0];
                    startHue = hsvs[0];
                    hsvs[1] = hsve[1];
                }
                if (e.getEndColor().equals(Color.black)) {
                    hsve[0] = hsvs[0];
                    endHue = hsve[0];
                    hsve[1] = hsvs[1];
                }
                float h, s, v;
                if (startHue != endHue) {
                    boolean clockwise = true;
                    if (endHue > startHue) {
                        if (endHue - startHue > 180) {
                            clockwise = false;
                        }
                    } else {
                        if (startHue - endHue < 180) {
                            clockwise = false;
                        }
                    }
                    // the math to make this work sucks and probably has redundancies but it works and I refuse to touch it
                    if (clockwise) {
                        if (hsve[0] >= hsvs[0]) {
                            h = ((hsve[0] - hsvs[0]) * shiftProgress + hsvs[0]) % 360;
                        } else {
                            h = ((hsve[0] + 360 - hsvs[0]) * shiftProgress + hsvs[0]) % 360;
                        }
                    } else {
                        if (hsve[0] >= hsvs[0]) {
                            h = ((hsvs[0] + 360 - (hsvs[0] - (hsve[0] - 360)) * shiftProgress)) % 360;
                        } else {
                            h = (hsvs[0] - (hsvs[0] - hsve[0]) * shiftProgress) % 360;
                        }
                    }
                } else {
                    h = hsvs[0];
                }
                s = (hsve[1] - hsvs[1]) * shiftProgress + hsvs[1];
                v = (hsve[2] - hsvs[2]) * shiftProgress + hsvs[2];
                h /= 360;
                return new Color(Color.HSBtoRGB(h,s,v));
            }
        }
        if (e.isSET_TIMEOUT()) {
            if (e.getStartTimeMSec() + e.getDelay().toMillis() + e.getDuration().toMillis() + e.getTimeout().toMillis() > currMS) {
                return e.getEndColor();
            }
        }
        return Color.black;
    }

    public static Color alternatingColorFunction(Effect e, long setMS, long currMS) {

        if (e.getStartTimeMSec() + e.getDuration().toMillis() >= currMS) {
            double percent = (currMS - e.getStartTimeMSec()) / (1.0 / (2.0 * e.getSpeed()) * 1000.0);
            int odd = (int)percent % 2;
            if (odd == 0) {
                return e.getStartColor();
            } else {
                return e.getEndColor();
            }
        }



        if (e.isSET_TIMEOUT()) {
            if (e.getStartTimeMSec() + e.getDelay().toMillis() + e.getDuration().toMillis() + e.getTimeout().toMillis() > currMS) {
                return e.getEndColor();
            }
        }
        return Color.black;
    }

    public static ArrayList<Color> chaseFunction(Effect e, LEDStrip l, long setMS, long currMS) {
        ArrayList<Color> colors = new ArrayList<>();
        if (e.getStartTimeMSec() + e.getDuration().toMillis() >= currMS) {
            double percent = (currMS - e.getStartTimeMSec()) / (1.0 / e.getSpeed() * 1000.0);
            int tick = (int)percent % e.getChaseSequence().size();
            for (int i = 0; i < l.getLedConfig().getLEDCount(); i++) {
                // TODO: test to see if this logic needs reversed
                if (e.isDirection()) {
                    int positive = tick - i;
                    while (positive < 0) {
                        positive += e.getChaseSequence().size();
                    }
                    colors.add(e.getChaseSequence().get(positive % e.getChaseSequence().size()));
                } else {
                    colors.add(e.getChaseSequence().get((tick+i) % e.getChaseSequence().size()));
                }
            }
        }

        if (e.isSET_TIMEOUT()) {
            if (e.getStartTimeMSec() + e.getDelay().toMillis() + e.getDuration().toMillis() + e.getTimeout().toMillis() > currMS) {
                double percent = (e.getEndTimeMSec() - e.getStartTimeMSec()) / (1.0 / e.getSpeed() * 1000.0);
                int tick = (int)percent % e.getChaseSequence().size();
                for (int i = 0; i < l.getLedConfig().getLEDCount(); i++) {
                    if (e.isDirection()) {
                        colors.add(e.getChaseSequence().get((tick+i) % e.getChaseSequence().size()));
                    } else {
                        int positive = tick - i;
                        while (positive < 0) {
                            positive += e.getChaseSequence().size();
                        }
                        colors.add(e.getChaseSequence().get((tick - i) % e.getChaseSequence().size()));
                    }
                }
            } else {
                for (int i = 0; i < l.getLedConfig().getLEDCount(); i++) {
                    colors.add(Color.black);
                }
            }
        }
        return colors;
    }

    public static Color randomNoiseFunction(Effect e, long setMS, long currMS) {
        if (e.getStartTimeMSec() + e.getDuration().toMillis() >= currMS) {
            ArrayList<Checkpoint> checkpoints = e.getNoiseCheckpoints();
            long start = e.getStartTimeMSec();
            int i = 0;
            long checkPointStart = start;
            while (checkPointStart + checkpoints.get(i).time() <= currMS) {
                checkPointStart += checkpoints.get(i).time();
                i++;
            }
            Checkpoint curr = checkpoints.get(i);
            if (e.isFade()) {
                Checkpoint next = checkpoints.get(i + 1);
                long startGradient = checkPointStart;
                float shiftProgress = (float) (currMS - startGradient) / (float) curr.time();
                float[] hsvs = new float[3];
                Color.RGBtoHSB(curr.color().getRed(), curr.color().getGreen(), curr.color().getBlue(), hsvs);
                hsvs[0] *= 360;
                float startHue = hsvs[0];
                float[] hsve = new float[3];
                Color.RGBtoHSB(next.color().getRed(), next.color().getGreen(), next.color().getBlue(), hsve);
                hsve[0] *= 360;
                float endHue = hsve[0];
                if (curr.color().equals(Color.black)) {
                    hsvs[0] = hsve[0];
                    startHue = hsvs[0];
                    hsvs[1] = hsve[1];
                }
                if (next.color().equals(Color.black)) {
                    hsve[0] = hsvs[0];
                    endHue = hsve[0];
                    hsve[1] = hsvs[1];
                }
                float h, s, v;
                if (startHue != endHue) {
                    boolean clockwise = true;
                    if (endHue > startHue) {
                        if (endHue - startHue > 180) {
                            clockwise = false;
                        }
                    } else {
                        if (startHue - endHue < 180) {
                            clockwise = false;
                        }
                    }
                    // the math to make this work sucks and probably has redundancies but it works and I refuse to touch it
                    if (clockwise) {
                        if (hsve[0] >= hsvs[0]) {
                            h = ((hsve[0] - hsvs[0]) * shiftProgress + hsvs[0]) % 360;
                        } else {
                            h = ((hsve[0] + 360 - hsvs[0]) * shiftProgress + hsvs[0]) % 360;
                        }
                    } else {
                        if (hsve[0] >= hsvs[0]) {
                            h = ((hsvs[0] + 360 - (hsvs[0] - (hsve[0] - 360)) * shiftProgress)) % 360;
                        } else {
                            h = (hsvs[0] - (hsvs[0] - hsve[0]) * shiftProgress) % 360;
                        }
                    }
                } else {
                    h = hsvs[0];
                }
                s = (hsve[1] - hsvs[1]) * shiftProgress + hsvs[1];
                v = (hsve[2] - hsvs[2]) * shiftProgress + hsvs[2];
                h /= 360;
                Color pbs = new Color(Color.HSBtoRGB(h,s,v)); // pre-brightness scaling
                float b = curr.brightness() + (next.brightness() - curr.brightness()) * shiftProgress;
                return new Color((int)(pbs.getRed() * b), (int)(pbs.getGreen() * b), (int)(pbs.getBlue() * b));
            } else {
                Color pbs = curr.color(); // pre-brightness scaling
                float b = curr.brightness();
                try {
                    return new Color((int)(pbs.getRed() * b), (int)(pbs.getGreen() * b), (int)(pbs.getBlue() * b));
                } catch (IllegalArgumentException ex) {
                    System.out.println(pbs.getRed() * b + ", " + pbs.getGreen() * b + ", " + pbs.getBlue() * b);
                }
            }
        }

        if (e.isSET_TIMEOUT()) {
            if (e.getStartTimeMSec() + e.getDelay().toMillis() + e.getDuration().toMillis() + e.getTimeout().toMillis() > currMS) {
                return e.getNoiseCheckpoints().get(e.getNoiseCheckpoints().size() - 1).color();
            }
        }
        return Color.black;
    }
}
