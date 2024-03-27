package org.emrick.project;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;

public class EffectGUI implements ActionListener {

    JPanel effectPanel;
    Effect effect;
    Effect effectCopy;

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
    JCheckBox TIME_GRADIENTBox = new JCheckBox("TIME_GRADIENT", true);
    JCheckBox SET_TIMEOUTBox = new JCheckBox("SET_TIMEOUT", true);
    JCheckBox DO_DELAYBox = new JCheckBox("DO_DELAY", true);
    JCheckBox INSTANT_COLORBox = new JCheckBox("INSTANT_COLOR", true);

    JButton applyBtn = new JButton("REPLACE THIS TEXT WITH UPDATE OR CREATE EFFECT TEXT");
    JButton deleteBtn = new JButton("Delete effect");

    /**
     * @param effect The current effect, if it exists. Null if it doesn't exist.
     * @param startTime In the case that no effect exists for the performer at the given time, we still need the current
     *                 time for gui display.
     */
    public EffectGUI(Effect effect, long startTime) {
        this.effect = effect;

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

        this.effectCopy = this.effect.clone(); // Changes made in GUI are not applied to original effect object
        setupGUI();
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
        SET_TIMEOUTBox.setHorizontalTextPosition(SwingConstants.LEFT);
        DO_DELAYBox.setHorizontalTextPosition(SwingConstants.LEFT);
        INSTANT_COLORBox.setHorizontalTextPosition(SwingConstants.LEFT);

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
        loadEffectToGUI(this.effectCopy);
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

        TIME_GRADIENTBox = new JCheckBox("TIME_GRADIENT", effect.isTIME_GRADIENT());
        SET_TIMEOUTBox = new JCheckBox("SET_TIMEOUT", effect.isSET_TIMEOUT());
        DO_DELAYBox = new JCheckBox("DO_DELAY", effect.isDO_DELAY());
        INSTANT_COLORBox = new JCheckBox("INSTANT_COLOR", effect.isINSTANT_COLOR());

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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(startColorBtn)) {
            Color selectedColor = JColorChooser.showDialog(this.effectPanel, "Choose Start Color", this.effectCopy.getStartColor());
            if (selectedColor != null) {
                this.effectCopy.setStartColor(selectedColor);
                this.startColorBtn.setBackground(selectedColor);
            }
        }
        else if (e.getSource().equals(endColorBtn)) {
            Color selectedColor = JColorChooser.showDialog(this.effectPanel, "Choose End Color", this.effectCopy.getEndColor());
            if (selectedColor != null) {
                this.effectCopy.setEndColor(selectedColor);
                this.endColorBtn.setBackground(selectedColor);
            }
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

                // Calculate the new end time, live gui update
                long newEndTime = effectCopy.getStartTimeMSec();

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

                if (DO_DELAYBox.isSelected()) newEndTime += delayMSec;
                if (TIME_GRADIENTBox.isSelected()) newEndTime += durationMSec;
                if (SET_TIMEOUTBox.isSelected()) newEndTime += timeoutMSec;

                long minutesEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(newEndTime);
                long secondsEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(newEndTime) % 60;
                long millisecondsEnd = newEndTime % 1000;
                endTimeLabel.setText(String.format("End: %d:%02d:%03d", minutesEnd, secondsEnd, millisecondsEnd));
            }
        };
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

        EffectGUI effectGUI = new EffectGUI(effect, startTimeMSec);
        frame.add(effectGUI.getEffectPanel());

        frame.setVisible(true);
    }

}
