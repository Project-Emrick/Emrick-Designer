package org.emrick.project;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.Duration;

public class ColorFadeGUI extends EffectGUI {
    public ColorFadeGUI(Effect effect, long startTime, EffectListener effectListener) {
        this.effect = effect;
        this.effectListener = effectListener;

        if (this.effect == null) {
            this.isNewEffect = true;

            // Set up dummy effect with default values for user to customize
            Color startColor = new Color(0, 0, 0);
            Color endColor = new Color(0, 0, 0);
            Duration delay = Duration.ofSeconds(0).plusMillis(0);
            Duration duration = Duration.ofSeconds(0).plusMillis(0);
            Duration timeout = Duration.ofSeconds(0).plusMillis(0);

            boolean TIME_GRADIENT = true;
            boolean SET_TIMEOUT = true;
            boolean DO_DELAY = true;
            boolean INSTANT_COLOR = true;

            this.effect = new Effect(startTime,
                    startColor, endColor, delay, duration, timeout,
                    TIME_GRADIENT, SET_TIMEOUT, DO_DELAY, INSTANT_COLOR);
        }

        this.effectMod = this.effect.clone(); // Changes made in GUI are not applied to original effect object
        setupGUI();
    }

    @Override
    protected void setupGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(20, 20));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(20, 20));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        // Text field customization
        delayField.getDocument().addDocumentListener(getDocumentListener());
        durationField.getDocument().addDocumentListener(getDocumentListener());
        timeoutField.getDocument().addDocumentListener(getDocumentListener());

        // Checkbox customization
        TIME_GRADIENTBox.setHorizontalTextPosition(SwingConstants.LEFT);
        TIME_GRADIENTBox.addItemListener(getCheckBoxItemListener());
        TIME_GRADIENTBox.setToolTipText("Enable/disable duration");
        INSTANT_COLORBox.setHorizontalTextPosition(SwingConstants.LEFT);
        INSTANT_COLORBox.addItemListener(getCheckBoxItemListener());
        INSTANT_COLORBox.setToolTipText("Tells the lights to change to the start color before the delay is executed if there is a delay.");

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        Border innerBorder = BorderFactory.createTitledBorder("Effect");
        Border outerBorder = BorderFactory.createEmptyBorder(5,5,5,5);

        this.effectPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        this.effectPanel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();

        Insets spacedInsets = new Insets(0,0,0,5);
        Insets noSpacedInsets = new Insets(0,0,0,0);

        //////////////// 0th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.2;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = spacedInsets;
        this.effectPanel.add(startTimeLabel, gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = spacedInsets;
        this.effectPanel.add(endTimeLabel, gc);

        //////////////// 1st Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0; // Horizontally, left to right
        gc.gridy = 1; // Vertically, top to bottom
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = spacedInsets;
        this.effectPanel.add(startColorLabel, gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = noSpacedInsets;
        this.effectPanel.add(startColorBtn, gc);

        //////////////// 2nd Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 2;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = spacedInsets;
        this.effectPanel.add(endColorLabel, gc);

        gc.gridx = 1;
        gc.gridy = 2;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = noSpacedInsets;
        this.effectPanel.add(endColorBtn, gc);

        //////////////// 4th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 3;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = spacedInsets;
        this.effectPanel.add(durationLabel, gc);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = noSpacedInsets;
        this.effectPanel.add(durationField, gc);


        //////////////// 6th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 4;
        gc.anchor = GridBagConstraints.LINE_END;
        this.effectPanel.add(TIME_GRADIENTBox, gc);


        //////////////// 9th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 5;
        gc.anchor = GridBagConstraints.LINE_END;
        this.effectPanel.add(INSTANT_COLORBox, gc);

        //////////////// Apply or Delete Buttons ////////////////

        gc.weightx = 1;
        gc.weighty = 2.0;

        gc.gridx = 0;
        gc.gridy = 6;
        gc.anchor = GridBagConstraints.FIRST_LINE_END;
        gc.insets = new Insets(0,0,0,5);
        this.effectPanel.add(deleteBtn, gc);

        gc.weightx = 1;
        gc.weighty = 2.0;

        gc.gridx = 1;
        gc.gridy = 6;
        gc.insets = new Insets(0,5,0,0);
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.effectPanel.add(applyBtn, gc);

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    @Override
    protected void loadEffectToGUI(Effect effect) {

        // Get start and end colors
        startColorBtn.setBackground(effect.getStartColor());
        endColorBtn.setBackground(effect.getEndColor());

        // Get delay, duration, and timeout
        String durationStr = String.valueOf(effect.getDuration().toNanos() / 1_000_000_000.0);
        durationField.setText(durationStr);

        TIME_GRADIENTBox.setSelected(effect.isTIME_GRADIENT());
        INSTANT_COLORBox.setSelected(effect.isINSTANT_COLOR());

        // Calculate start time label
        long minutesStart = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(effect.getStartTimeMSec());
        long secondsStart = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(effect.getStartTimeMSec()) % 60;
        long millisecondsStart = effect.getStartTimeMSec() % 1000;
        startTimeLabel.setText(String.format("Start: %d:%02d:%03d", minutesStart, secondsStart, millisecondsStart));

        long minutesEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(effect.getEndTimeMSec());
        long secondsEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(effect.getEndTimeMSec()) % 60;
        long millisecondsEnd = effect.getEndTimeMSec() % 1000;
        endTimeLabel.setText(String.format("End: %d:%02d:%03d", minutesEnd, secondsEnd, millisecondsEnd));

        if (this.isNewEffect) {
            applyBtn.setText("Create effect");
            deleteBtn.setEnabled(false);
        }
        else {
            applyBtn.setText("Update effect");
            deleteBtn.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(300, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        long startTimeMSec = Duration.ofMillis(500).toMillis();

        Color startColor = new Color(0, 255, 0);
        Color endColor = new Color(255, 0, 0);

        Duration duration = Duration.ofSeconds(2).plusMillis(205);

        boolean TIME_GRADIENT = true;
        boolean SET_TIMEOUT = true;
        boolean DO_DELAY = true;
        boolean INSTANT_COLOR = true;

        ColorFade cf = new ColorFade(startTimeMSec, startTimeMSec+duration.toMillis(), startColor, endColor);

        EffectListener el = new EffectListener() {
            @Override
            public void onCreateEffect(Effect effect) {
                System.out.println("onCreateEffect()");
            }

            @Override
            public void onUpdateEffect(Effect oldEffect, Effect newEffect) {
                System.out.println("onUpdateEffect");
            }

            @Override
            public void onDeleteEffect(Effect effect) {
                System.out.println("onDeleteEffect");
            }
        };

        ColorFadeGUI effectGUI = new ColorFadeGUI(cf, startTimeMSec, el);
        //effectGUI = new EffectGUI(EffectGUI.noCommonEffectMsg);
        frame.add(effectGUI.getEffectPanel());

        frame.setVisible(true);
    }
}
