package org.emrick.project;

/**
 * Listen to important events pertaining to effects. For example, when an effect is created or updated.
 */
public interface EffectListener {
    void onCreateEffect();
    void onUpdateEffect();
    void onDeleteEffect();
}
