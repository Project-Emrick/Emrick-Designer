package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.TimeManager;

import java.awt.Color;
import java.util.HashSet;

/**
 * Listen to important events pertaining to effects. For example, when an effect is created or updated.
 */
public interface EffectListener {
    void onCreateEffect(Effect effect);
    void onUpdateEffect(Effect oldEffect, Effect newEffect);
    void onDeleteEffect(Effect effect);
    void onUpdateEffectPanel(Effect effect, boolean isNew, int index);
    void onChangeSelectionMode(boolean isInnerSelect, HashSet<LEDStrip> strips);
    HashSet<LEDStrip> onInnerSelectionRequired();
    HashSet<LEDStrip> onSelectionRequired();
    TimeManager onTimeRequired();

    void onPressEffect(Effect effect);

    /** Sends the given effect to a connected Receiver board for a live, on-hardware preview. */
    void onPreviewOnHardware(Effect effect);

    /** Returns a connected Receiver's LEDs to idle and exits its preview mode, if it was previewing. */
    void onStopHardwarePreview();

    /** Holds a solid color on a connected Receiver indefinitely, for quick verification while picking colors. */
    void onPreviewColorOnHardware(Color color);
}
