package org.emrick.project;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.Duration;

public class EffectGUI implements ActionListener {

    JPanel effectPanel;
    Effect effect;
    Effect effectMod;
    EffectListener effectListener;

    // Status
    boolean isNewEffect;

    // Pattern Parameter Components
    JLabel startTimeLabel = new JLabel("Start: ");
    JLabel endTimeLabel = new JLabel("End: ");
    JLabel startColorLabel = new JLabel("Start color: ");
    JLabel endColorLabel = new JLabel("End color: ");
    JLabel delayLabel = new JLabel("Delay (s): ");
    JLabel durationLabel = new JLabel("Duration (s): ");
    JLabel timeoutLabel = new JLabel("Timeout (s): ");

    JButton startColorBtn = new JButton();
    JButton endColorBtn = new JButton();
    JTextField delayField = new JTextField(10);
    JTextField durationField = new JTextField(10);
    JTextField timeoutField = new JTextField(10);
    JCheckBox TIME_GRADIENTBox = new JCheckBox("TIME_GRADIENT");
    JCheckBox SET_TIMEOUTBox = new JCheckBox("SET_TIMEOUT");
    JCheckBox DO_DELAYBox = new JCheckBox("DO_DELAY");
    JCheckBox INSTANT_COLORBox = new JCheckBox("INSTANT_COLOR");

    JButton applyBtn = new JButton("REPLACE THIS TEXT WITH UPDATE OR CREATE EFFECT TEXT");
    JButton deleteBtn = new JButton("Delete effect");

    // Strings
    public static String noProjectSyncMsg = "<html><body style='text-align: center;'>Load a time-synced Emrick project to get started using effects.</body></html>";
    public static String noPerformerMsg = "<html><body style='text-align: center;'>Select one or more performers to modify their effects.</body></html>";
    public static String noCommonEffectMsg = "<html><body style='text-align: center;'>No common effect found among selected performers.</body></html>";

    /**
     * @param effect The current effect, as it exists. Passed in null if it doesn't exist.
     * @param startTime In the case that no effect exists for the performer at the given time, we still need the current
     *                 time for gui display.
     */
    public EffectGUI(Effect effect, long startTime, EffectListener effectListener) {
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

    /**
     * This alternative constructor can deliver a placeholder panel with instructions about working with effects. You
     * can call this when the program is first started and the project isn't loaded or synced.
     */
    public EffectGUI(String placeholderText) {
        this.effectPanel = new JPanel();

        Border innerBorder = BorderFactory.createTitledBorder("Effect");
        Border outerBorder = BorderFactory.createEmptyBorder(5,5,5,5);
        Border innerBorder2 = BorderFactory.createEmptyBorder(20,20,20,20);
        Border outerBorder2 = BorderFactory.createCompoundBorder(outerBorder, innerBorder);

        this.effectPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder2, innerBorder2));

        this.effectPanel.setLayout(new GridBagLayout());

        JLabel placeholderLabel = new JLabel(placeholderText);
        placeholderLabel.setHorizontalAlignment(JLabel.CENTER);

        GridBagConstraints gc = new GridBagConstraints();

        //////////////// 0th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 1;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;
        this.effectPanel.add(placeholderLabel, gc);
    }

    private void setupGUI() {
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
        SET_TIMEOUTBox.setHorizontalTextPosition(SwingConstants.LEFT);
        SET_TIMEOUTBox.addItemListener(getCheckBoxItemListener());
        SET_TIMEOUTBox.setToolTipText("Enable/disable timeout");
        DO_DELAYBox.setHorizontalTextPosition(SwingConstants.LEFT);
        DO_DELAYBox.addItemListener(getCheckBoxItemListener());
        DO_DELAYBox.setToolTipText("Enable/disable delay");
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

        //////////////// 3rd Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 3;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = spacedInsets;
        this.effectPanel.add(delayLabel, gc);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = noSpacedInsets;
        this.effectPanel.add(delayField, gc);

        //////////////// 4th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 4;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = spacedInsets;
        this.effectPanel.add(durationLabel, gc);

        gc.gridx = 1;
        gc.gridy = 4;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = noSpacedInsets;
        this.effectPanel.add(durationField, gc);

        //////////////// 5th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 5;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = spacedInsets;
        this.effectPanel.add(timeoutLabel, gc);

        gc.gridx = 1;
        gc.gridy = 5;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = noSpacedInsets;
        this.effectPanel.add(timeoutField, gc);

        //////////////// 6th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 6;
        gc.anchor = GridBagConstraints.LINE_END;
        this.effectPanel.add(TIME_GRADIENTBox, gc);

        //////////////// 7th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 7;
        gc.anchor = GridBagConstraints.LINE_END;
        this.effectPanel.add(SET_TIMEOUTBox, gc);

        //////////////// 8th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 8;
        gc.anchor = GridBagConstraints.LINE_END;
        this.effectPanel.add(DO_DELAYBox, gc);

        //////////////// 9th Row ////////////////

        gc.weightx = 1;
        gc.weighty = 0.1;

        gc.gridx = 0;
        gc.gridy = 9;
        gc.anchor = GridBagConstraints.LINE_END;
        this.effectPanel.add(INSTANT_COLORBox, gc);

        //////////////// Apply or Delete Buttons ////////////////

        gc.weightx = 1;
        gc.weighty = 2.0;

        gc.gridx = 0;
        gc.gridy = 10;
        gc.anchor = GridBagConstraints.FIRST_LINE_END;
        gc.insets = new Insets(0,0,0,5);
        this.effectPanel.add(deleteBtn, gc);

        gc.weightx = 1;
        gc.weighty = 2.0;

        gc.gridx = 1;
        gc.gridy = 10;
        gc.insets = new Insets(0,5,0,0);
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.effectPanel.add(applyBtn, gc);

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void loadEffectToGUI(Effect effect) {

        // Get start and end colors
        startColorBtn.setBackground(effect.getStartColor());
        endColorBtn.setBackground(effect.getEndColor());

        // Get delay, duration, and timeout
        String delayStr = String.valueOf(effect.getDelay().toNanos() / 1_000_000_000.0);
        String durationStr = String.valueOf(effect.getDuration().toNanos() / 1_000_000_000.0);
        String timeoutStr = String.valueOf(effect.getTimeout().toNanos() / 1_000_000_000.0);
        delayField.setText(delayStr);
        durationField.setText(durationStr);
        timeoutField.setText(timeoutStr);

        TIME_GRADIENTBox.setSelected(effect.isTIME_GRADIENT());
        SET_TIMEOUTBox.setSelected(effect.isSET_TIMEOUT());
        DO_DELAYBox.setSelected(effect.isDO_DELAY());
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

    public JPanel getEffectPanel() {
        return effectPanel;
    }

    private void applyToEffectMod() {

        // Start and end color are applied automatically to effectCopy
        Duration delay = Duration.ofNanos((long) (Double.parseDouble(delayField.getText()) * 1_000_000_000L));
        Duration duration = Duration.ofNanos((long) (Double.parseDouble(durationField.getText()) * 1_000_000_000L));
        Duration timeout = Duration.ofNanos((long) (Double.parseDouble(timeoutField.getText()) * 1_000_000_000L));
        this.effectMod.setDelay(delay);
        this.effectMod.setDuration(duration);
        this.effectMod.setTimeout(timeout);

        this.effectMod.setTIME_GRADIENT(this.TIME_GRADIENTBox.isSelected());
        this.effectMod.setSET_TIMEOUT(this.SET_TIMEOUTBox.isSelected());
        this.effectMod.setDO_DELAY(this.DO_DELAYBox.isSelected());
        this.effectMod.setINSTANT_COLOR(this.INSTANT_COLORBox.isSelected());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(this.startColorBtn)) {
            Color selectedColor = JColorChooser.showDialog(this.effectPanel, "Choose Start Color", this.effectMod.getStartColor());
            if (selectedColor != null) {
                this.effectMod.setStartColor(selectedColor);
                this.startColorBtn.setBackground(selectedColor);
            }
        }
        else if (e.getSource().equals(this.endColorBtn)) {
            Color selectedColor = JColorChooser.showDialog(this.effectPanel, "Choose End Color", this.effectMod.getEndColor());
            if (selectedColor != null) {
                this.effectMod.setEndColor(selectedColor);
                this.endColorBtn.setBackground(selectedColor);
            }
        }
        else if (e.getSource().equals(this.applyBtn)) {
            applyToEffectMod();
            if (this.isNewEffect)
                effectListener.onCreateEffect(this.effectMod);
            else
                effectListener.onUpdateEffect(this.effect, this.effectMod);
        }
        else if (e.getSource().equals(this.deleteBtn)) {
            effectListener.onDeleteEffect(this.effect); // Delete target is the original
        }
    }

    private DocumentListener getDocumentListener() {
        return new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                documentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                documentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {}

            private void documentChanged() {
                liveUpdateEndTime();
            }
        };
    }

    private ItemListener getCheckBoxItemListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                liveUpdateEndTime();
            }
        };
    }

    private void liveUpdateEndTime() {

        // Calculate the new end time, live gui update
        long newEndTime = effectMod.getStartTimeMSec();

        long delayMSec;
        long durationMSec;
        long timeoutMSec;
        try {
            delayMSec = (long) (Float.parseFloat(delayField.getText()) * 1000);
            durationMSec = (long) (Float.parseFloat(durationField.getText()) * 1000);
            timeoutMSec = (long) (Float.parseFloat(timeoutField.getText()) * 1000);
        } catch (NumberFormatException nfe) {
            // System.out.println("Live End Time Calculation: Number Format Exception.");
            return;
        }

        if (DO_DELAYBox.isSelected()) {
            newEndTime += delayMSec;
        }
        if (TIME_GRADIENTBox.isSelected()) {
            newEndTime += durationMSec;
        }
        if (SET_TIMEOUTBox.isSelected()) {
            newEndTime += timeoutMSec;
        }

        long minutesEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(newEndTime);
        long secondsEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(newEndTime) % 60;
        long millisecondsEnd = newEndTime % 1000;
        endTimeLabel.setText(String.format("End: %d:%02d:%03d", minutesEnd, secondsEnd, millisecondsEnd));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(300, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        long startTimeMSec = Duration.ofMillis(500).toMillis();

        Color startColor = new Color(0, 255, 0);
        Color endColor = new Color(255, 0, 0);

        Duration delay = Duration.ofSeconds(1).plusMillis(105);
        Duration duration = Duration.ofSeconds(2).plusMillis(205);
        Duration timeout = Duration.ofSeconds(3).plusMillis(305);

        boolean TIME_GRADIENT = true;
        boolean SET_TIMEOUT = true;
        boolean DO_DELAY = true;
        boolean INSTANT_COLOR = true;

        Effect effect = new Effect(startTimeMSec,
                startColor, endColor, delay, duration, timeout,
                TIME_GRADIENT, SET_TIMEOUT, DO_DELAY, INSTANT_COLOR);

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

        EffectGUI effectGUI = new EffectGUI(null, startTimeMSec, el);
        effectGUI = new EffectGUI(EffectGUI.noCommonEffectMsg);
        frame.add(effectGUI.getEffectPanel());

        frame.setVisible(true);
    }

}
