package org.emrick.project.effect;

import java.awt.*;

public class LightingDisplay {

    public enum Function {
        DEFAULT,
        ALTERNATING_COLOR
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
}
