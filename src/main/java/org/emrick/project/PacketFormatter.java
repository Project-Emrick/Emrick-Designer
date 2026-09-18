package org.emrick.project;

import org.emrick.project.effect.Checkpoint;
import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectList;
import org.emrick.project.effect.LightingDisplay;

import java.awt.*;

/**
 * Builds standalone packet lines (matching firmware Packet::fromString format) for previewing
 * a single Effect on hardware, independent of any show timeline, RF triggers, or other effects.
 */
public class PacketFormatter {

    // Must match Heltec Receiver/src/Packet.h pattern flag bits
    private static final int DEFAULT_FUNCTION_FLAG = 0x1;
    private static final int USE_DURATION_FLAG = 0x2;
    private static final int SET_TIMEOUT_FLAG = 0x4;
    private static final int DO_DELAY_FLAG = 0x8;
    private static final int INSTANT_COLOR_FLAG = 0x10;

    public static String toPreviewPacketString(Effect e, int stripId) {
        // SET_TIMEOUT is always forced so the board cleanly returns to idle instead of
        // reading past this one packet into unrelated storage (see runLED in main.cpp).
        int flags = SET_TIMEOUT_FLAG;
        if (e.isUSE_DURATION()) flags += USE_DURATION_FLAG;
        if (e.getEffectType() == EffectList.STATIC_COLOR) flags += INSTANT_COLOR_FLAG;
        if (e.getFunction() == LightingDisplay.Function.DEFAULT) flags += DEFAULT_FUNCTION_FLAG;
        if (e.isDO_DELAY()) flags += DO_DELAY_FLAG;

        int size;
        if (e.getEffectType() == EffectList.NOISE) {
            size = e.getNoiseCheckpoints().size() * 5 + 1;
            if (e.isFade()) size--;
        } else {
            size = e.getSize();
        }

        StringBuilder out = new StringBuilder();
        out.append("Size: ").append(size);
        out.append(", Strip_id: ").append(stripId);
        out.append(", Set_id: 0");
        out.append(", Flags: ").append(flags);
        Color startColor = e.getStartColor();
        out.append(", Start_color: ").append(startColor.getRed()).append(",").append(startColor.getGreen()).append(",").append(startColor.getBlue());
        Color endColor = e.getEndColor();
        out.append(", End_color: ").append(endColor.getRed()).append(",").append(endColor.getGreen()).append(",").append(endColor.getBlue());
        out.append(", Delay: ").append(e.isDO_DELAY() ? e.getDelay().toMillis() : 0);
        out.append(", Duration: ").append(e.getDuration().toMillis());
        out.append(", Function: ").append(e.getFunction().ordinal());
        // Give a visible hold at the end color so the preview doesn't blink off instantly.
        long timeoutMs = e.getTimeout().toMillis();
        out.append(", Timeout: ").append(timeoutMs > 0 ? timeoutMs : 1000);

        if (e.getFunction() == LightingDisplay.Function.ALTERNATING_COLOR) {
            out.append(", ExtraParameters: ").append(e.getSpeed());
        } else if (e.getFunction() == LightingDisplay.Function.CHASE) {
            out.append(", ExtraParameters: ").append(e.getChaseSequence().size()).append(",").append(e.getSpeed());
            for (Color c : e.getChaseSequence()) {
                out.append(",").append(c.getRed()).append(",").append(c.getGreen()).append(",").append(c.getBlue());
            }
        } else if (e.getFunction() == LightingDisplay.Function.NOISE) {
            out.append(", ExtraParameters: ").append(e.isFade() ? 1 : 0);
            for (Checkpoint c : e.getNoiseCheckpoints()) {
                if (c.time() != 0) out.append(",").append(c.time());
                out.append(",").append(c.color().getRed()).append(",").append(c.color().getGreen()).append(",").append(c.color().getBlue());
                out.append(",").append(c.brightness());
            }
        }
        return out.toString();
    }

    // How long a "hold this color" packet is displayed before the board's watchdog auto-clears
    // it; comfortably long for manual verification, well under the firmware's int-ms overflow limit.
    private static final long HOLD_COLOR_TIMEOUT_MS = 20L * 60L * 1000L;

    /** Builds a packet that shows a solid color indefinitely (until replaced or preview mode exits). */
    public static String toHoldColorPacketString(Color color, int stripId) {
        int flags = INSTANT_COLOR_FLAG | DEFAULT_FUNCTION_FLAG | SET_TIMEOUT_FLAG;
        StringBuilder out = new StringBuilder();
        out.append("Size: 0");
        out.append(", Strip_id: ").append(stripId);
        out.append(", Set_id: 0");
        out.append(", Flags: ").append(flags);
        out.append(", Start_color: ").append(color.getRed()).append(",").append(color.getGreen()).append(",").append(color.getBlue());
        out.append(", End_color: ").append(color.getRed()).append(",").append(color.getGreen()).append(",").append(color.getBlue());
        out.append(", Delay: 0");
        out.append(", Duration: 0");
        out.append(", Function: ").append(LightingDisplay.Function.DEFAULT.ordinal());
        out.append(", Timeout: ").append(HOLD_COLOR_TIMEOUT_MS);
        return out.toString();
    }
}
