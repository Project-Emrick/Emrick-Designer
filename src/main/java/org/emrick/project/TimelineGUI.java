package org.emrick.project;

import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectListener;
import org.emrick.project.effect.RFTrigger;
import org.emrick.project.effect.RFTriggerListener;

import javax.swing.*;
import java.util.ArrayList;

public class TimelineGUI {

    private ArrayList<Effect> effects;
    private ArrayList<RFTrigger> triggers;
    private EffectListener effectListener;
    private RFTriggerListener rfTriggerListener;

    public TimelineGUI(ArrayList<Effect> effects, ArrayList<RFTrigger> triggers,
                       EffectListener effectListener, RFTriggerListener rfTriggerListener) {
        this.effects = effects;
        this.triggers = triggers;
        this.effectListener = effectListener;
        this.rfTriggerListener = rfTriggerListener;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(600, 120);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
    }

}
