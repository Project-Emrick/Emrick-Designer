package org.emrick.project.effect;

import com.google.gson.*;
import com.google.gson.reflect.*;
import org.emrick.project.EffectsGroup;
import org.emrick.project.PathConverter;
import org.emrick.project.Performer;
import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.stream.*;

public class EffectGUI implements ActionListener {
    private static List<EffectsGroup> EFFECTS_GROUPS;
    // Strings
    public static String
            noProjectSyncMsg
            = "<html><body style='text-align: center;'>Load a time-synced Emrick project to get started using " +
              "effects" + ".</body></html>";
    public static String
            noPerformerMsg
            = "<html><body style='text-align: center;'>Select one or more performers to modify their effects" +
              ".</body></html>";
    public static String
            noCommonEffectMsg
            = "<html><body style='text-align: center;'>No common effect found among selected performers.</body></html>";

    public static String
            noEffectGroupMsg
            = "<html><body style='text-align: center;'>One or more performers are missing effects to use effect groups.</body></html>";


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
    JLabel staticColorLabel = new JLabel("Static color: ");
    JLabel waveColorLabel = new JLabel("Wave Color: ");
    JLabel color1Label = new JLabel("Color 1: ");
    JLabel color2Label = new JLabel("Color 2: ");
    JLabel rateLabel = new JLabel("Rate (Hz): ");
    JCheckBox upOrSideBox = new JCheckBox("Vertical");
    JCheckBox clockwiseBox = new JCheckBox("Clockwise");
    JCheckBox directionBox = new JCheckBox("Up/Right");
    JCheckBox dirBox = new JCheckBox("Clockwise");
    JLabel speedLabel = new JLabel("Speed: ");
    JLabel angleLabel = new JLabel("Start Angle (deg): ");
    JButton startColorBtn = new JButton();
    JButton endColorBtn = new JButton();
    JTextField delayField = new JTextField(10);
    JTextField durationField = new JTextField(10);
    JTextField timeoutField = new JTextField(10);
    JTextField speedField = new JTextField(10);
    JTextField angleField = new JTextField(10);
    JCheckBox TIME_GRADIENTBox = new JCheckBox("TIME_GRADIENT");
    JCheckBox SET_TIMEOUTBox = new JCheckBox("SET_TIMEOUT");
    JCheckBox DO_DELAYBox = new JCheckBox("DO_DELAY");
    JCheckBox INSTANT_COLORBox = new JCheckBox("INSTANT_COLOR");
    JButton applyBtn = new JButton("REPLACE THIS TEXT WITH UPDATE OR CREATE EFFECT TEXT");
    JButton deleteBtn = new JButton("Delete effect");
    JLabel batteryEstLabel = new JLabel("Estimated Battery Usage: ");
    ArrayList<JButton> colorButtons = new ArrayList<>();
    String[] durationTypeOptions = {"Seconds", "Counts"};
    JComboBox<String> durationTypeSelect = new JComboBox<String>(durationTypeOptions);
    String durationType = "Seconds";
    ArrayList<JComponent[]> panelComponents = new ArrayList<>();

    private JLabel placeholderLabel;
    private EffectList effectType;

    /**
     * @param effect    The current effect, as it exists. Passed in null if it doesn't exist.
     * @param startTime In the case that no effect exists for the performer at the given time, we still need the current
     *                  time for gui display.
     */
    public EffectGUI(Effect effect, long startTime, EffectListener effectListener, EffectList effectType, boolean isNew) {
        this.effect = effect;
        this.effectListener = effectListener;
        this.effectType = effectType;
        this.isNewEffect = true;

        if (this.effect.equals(new Effect(startTime))) {

            // Set up dummy effect with default values for user to customize
            Color startColor = new Color(0, 0, 0);
            Color endColor = new Color(0, 0, 0);
            Duration delay = Duration.ofSeconds(0).plusMillis(0);
            Duration duration = Duration.ofSeconds(0).plusMillis(0);
            Duration timeout = Duration.ofSeconds(0).plusMillis(0);

            boolean TIME_GRADIENT = true;
            boolean SET_TIMEOUT = true;
            boolean DO_DELAY = false;
            boolean INSTANT_COLOR = true;

            this.effect = new Effect(startTime,
                                     startColor,
                                     endColor,
                                     delay,
                                     duration,
                                     timeout,
                                     TIME_GRADIENT,
                                     SET_TIMEOUT,
                                     DO_DELAY,
                                     INSTANT_COLOR,
                                  0);
            this.effect.setEffectType(effectType);
        } else {
            if (!isNew) {
                this.isNewEffect = false;
            }
        }
        placeholderLabel = null;

        this.effectMod = this.effect.clone(); // Changes made in GUI are not applied to original effect object

        switch (effectType) {
            case GENERATED_FADE -> setupFadeGUI();
            case STATIC_COLOR -> setupStaticColorGUI();
            case WAVE -> setupWaveGUI();
            case ALTERNATING_COLOR -> setupAlternatingColorGUI();
            case RIPPLE -> setupRippleGUI();
            case CIRCLE_CHASE -> setupCircleChaseGUI();
            case CHASE -> setupChaseGUI();
        }
    }

    private void setupGUI() {
        this.effectPanel = new JPanel();
        this.effectPanel.setLayout(new BoxLayout(this.effectPanel, BoxLayout.Y_AXIS));
        String effectTitle = "";
        switch (effectType) {
            case GENERATED_FADE -> effectTitle = "Fade Effect";
            case STATIC_COLOR -> effectTitle = "Static Color Effect";
            case WAVE -> effectTitle = "Wave Effect";
            case ALTERNATING_COLOR -> effectTitle = "Alternating Color Effect";
            case RIPPLE -> effectTitle = "Ripple Effect";
            case CIRCLE_CHASE -> effectTitle = "Circle Chase Effect";
            case CHASE -> effectTitle = "Chase Effect";
        }
        Border innerBorder = BorderFactory.createTitledBorder(effectTitle);
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        this.effectPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        for (JComponent[] jc : panelComponents) {
            JPanel innerPanel = new JPanel();
            innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
            innerPanel.add(jc[0]);
            if (jc.length > 1) {
                innerPanel.add(jc[1]);
            }
            this.effectPanel.add(innerPanel);
        }
    }

    private void setComponentSize(JComponent component, int width, int height) {
        component.setPreferredSize(new Dimension(width,height));
        component.setMaximumSize(new Dimension(width,height));
        component.setMinimumSize(new Dimension(width,height));
    }

    private void setupChaseGUI() {
        this.effectPanel = new JPanel();

        JButton colorButton1 = new JButton();
        setComponentSize(colorButton1, 20, 20);
        colorButton1.setFocusable(false);
        colorButton1.addActionListener(this);
        JButton colorButton2 = new JButton();
        setComponentSize(colorButton2, 20, 20);
        colorButton2.setFocusable(false);
        colorButton2.addActionListener(this);

        if (!effectMod.getChaseSequence().isEmpty()) {
            colorButton1.setBackground(effectMod.getChaseSequence().get(0));
            colorButton2.setBackground(effectMod.getChaseSequence().get(1));
        } else {
            colorButton1.setBackground(Color.black);
            colorButton2.setBackground(Color.black);
        }

        colorButtons.add(colorButton1);
        colorButtons.add(colorButton2);

        for (int i = 2; i < effectMod.getChaseSequence().size(); i++) {
            JButton colorButton = new JButton();
            setComponentSize(colorButton, 20, 20);
            colorButton.setFocusable(false);
            colorButton.addActionListener(this);
            colorButton.setBackground(effectMod.getChaseSequence().get(i));
            colorButtons.add(colorButton);
        }

        JButton addColorButton = new JButton();
        setComponentSize(addColorButton, 20, 20);
        addColorButton.setFocusable(false);
        addColorButton.addActionListener(this);
        addColorButton.setBackground(Color.black);
        colorButtons.add(addColorButton);

        dirBox.setSelected(effectMod.isDirection());

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        //////////////// 0th Row ////////////////

        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = startTimeLabel;
        currentComponents[1] = endTimeLabel;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Set count by: ");
        setComponentSize(durationTypeSelect, 100, 25);
        currentComponents[1] = durationTypeSelect;
        panelComponents.add(currentComponents);

        //////////////// 2nd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Color 1");
        currentComponents[1] = colorButton1;
        panelComponents.add(currentComponents);

        //////////////// 3rd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Color 2");
        currentComponents[1] = colorButton2;
        panelComponents.add(currentComponents);

        int adj = 0;

        for (int i = 2; i < colorButtons.size(); i++) {
            adj++;
            currentComponents = new JComponent[2];
            currentComponents[0] = new JLabel("Color " + (2 + adj));
            currentComponents[1] = colorButtons.get(i);
            panelComponents.add(currentComponents);
        }

        //////////////// 4th +n Row ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = durationLabel;
        setComponentSize(durationField, 100, 25);
        currentComponents[1] = durationField;
        panelComponents.add(currentComponents);

        //////////////// 5th +n Row ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = speedLabel;
        setComponentSize(speedField, 100, 25);
        currentComponents[1] = speedField;
        panelComponents.add(currentComponents);

        //////////////// 6th +n Row ////////////////
        currentComponents = new JComponent[1];
        currentComponents[0] = dirBox;
        panelComponents.add(currentComponents);

        //////////////// Apply or Delete Buttons ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = deleteBtn;
        currentComponents[1] = applyBtn;
        panelComponents.add(currentComponents);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupWaveGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(20, 20));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(20,20));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);


        //////////////// 0th Row ////////////////
        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = startTimeLabel;
        currentComponents[1] = endTimeLabel;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Set count by: ");
        setComponentSize(durationTypeSelect, 100, 25);
        currentComponents[1] = durationTypeSelect;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = staticColorLabel;
        setComponentSize(startColorBtn, 20, 20);
        currentComponents[1] = startColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 2nd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = waveColorLabel;
        setComponentSize(endColorBtn, 20, 20);
        currentComponents[1] = endColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 3rd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = durationLabel;
        setComponentSize(durationField, 100, 25);
        currentComponents[1] = durationField;
        panelComponents.add(currentComponents);

        //////////////// 4th Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = speedLabel;
        setComponentSize(speedField, 100, 25);
        currentComponents[1] = speedField;

        //////////////// 5th Row ////////////////
        currentComponents = new JComponent[1];
        currentComponents[0] = upOrSideBox;
        panelComponents.add(currentComponents);

        //////////////// 6th Row ////////////////
        currentComponents = new JComponent[1];
        currentComponents[0] = directionBox;
        panelComponents.add(currentComponents);

        //////////////// Apply or Delete Buttons ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = deleteBtn;
        currentComponents[1] = applyBtn;
        panelComponents.add(currentComponents);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupCircleChaseGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(20, 20));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(20,20));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        //////////////// 0th Row ////////////////

        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = startTimeLabel;
        currentComponents[1] = endTimeLabel;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Set count by: ");
        setComponentSize(durationTypeSelect, 100, 25);
        currentComponents[1] = durationTypeSelect;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = staticColorLabel;
        setComponentSize(startColorBtn, 20, 20);
        currentComponents[1] = startColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 2nd Row ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = endColorLabel;
        setComponentSize(endColorBtn, 20, 20);
        currentComponents[1] = endColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 3rd Row ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = durationLabel;
        setComponentSize(durationField, 100, 25);
        currentComponents[1] = durationField;
        panelComponents.add(currentComponents);

        //////////////// 4th Row ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = speedLabel;
        setComponentSize(speedField, 100, 25);
        currentComponents[1] = speedField;
        panelComponents.add(currentComponents);

        //////////////// 5th Row ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = angleLabel;
        setComponentSize(angleField, 100, 25);
        currentComponents[1] = angleField;
        panelComponents.add(currentComponents);

        //////////////// 6th Row ////////////////

        currentComponents = new JComponent[1];
        currentComponents[0] = clockwiseBox;
        panelComponents.add(currentComponents);

        //////////////// Apply or Delete Buttons ////////////////

        currentComponents = new JComponent[2];
        currentComponents[0] = deleteBtn;
        currentComponents[1] = applyBtn;
        panelComponents.add(currentComponents);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupRippleGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(20, 20));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(20,20));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        //////////////// 0th Row ////////////////
        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = startTimeLabel;
        currentComponents[1] = endTimeLabel;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Set count by: ");
        setComponentSize(durationTypeSelect, 100, 25);
        currentComponents[1] = durationTypeSelect;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = staticColorLabel;
        setComponentSize(startColorBtn, 20, 20);
        currentComponents[1] = startColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 2nd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = endColorLabel;
        setComponentSize(endColorBtn, 20, 20);
        currentComponents[1] = endColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 3rd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = durationLabel;
        setComponentSize(durationField, 100, 25);
        currentComponents[1] = durationField;
        panelComponents.add(currentComponents);

        //////////////// 4th Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = speedLabel;
        setComponentSize(speedField, 100, 25);
        currentComponents[1] = speedField;

        //////////////// 5th Row ////////////////
        currentComponents = new JComponent[1];
        currentComponents[0] = upOrSideBox;
        panelComponents.add(currentComponents);

        //////////////// 6th Row ////////////////
        currentComponents = new JComponent[1];
        currentComponents[0] = directionBox;
        panelComponents.add(currentComponents);

        //////////////// Apply or Delete Buttons ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = deleteBtn;
        currentComponents[1] = applyBtn;
        panelComponents.add(currentComponents);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupStaticColorGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(20, 20));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        //////////////// 0th Row ////////////////
        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = startTimeLabel;
        currentComponents[1] = endTimeLabel;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = staticColorLabel;
        setComponentSize(startColorBtn, 20, 20);
        currentComponents[1] = startColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 2nd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = durationLabel;
        setComponentSize(delayField, 100, 25);
        currentComponents[1] = delayField;
        panelComponents.add(currentComponents);

        //////////////// Apply or Delete Buttons ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = deleteBtn;
        currentComponents[1] = applyBtn;
        panelComponents.add(currentComponents);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupFadeGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(20, 20));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(20, 20));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        //////////////// 0th Row ////////////////
        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = startTimeLabel;
        currentComponents[1] = endTimeLabel;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Set count by: ");
        setComponentSize(durationTypeSelect, 100, 25);
        currentComponents[1] = durationTypeSelect;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = staticColorLabel;
        setComponentSize(startColorBtn, 20, 20);
        currentComponents[1] = startColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 2nd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = endColorLabel;
        setComponentSize(endColorBtn, 20, 20);
        currentComponents[1] = endColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 3rd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = durationLabel;
        setComponentSize(durationField, 100, 25);
        currentComponents[1] = durationField;
        panelComponents.add(currentComponents);

        //////////////// Apply or Delete Buttons ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = deleteBtn;
        currentComponents[1] = applyBtn;
        panelComponents.add(currentComponents);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupAlternatingColorGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(20, 20));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(20, 20));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        //////////////// 0th Row ////////////////
        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = startTimeLabel;
        currentComponents[1] = endTimeLabel;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = new JLabel("Set count by: ");
        setComponentSize(durationTypeSelect, 100, 25);
        currentComponents[1] = durationTypeSelect;
        panelComponents.add(currentComponents);

        //////////////// 1st Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = color1Label;
        setComponentSize(startColorBtn, 20, 20);
        currentComponents[1] = startColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 2nd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = color2Label;
        setComponentSize(endColorBtn, 20, 20);
        currentComponents[1] = endColorBtn;
        panelComponents.add(currentComponents);

        //////////////// 3rd Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = durationLabel;
        setComponentSize(durationField, 100, 25);
        currentComponents[1] = durationField;
        panelComponents.add(currentComponents);

        //////////////// 4th Row ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = rateLabel;
        setComponentSize(speedField, 100, 25);
        currentComponents[1] = speedField;
        panelComponents.add(currentComponents);

        //////////////// Apply or Delete Buttons ////////////////
        currentComponents = new JComponent[2];
        currentComponents[0] = deleteBtn;
        currentComponents[1] = applyBtn;
        panelComponents.add(currentComponents);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
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


    private double calcBattery(int R, int G, int B, double secondsDuration) {
        double normR = R/255.0;
        double normG = G/255.0;
        double normB = B/255.0;

        // Coefficients for power consumption
        double coeffR = 1.0;
        double coeffG = 1.2;
        double coeffB = 1.5;

        // Total power consumption relative to full white
        double totalPower = (normR * coeffR + normG * coeffG + normB * coeffB) / (coeffR + coeffG + coeffB);

        // Calculate battery usage as a percentage
        double batteryUsage = (totalPower * (secondsDuration/60.0)) / 90.0;  // 90 minutes is the baseline for full white at 100%

        return batteryUsage * 100;  // Convert to percentage
    }

    private void loadEffectToGUI(Effect effect) {

        // Get start and end colors
        startColorBtn.setBackground(effect.getStartColor());
        endColorBtn.setBackground(effect.getEndColor());

        // Get delay, duration, and timeout
        String delayStr = String.valueOf(effect.getDelay().toNanos() / 1_000_000_000.0);
        String durationStr = String.valueOf(effect.getDuration().toNanos() / 1_000_000_000.0);
        String timeoutStr = String.valueOf(effect.getTimeout().toNanos() / 1_000_000_000.0);
        String speedStr = String.valueOf(effect.getSpeed());
        String angleStr = String.valueOf(effect.getAngle());
        delayField.setText(delayStr);
        durationField.setText(durationStr);
        timeoutField.setText(timeoutStr);
        speedField.setText(speedStr);
        angleField.setText(angleStr);

        TIME_GRADIENTBox.setSelected(effect.isUSE_DURATION());
        SET_TIMEOUTBox.setSelected(effect.isSET_TIMEOUT());
        DO_DELAYBox.setSelected(effect.isDO_DELAY());
        INSTANT_COLORBox.setSelected(effect.isINSTANT_COLOR());
        upOrSideBox.setSelected(effect.isUpOrSide());
        clockwiseBox.setSelected(effect.isDirection());
        directionBox.setSelected(effect.isDirection());

        // Calculate start time label
        long minutesStart = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(effect.getStartTimeMSec());
        long secondsStart = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(effect.getStartTimeMSec()) % 60;
        long millisecondsStart = effect.getStartTimeMSec() % 1000;
        startTimeLabel.setText(String.format("Start: %d:%02d:%03d", minutesStart, secondsStart, millisecondsStart));

        long minutesEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(effect.getEndTimeMSec());
        long secondsEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(effect.getEndTimeMSec()) % 60;
        long millisecondsEnd = effect.getEndTimeMSec() % 1000;
        endTimeLabel.setText(String.format("End: %d:%02d:%03d", minutesEnd, secondsEnd, millisecondsEnd));

        //calcBattery
        int avgRed = (int)((effect.getStartColor().getRed() + effect.getEndColor().getRed())/2);
        int avgBlue = (int)((effect.getStartColor().getBlue() + effect.getEndColor().getBlue())/2);
        int avgGreen = (int)((effect.getStartColor().getGreen() + effect.getEndColor().getGreen())/2);
        double battEst = calcBattery(avgRed, avgBlue, avgGreen, Double.parseDouble(durationStr));
        batteryEstLabel.setText(String.format("Battery Usage: %.2f%%", battEst));

        if (this.isNewEffect) {
            applyBtn.setText("Create effect");
            deleteBtn.setEnabled(false);
        } else {
            applyBtn.setText("Update effect");
            deleteBtn.setEnabled(true);
        }
    }

    private void liveUpdateEndTime() {

        // Calculate the new end time, live gui update
        long newEndTime = effectMod.getStartTimeMSec();

        long delayMSec = 0;
        long durationMSec = 0;
        long timeoutMSec = 0;
        try {
            if (durationType.equals("Seconds")) {
                if (delayField.isShowing()) {
                    delayMSec = (long) (Float.parseFloat(delayField.getText()) * 1000);
                }
                if (durationField.isShowing()) {
                    durationMSec = (long) (Float.parseFloat(durationField.getText()) * 1000);
                }
                if (timeoutField.isShowing()) {
                    timeoutMSec = (long) (Float.parseFloat(timeoutField.getText()) * 1000);
                }
                newEndTime = newEndTime + delayMSec + durationMSec + timeoutMSec;
            } else {
                TimeManager timeManager = effectListener.onTimeRequired();
                int startCount = 0;
                for (int i = 0; i < timeManager.getCount2MSec().size() - 1; i++) {
                    long startMsec = this.effectMod.getStartTimeMSec();
                    if (startMsec >= timeManager.getCount2MSec().get(i) && startMsec < timeManager.getCount2MSec().get(i + 1)) {
                        startCount = i;
                        break;
                    }
                }
                String delayFieldText;
                if (delayField.getText().contains(".")) {
                    delayFieldText = delayField.getText().substring(0, delayField.getText().indexOf("."));
                } else {
                    delayFieldText = delayField.getText();
                }
                String durationFieldText;
                if (durationField.getText().contains(".")) {
                    durationFieldText = durationField.getText().substring(0, durationField.getText().indexOf("."));
                } else {
                    durationFieldText = durationField.getText();
                }
                int delayCount = Integer.parseInt(delayFieldText);
                int durationCount = Integer.parseInt(durationFieldText);
                if (DO_DELAYBox.isSelected()) {
                    newEndTime = timeManager.getCount2MSec().get(startCount + delayCount);
                }
                if (TIME_GRADIENTBox.isSelected()) {
                    newEndTime = timeManager.getCount2MSec().get(startCount + durationCount);
                }
                if (DO_DELAYBox.isSelected() && TIME_GRADIENTBox.isSelected()) {
                    newEndTime = timeManager.getCount2MSec().get(startCount + durationCount + delayCount);
                }
                newEndTime--;
            }
        } catch (NumberFormatException nfe) {
            // System.out.println("Live End Time Calculation: Number Format Exception.");
            return;
        }


        long minutesEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(newEndTime);
        long secondsEnd = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(newEndTime) % 60;
        long millisecondsEnd = newEndTime % 1000;
        endTimeLabel.setText(String.format("End: %d:%02d:%03d", minutesEnd, secondsEnd, millisecondsEnd));
    }

    /**
     * This alternative constructor can deliver a placeholder panel with instructions about working with effects. You
     * can call this when the program is first started and the project isn't loaded or synced.
     */
    public EffectGUI(String placeholderText) {

        GridBagConstraints gridBagConstraints;

        effectPanel = new JPanel();
        placeholderLabel = new JLabel();

        effectPanel.setBorder(BorderFactory.createTitledBorder("Effect"));
        effectPanel.setLayout(new CardLayout());

        placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        placeholderLabel.setText(placeholderText);
        placeholderLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        effectPanel.add(placeholderLabel, "placeholderCard");
    }


    public JPanel getEffectPanel() {
        return effectPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(this.startColorBtn)) {
            Color selectedColor = JColorChooser.showDialog(this.effectPanel,
                                                           "Choose Start Color",
                                                           this.effectMod.getStartColor());
            if (selectedColor != null) {
                this.effectMod.setStartColor(selectedColor);
                this.startColorBtn.setBackground(selectedColor);
            }
        } else if (e.getSource().equals(this.endColorBtn)) {
            Color selectedColor = JColorChooser.showDialog(this.effectPanel,
                                                           "Choose End Color",
                                                           this.effectMod.getEndColor());
            if (selectedColor != null) {
                this.effectMod.setEndColor(selectedColor);
                this.endColorBtn.setBackground(selectedColor);
            }
        } else if (e.getSource().equals(this.durationTypeSelect)) {
            if (durationTypeSelect.getSelectedItem().equals("Seconds") && durationType.equals("Counts")) {
                durationType = "Seconds";
                TimeManager timeManager = effectListener.onTimeRequired();
                int startCount = 0;
                for (int i = 0; i < timeManager.getCount2MSec().size() - 1; i++) {
                    long startMsec = this.effectMod.getStartTimeMSec();
                    if (startMsec >= timeManager.getCount2MSec().get(i) && startMsec < timeManager.getCount2MSec().get(i + 1)) {
                        startCount = i;
                        break;
                    }
                }

                int durationCounts = Integer.parseInt(durationField.getText());
                long durationMsec = timeManager.getCount2MSec().get(startCount + durationCounts) - effectMod.getStartTimeMSec() - 1;
                String durationText = Long.toString(durationMsec);
                System.out.println("Before: " + durationText);
                durationText = durationText.substring(0, durationText.length() - 3) + "." + durationText.substring(durationText.length() - 3);
                System.out.println("After: " + durationText);
                durationField.setText(durationText);
                liveUpdateEndTime();
            } else if (durationTypeSelect.getSelectedItem().equals("Counts") && durationType.equals("Seconds")) {
                durationType = "Counts";
                TimeManager timeManager = effectListener.onTimeRequired();
                int startCount = 0;
                for (int i = 0; i < timeManager.getCount2MSec().size() - 1; i++) {
                    long startMsec = this.effectMod.getStartTimeMSec();
                    if (startMsec >= timeManager.getCount2MSec().get(i) && startMsec < timeManager.getCount2MSec().get(i + 1)) {
                        startCount = i;
                        break;
                    }
                }
                int endCount = 0;
                long durationMsec = (long) (Double.parseDouble(durationField.getText()) * 1000);
                long endMsec = this.effectMod.getStartTimeMSec() + durationMsec;
                for (int i = 0; i < timeManager.getCount2MSec().size() - 1; i++) {
                    if (endMsec >= timeManager.getCount2MSec().get(i) - 1 && endMsec < timeManager.getCount2MSec().get(i + 1) - 1) {
                        endCount = i;
                    }
                }
                durationField.setText(Integer.toString(endCount - startCount));
                liveUpdateEndTime();
            }
        } else if (e.getSource().equals(this.applyBtn)) {
            applyToEffectMod();
            if (this.effectMod.getEffectType() == EffectList.STATIC_COLOR) {
                this.effectMod.setDO_DELAY(true);
                this.effectMod.setUSE_DURATION(false);
            }
            if (this.effectMod.getEffectType() == EffectList.CHASE) {
                this.effectMod.getChaseSequence().remove(this.effectMod.getChaseSequence().size() - 1);
            }
            if (this.isNewEffect) effectListener.onCreateEffect(this.effectMod);
            else effectListener.onUpdateEffect(this.effect, this.effectMod);
        } else if (e.getSource().equals(this.deleteBtn)) {
            effectListener.onDeleteEffect(this.effect); // Delete target is the original
        } else if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            int i;
            for (i = 0; i < colorButtons.size(); i++) {
                if (button == colorButtons.get(i)) {
                    break;
                }
            }
            Color color = JColorChooser.showDialog(this.effectPanel,
                                                    "Choose Color " + (i+1),
                                                    button.getBackground());
            button.setBackground(color);
            if (button.equals(colorButtons.get(colorButtons.size() - 1))) {
                applyToEffectMod();
                effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect);
            }
        }
    }

    private void applyToEffectMod() {

        // Start and end color are applied automatically to effectCopy

        long delayMSec = 0;
        long durationMSec = 0;
        long timeoutMSec = 0;
        if (durationType.equals("Seconds")) {
            if (delayField.isShowing()) {
                delayMSec = (long) (Float.parseFloat(delayField.getText()) * 1000);
            }
            if (durationField.isShowing()) {
                durationMSec = (long) (Float.parseFloat(durationField.getText()) * 1000);
            }
            if (timeoutField.isShowing()) {
                timeoutMSec = (long) (Float.parseFloat(timeoutField.getText()) * 1000);
            }
        } else {
            TimeManager timeManager = effectListener.onTimeRequired();
            int startCount = 0;
            for (int i = 0; i < timeManager.getCount2MSec().size() - 1; i++) {
                long startMsec = this.effectMod.getStartTimeMSec();
                if (startMsec >= timeManager.getCount2MSec().get(i) && startMsec < timeManager.getCount2MSec().get(i + 1)) {
                    startCount = i;
                    break;
                }
            }
            String delayFieldText;
            if (delayField.getText().contains(".")) {
                delayFieldText = delayField.getText().substring(0, delayField.getText().indexOf("."));
            } else {
                delayFieldText = delayField.getText();
            }
            String durationFieldText;
            if (durationField.getText().contains(".")) {
                durationFieldText = durationField.getText().substring(0, durationField.getText().indexOf("."));
            } else {
                durationFieldText = durationField.getText();
            }
            int delayCount = Integer.parseInt(delayFieldText);
            int durationCount = Integer.parseInt(durationFieldText);
            if (DO_DELAYBox.isSelected()) {
                delayMSec = timeManager.getCount2MSec().get(startCount + delayCount) - effectMod.getStartTimeMSec();
            } else if (TIME_GRADIENTBox.isSelected()) {
                durationMSec = timeManager.getCount2MSec().get(startCount + durationCount) - effectMod.getStartTimeMSec();
            }
            if (DO_DELAYBox.isSelected() && TIME_GRADIENTBox.isSelected()) {
                durationMSec = timeManager.getCount2MSec().get(startCount + durationCount + delayCount) - delayMSec;
            }
            durationMSec--;
        }




        Duration delay = Duration.ofMillis(delayMSec);
        Duration duration = Duration.ofMillis(durationMSec);
        Duration timeout = Duration.ofMillis(timeoutMSec);
        this.effectMod.setDelay(delay);
        this.effectMod.setDuration(duration);
        this.effectMod.setTimeout(timeout);
        this.effectMod.setSpeed(Double.parseDouble(speedField.getText()));
        this.effectMod.setAngle(Double.parseDouble(angleField.getText()));

        this.effectMod.setUSE_DURATION(this.TIME_GRADIENTBox.isSelected());
        this.effectMod.setSET_TIMEOUT(this.SET_TIMEOUTBox.isSelected());
        this.effectMod.setDO_DELAY(this.DO_DELAYBox.isSelected());
        this.effectMod.setINSTANT_COLOR(this.INSTANT_COLORBox.isSelected());
        this.effectMod.setUpOrSide(this.upOrSideBox.isSelected());
        if (effectType == EffectList.CIRCLE_CHASE) {
            this.effectMod.setDirection(this.clockwiseBox.isSelected());
        } else {
            this.effectMod.setDirection(this.directionBox.isSelected());
        }
        if (this.effectType == EffectList.CHASE) {
            this.effectMod.setDirection(this.dirBox.isSelected());
            ArrayList<Color> chaseSequence = new ArrayList<>();
            for (JButton button : colorButtons) {
                chaseSequence.add(button.getBackground());
            }
            this.effectMod.setChaseSequence(chaseSequence);
        }

    }

}