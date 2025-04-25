package org.emrick.project.effect;

import org.emrick.project.EffectsGroup;
import org.emrick.project.LEDStrip;
import org.emrick.project.Performer;
import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.util.List;
import java.util.*;

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
    JLabel speedLabel = new JLabel("Speed: ");
    JLabel angleLabel = new JLabel("Start Angle (deg): ");
    JButton startColorBtn = new JButton();
    JButton endColorBtn = new JButton();
    JTextField delayField = new JTextField(10);
    JTextField durationField = new JTextField(10);
    JTextField timeoutField = new JTextField(10);
    JTextField speedField = new JTextField(10);
    JTextField angleField = new JTextField(10);
    JButton applyBtn = new JButton("REPLACE THIS TEXT WITH UPDATE OR CREATE EFFECT TEXT");
    JButton deleteBtn = new JButton("Delete effect");
    JLabel batteryEstLabel = new JLabel("Estimated Battery Usage: ");
    ArrayList<JButton> colorButtons = new ArrayList<>();
    String[] durationTypeOptions = {"Seconds", "Counts"};
    JComboBox<String> durationTypeSelect = new JComboBox<String>(durationTypeOptions);
    String durationType = "Seconds";
    ArrayList<JComponent[]> panelComponents = new ArrayList<>();
    JLabel addShapeLabel = new JLabel("Add Shape");
    JButton addShapeBtn = new JButton();
    int showGridIndex = -1;
    JTextField hMovementField = new JTextField(10);
    JTextField vMovementField = new JTextField(10);
    JCheckBox wholePerformer = new JCheckBox("Move by performer");
    String[] rotationOptions = {"Clockwise", "Counterclockwise"};
    JComboBox<String> rotationSelect = new JComboBox<>(rotationOptions);
    String[] directionOptions = {"Right", "Left", "Up", "Down"};
    JComboBox<String> directionSelect = new JComboBox<>(directionOptions);
    JCheckBox varyBrightnessBox = new JCheckBox("  (Opens Text Fields)");
    JCheckBox varyColorBox = new JCheckBox("  (Opens Text Field)");
    JCheckBox varyTimeBox = new JCheckBox("  (Opens Text Fields)");
    JCheckBox fadeBox = new JCheckBox("  (Toggles Fade)");
    JTextField colorVarianceField = new JTextField(10);
    JTextField minBrightnessField = new JTextField(10);
    JTextField maxBrightnessField = new JTextField(10);
    JTextField maxTimeField = new JTextField(10);
    JTextField minTimeField = new JTextField(10);

    private JLabel placeholderLabel;
    private EffectList effectType;

    /**
     * @param effect    The current effect, as it exists. Passed in null if it doesn't exist.
     * @param startTime In the case that no effect exists for the performer at the given time, we still need the current
     *                  time for gui display.
     */
    public EffectGUI(Effect effect, long startTime, EffectListener effectListener, EffectList effectType, boolean isNew, int index) {
        this.effect = effect;
        this.effectListener = effectListener;
        effectListener.onChangeSelectionMode(index != -1, index != -1 ? effect.getShapes()[index].getLedStrips() : new HashSet<>());
        this.effectType = effectType;
        this.isNewEffect = true;
        this.showGridIndex = index;

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
            case GRID -> setupGridGUI();
            case NOISE -> setupNoiseGUI();
        }
    }

    private void setupGUI() {
        // Create main panel with border layout
        this.effectPanel = new JPanel(new BorderLayout());
        
        // Create content panel that will hold all components
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1.0;
        
        // Create scroll pane for the content
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null); // Remove scroll pane border
        
        // Set up the title border
        String effectTitle = switch (effectType) {
            case GENERATED_FADE -> "Fade Effect";
            case STATIC_COLOR -> "Static Color Effect"; 
            case WAVE -> "Wave Effect";
            case ALTERNATING_COLOR -> "Alternating Color Effect";
            case RIPPLE -> "Ripple Effect";
            case CIRCLE_CHASE -> "Circle Chase Effect";
            case CHASE -> "Chase Effect";
            case GRID -> "Grid Effect";
            case NOISE -> "Noise Effect";
            default -> throw new IllegalStateException("Unexpected effect type: " + effectType);
        };
        
        Border innerBorder = BorderFactory.createTitledBorder(effectTitle);
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        this.effectPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        // Remove the buttons from panelComponents and store them
        JComponent[] buttonComponents = null;
        if (!panelComponents.isEmpty() && 
            panelComponents.get(panelComponents.size() - 1)[0] == deleteBtn) {
            buttonComponents = panelComponents.remove(panelComponents.size() - 1);
        }

        // Add remaining components to the grid
        int row = 0;
        for (JComponent[] components : panelComponents) {
            gbc.gridy = row++;
            
            if (components.length == 1) {
                gbc.gridx = 0;
                gbc.gridwidth = 2;
                contentPanel.add(components[0], gbc);
            } else if (components.length == 2) {
                gbc.gridwidth = 1;
                
                gbc.gridx = 0;
                gbc.weightx = 0.4;
                if (components[0] instanceof JLabel) {
                    ((JLabel) components[0]).setHorizontalAlignment(SwingConstants.RIGHT);
                }
                contentPanel.add(components[0], gbc);
                
                gbc.gridx = 1;
                gbc.weightx = 0.6;
                contentPanel.add(components[1], gbc);
            }
        }

        // Add a dummy component at the bottom to push everything up
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        JPanel filler = new JPanel();
        contentPanel.add(filler, gbc);

        // Create button panel with centered alignment
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        if (buttonComponents != null) {
            // Set preferred size for buttons to make them more visually balanced
            deleteBtn.setPreferredSize(new Dimension(120, 25));
            applyBtn.setPreferredSize(new Dimension(120, 25));
            
            buttonPanel.add(buttonComponents[0]); // Delete button
            buttonPanel.add(buttonComponents[1]); // Apply button
        }
        
        // Add components to the main panel
        this.effectPanel.add(scrollPane, BorderLayout.CENTER);
        this.effectPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupComponent(JComponent component, int width, int height) {
        component.setPreferredSize(new Dimension(width, height));
        
        // Additional styling based on component type
        if (component instanceof JTextField) {
            ((JTextField) component).setHorizontalAlignment(JTextField.LEFT);
        } else if (component instanceof JButton) {
            ((JButton)component).setFocusPainted(false);
        } else if (component instanceof JLabel) {
            ((JLabel) component).setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    // Helper method to create aligned checkbox panel
    private JPanel createCheckBoxPanel(JCheckBox checkBox) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(checkBox);
        return panel;
    }

    // Helper method to add component pairs
    private void addComponentPair(JComponent comp1, JComponent comp2) {
        JComponent[] currentComponents = new JComponent[2];
        currentComponents[0] = comp1;
        currentComponents[1] = comp2;
        panelComponents.add(currentComponents);
    }

    private void setupNoiseGUI() {
        panelComponents.clear();

        // Set up components
        setupComponent(startColorBtn, 25, 25);
        setupComponent(durationField, 100, 25);
        setupComponent(durationTypeSelect, 100, 25);
        setupComponent(maxTimeField, 100, 25);
        setupComponent(minTimeField, 100, 25);
        setupComponent(maxBrightnessField, 100, 25);
        setupComponent(minBrightnessField, 100, 25);
        setupComponent(colorVarianceField, 100, 25);

        // Add listeners
        startColorBtn.addActionListener(this);
        durationField.getDocument().addDocumentListener(getDocumentListener());
        durationTypeSelect.addActionListener(this);
        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        // Add time components
        addComponentPair(startTimeLabel, endTimeLabel);
        
        // Add dropdown with label
        addComponentPair(new JLabel("Duration Type: "), durationTypeSelect);
        
        // Add color components
        addComponentPair(startColorLabel, startColorBtn);
        addComponentPair(durationLabel, durationField);

        // Add checkboxes with labels
        addComponentPair(new JLabel("Vary Time: "), createCheckBoxPanel(varyTimeBox));
        addComponentPair(new JLabel("Vary Brightness: "), createCheckBoxPanel(varyBrightnessBox));
        addComponentPair(new JLabel("Vary Color: "), createCheckBoxPanel(varyColorBox));
        addComponentPair(new JLabel("Fade: "), createCheckBoxPanel(fadeBox));

        // Setup checkbox listeners
        varyTimeBox.setSelected(effectMod.isVaryTime());
        varyTimeBox.addActionListener(e -> {
            applyToEffectMod();
            effectMod.setVaryTime(varyTimeBox.isSelected());
            effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, showGridIndex);
        });

        varyBrightnessBox.setSelected(effectMod.isVaryBrightness());
        varyBrightnessBox.addActionListener(e -> {
            applyToEffectMod();
            effectMod.setVaryBrightness(varyBrightnessBox.isSelected());
            effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, showGridIndex);
        });

        varyColorBox.setSelected(effectMod.isVaryColor());
        varyColorBox.addActionListener(e -> {
            applyToEffectMod();
            effectMod.setVaryColor(varyColorBox.isSelected());
            effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, showGridIndex);
        });

        fadeBox.setSelected(effectMod.isFade());
        fadeBox.addActionListener(e -> {
            applyToEffectMod();
            effectMod.setFade(fadeBox.isSelected());
        });

        // Add conditional components based on checkbox states
        if (effectMod.isVaryTime()) {
            addComponentPair(new JLabel("Max Time (ms): "), maxTimeField);
            addComponentPair(new JLabel("Min Time (ms): "), minTimeField);
        } else {
            addComponentPair(new JLabel("Time (ms): "), maxTimeField);
        }

        if (effectMod.isVaryBrightness()) {
            addComponentPair(new JLabel("Max Brightness: "), maxBrightnessField);
            addComponentPair(new JLabel("Min Brightness: "), minBrightnessField);
        } else {
            addComponentPair(new JLabel("Brightness: "), maxBrightnessField);
        }

        if (effectMod.isVaryColor()) {
            addComponentPair(new JLabel("Color Variance: "), colorVarianceField);
        }

        // Add action buttons
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();
        loadEffectToGUI(effectMod);
    }

    private void setupGridGUI() {
        this.effectPanel = new JPanel();

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        addComponentPair(startTimeLabel, endTimeLabel);
        addComponentPair(new JLabel("Set count by: "), durationTypeSelect);
        addComponentPair(durationLabel, durationField);

        if (effectMod.getShapes() == null) {
            effectMod.setShapes(new GridShape[1]);
            effectMod.getShapes()[0] = new GridShape(new boolean[1][1], new Point(0,0), 2, Color.BLACK);
        }

        if (showGridIndex == -1) {
            for (int i = 0; i < effectMod.getShapes().length; i++) {
                JButton editShapeButton = new JButton("Edit Shape " + (i + 1));
                int index = i;
                editShapeButton.addActionListener(e -> {
                    applyToEffectMod();
                    effectListener.onChangeSelectionMode(true, effectMod.getShapes()[index].getLedStrips());
                    effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, index);
                });
                JButton deleteShapeButton = new JButton("Delete Shape " + (i + 1));
                deleteShapeButton.addActionListener(e -> {
                    GridShape[] shapes = effectMod.getShapes();
                    int next = 0;
                    boolean skipped = false;
                    GridShape[] tmp = new GridShape[shapes.length-1];
                    for (GridShape shape : shapes) {
                        if (next == index) {
                            skipped = true;
                        } else if (skipped) {
                            tmp[next-1] = shape;
                            next++;
                        } else {
                            tmp[next] = shape;
                            next++;
                        }
                    }
                    effectMod.setShapes(tmp);
                    effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, -1);
                });
                addComponentPair(editShapeButton, deleteShapeButton);
            }

            JButton addButton = new JButton("Add Shape");
            addButton.addActionListener(e -> {
                GridShape[] prevShapes = effectMod.getShapes();
                GridShape[] newShapes = new GridShape[prevShapes.length + 1];
                for (int i = 0; i < prevShapes.length; i++) {
                    newShapes[i] = prevShapes[i];
                }
                newShapes[newShapes.length-1] = new GridShape(new boolean[1][1], new Point(0,0), 2, Color.BLACK);
                effectMod.setShapes(newShapes);
                effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, showGridIndex);
            });
            addComponentPair(new JLabel(""), addButton);

        } else {
            JButton colorButton = new JButton();
            colorButton.addActionListener(this);
            setupComponent(colorButton, 25, 25);
            colorButton.setBackground(effectMod.getShapes()[showGridIndex].getColor());
            addComponentPair(new JLabel("Set Shape Color:"), colorButton);

            setupComponent(hMovementField, 100, 25);
            hMovementField.setText(Integer.toString(effectMod.getShapes()[showGridIndex].getMovement().x));
            addComponentPair(new JLabel("Set horizontal movement:"), hMovementField);

            setupComponent(vMovementField, 100, 25);
            vMovementField.setText(Integer.toString(effectMod.getShapes()[showGridIndex].getMovement().y));
            addComponentPair(new JLabel("Set vertical movement:"), vMovementField);

            wholePerformer.setSelected(effectMod.getShapes()[showGridIndex].getSpeed() == 2);
            addComponentPair(new JLabel("Whole Performer:"), wholePerformer);

            JButton doneButton = new JButton("Done");
            doneButton.addActionListener(e -> {
                applyToEffectMod();
                ArrayList<LEDStrip> strips = new ArrayList<>();
                Iterator<LEDStrip> iterator = effectListener.onSelectionRequired().iterator();
                while (iterator.hasNext()) {
                    strips.add(iterator.next());
                }
                LEDStrip[][] grid = GridEffect.buildGrid(strips, effectMod.getWidth(), effectMod.getHeight());
                HashSet<LEDStrip> selectedStrips = effectListener.onInnerSelectionRequired();
                boolean[][] untrimmedGrid = new boolean[effectMod.getHeight()][effectMod.getWidth()];
                for (int j = 0; j < grid.length; j++) {
                    for (int k = 0; k < grid[j].length; k++) {
                        untrimmedGrid[j][k] = selectedStrips.contains(grid[j][k]);
                    }
                }
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                for (int j = 0; j < untrimmedGrid.length; j++) {
                    for (int k = 0; k < untrimmedGrid[j].length; k++) {
                        if (untrimmedGrid[j][k]) {
                            if (j > maxY) {
                                maxY = j;
                            }
                            if (j < minY) {
                                minY = j;
                            }
                            if (k > maxX) {
                                maxX = k;
                            }
                            if (k < minX) {
                                minX = k;
                            }
                        }
                    }
                }
                int dimX = maxX - minX + 1;
                int dimY = maxY - minY + 1;
                boolean[][] trimmedGrid = new boolean[dimY][dimX];
                for (int j = minY; j <= maxY; j++) {
                    for (int k = minX; k <= maxX; k++) {
                        trimmedGrid[j-minY][k-minX] = untrimmedGrid[j][k];
                    }
                }
                if (wholePerformer.isSelected()) {
                    effectMod.getShapes()[showGridIndex].setSpeed(2);
                } else {
                    effectMod.getShapes()[showGridIndex].setSpeed(1);
                }
                effectMod.getShapes()[showGridIndex].setShape(trimmedGrid);
                Point move = effectMod.getShapes()[showGridIndex].getMovement();
                effectMod.getShapes()[showGridIndex].setLedStrips(selectedStrips);
                effectMod.getShapes()[showGridIndex].setStartPos(new Point(minX, minY));
                effectListener.onChangeSelectionMode(false, effectMod.getShapes()[showGridIndex].getLedStrips());
                effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, -1);
            });
            JComponent[] currentComponents = {doneButton};
            panelComponents.add(currentComponents);
        }

        if (showGridIndex != -1) {
            applyBtn.setEnabled(false);
        } else {
            applyBtn.setEnabled(true);
        }
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();

        loadEffectToGUI(effectMod);
    }

    private void setupChaseGUI() {
        this.effectPanel = new JPanel();

        JButton colorButton1 = new JButton();
        setupComponent(colorButton1, 25, 25);
        colorButton1.setFocusable(false);
        colorButton1.addActionListener(this);
        JButton colorButton2 = new JButton();
        setupComponent(colorButton2, 25, 25);
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
            setupComponent(colorButton, 25, 25);
            colorButton.setFocusable(false);
            colorButton.addActionListener(this);
            colorButton.setBackground(effectMod.getChaseSequence().get(i));
            colorButtons.add(colorButton);
        }

        JButton addColorButton = new JButton();
        setupComponent(addColorButton, 25, 25);
        addColorButton.setFocusable(false);
        addColorButton.addActionListener(this);
        addColorButton.setBackground(Color.black);
        colorButtons.add(addColorButton);

        if (effectMod.isDirection()) {
            rotationSelect.setSelectedItem(rotationOptions[0]);
        } else {
            rotationSelect.setSelectedItem(rotationOptions[1]);
        }

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        addComponentPair(startTimeLabel, endTimeLabel);
        addComponentPair(new JLabel("Set count by: "), durationTypeSelect);
        addComponentPair(new JLabel("Color 1"), colorButton1);
        addComponentPair(new JLabel("Color 2"), colorButton2);

        int adj = 0;
        for (int i = 2; i < colorButtons.size(); i++) {
            adj++;
            addComponentPair(new JLabel("Color " + (2 + adj)), colorButtons.get(i));
        }

        addComponentPair(durationLabel, durationField);
        addComponentPair(speedLabel, speedField);
        addComponentPair(new JLabel("Set rotation: "), rotationSelect);
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupWaveGUI() {
        panelComponents.clear();

        // Set up components
        setupComponent(startColorBtn, 25, 25);
        setupComponent(durationField, 100, 25);
        setupComponent(durationTypeSelect, 100, 25);
        setupComponent(directionSelect, 100, 25);
        setupComponent(speedField, 100, 25);
        setupComponent(endColorBtn, 25, 25);

        // Add listeners
        startColorBtn.addActionListener(this);
        endColorBtn.addActionListener(this);
        durationField.getDocument().addDocumentListener(getDocumentListener());
        durationTypeSelect.addActionListener(this);
        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        // Add time components
        addComponentPair(startTimeLabel, endTimeLabel);
        
        // Add dropdown with label
        addComponentPair(new JLabel("Duration Type: "), durationTypeSelect);
        
        // Add color components
        addComponentPair(startColorLabel, startColorBtn);
        addComponentPair(waveColorLabel, endColorBtn);
        addComponentPair(durationLabel, durationField);
        addComponentPair(speedLabel, speedField);
        addComponentPair(new JLabel("Wave Direction: "), directionSelect);

        // Add action buttons
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupCircleChaseGUI() {
        panelComponents.clear();

        // Set up components
        setupComponent(startColorBtn, 25, 25);
        setupComponent(durationField, 100, 25);
        setupComponent(durationTypeSelect, 100, 25);
        setupComponent(rotationSelect, 100, 25);
        setupComponent(speedField, 100, 25);
        setupComponent(angleField, 100, 25);
        setupComponent(endColorBtn, 25, 25);

        // Add listeners
        startColorBtn.addActionListener(this);
        endColorBtn.addActionListener(this);
        durationField.getDocument().addDocumentListener(getDocumentListener());
        durationTypeSelect.addActionListener(this);
        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        // Add time components
        addComponentPair(startTimeLabel, endTimeLabel);
        
        // Add dropdown with label
        addComponentPair(new JLabel("Duration Type: "), durationTypeSelect);
        
        // Add color components
        addComponentPair(staticColorLabel, startColorBtn);
        addComponentPair(endColorLabel, endColorBtn);
        addComponentPair(durationLabel, durationField);
        addComponentPair(speedLabel, speedField);
        addComponentPair(angleLabel, angleField);
        addComponentPair(new JLabel("Rotation Direction: "), rotationSelect);

        // Add action buttons
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupRippleGUI() {
        panelComponents.clear();

        // Set up components
        setupComponent(startColorBtn, 25, 25);
        setupComponent(durationField, 100, 25);
        setupComponent(durationTypeSelect, 100, 25);
        setupComponent(directionSelect, 100, 25);
        setupComponent(speedField, 100, 25);
        setupComponent(endColorBtn, 25, 25);

        // Add listeners
        startColorBtn.addActionListener(this);
        endColorBtn.addActionListener(this);
        durationField.getDocument().addDocumentListener(getDocumentListener());
        durationTypeSelect.addActionListener(this);
        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        // Add time components
        addComponentPair(startTimeLabel, endTimeLabel);
        
        // Add dropdown with label
        addComponentPair(new JLabel("Duration Type: "), durationTypeSelect);
        
        // Add color components
        addComponentPair(staticColorLabel, startColorBtn);
        addComponentPair(endColorLabel, endColorBtn);
        addComponentPair(durationLabel, durationField);
        addComponentPair(speedLabel, speedField);
        addComponentPair(new JLabel("Ripple Direction: "), directionSelect);

        // Add action buttons
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupStaticColorGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(25, 25));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        addComponentPair(startTimeLabel, endTimeLabel);
        addComponentPair(new JLabel("Set count by: " , SwingConstants.RIGHT), durationTypeSelect);
        addComponentPair(staticColorLabel, startColorBtn);
        addComponentPair(durationLabel, durationField);
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupFadeGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(25, 25));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(25, 25));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        addComponentPair(startTimeLabel, endTimeLabel);
        addComponentPair(new JLabel("Set count by: ", SwingConstants.RIGHT), durationTypeSelect);
        addComponentPair(staticColorLabel, startColorBtn);
        addComponentPair(endColorLabel, endColorBtn);
        addComponentPair(durationLabel, durationField);
        addComponentPair(deleteBtn, applyBtn);

        setupGUI();

        // If effect exists, load pattern on gui
        loadEffectToGUI(this.effectMod);
    }

    private void setupAlternatingColorGUI() {
        this.effectPanel = new JPanel();

        // Color button customization
        startColorBtn.setPreferredSize(new Dimension(25, 25));
        startColorBtn.setFocusable(false);
        startColorBtn.addActionListener(this);
        endColorBtn.setPreferredSize(new Dimension(25, 25));
        endColorBtn.setFocusable(false);
        endColorBtn.addActionListener(this);

        durationField.getDocument().addDocumentListener(getDocumentListener());

        durationTypeSelect.addActionListener(this);

        applyBtn.addActionListener(this);
        deleteBtn.addActionListener(this);

        addComponentPair(startTimeLabel, endTimeLabel);
        addComponentPair(new JLabel("Set count by: ", SwingConstants.RIGHT), durationTypeSelect);
        addComponentPair(color1Label, startColorBtn);
        addComponentPair(color2Label, endColorBtn);
        addComponentPair(durationLabel, durationField);
        addComponentPair(rateLabel, speedField);
        addComponentPair(deleteBtn, applyBtn);

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
        maxTimeField.setText(String.valueOf(effect.getMaxTime()));
        minTimeField.setText(String.valueOf(effect.getMinTime()));
        maxBrightnessField.setText(String.valueOf(effect.getMaxBrightness()));
        minBrightnessField.setText(String.valueOf(effect.getMinBrightness()));
        colorVarianceField.setText(String.valueOf(effect.getColorVariance()));

        if (effect.isDirection()) {
            if (effect.isUpOrSide()) {
                directionSelect.setSelectedItem(directionOptions[2]);
            } else {
                directionSelect.setSelectedItem(directionOptions[0]);
            }
            rotationSelect.setSelectedItem(rotationOptions[0]);
        } else {
            if (effect.isUpOrSide()) {
                directionSelect.setSelectedItem(directionOptions[3]);
            } else {
                directionSelect.setSelectedItem(directionOptions[1]);
            }
            rotationSelect.setSelectedItem(rotationOptions[1]);
        }

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
                String durationFieldText;
                if (durationField.getText().contains(".")) {
                    durationFieldText = durationField.getText().substring(0, durationField.getText().indexOf("."));
                } else {
                    durationFieldText = durationField.getText();
                }
                int durationCount = Integer.parseInt(durationFieldText);

                newEndTime = timeManager.getCount2MSec().get(startCount + durationCount);
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
                durationText = durationText.substring(0, durationText.length() - 3) + "." + durationText.substring(durationText.length() - 3);
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
            if (effectType == EffectList.GRID) {
                effectMod.getShapes()[showGridIndex].setColor(color);
            } else if (button.equals(colorButtons.get(colorButtons.size() - 1))) {
                applyToEffectMod();
                effectListener.onUpdateEffectPanel(effectMod, this.isNewEffect, -1);
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
            String durationFieldText;
            if (durationField.getText().contains(".")) {
                durationFieldText = durationField.getText().substring(0, durationField.getText().indexOf("."));
            } else {
                durationFieldText = durationField.getText();
            }
            int durationCount = Integer.parseInt(durationFieldText);
            durationMSec = timeManager.getCount2MSec().get(startCount + durationCount) - effectMod.getStartTimeMSec();
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
        this.effectMod.setMaxBrightness(Float.parseFloat(maxBrightnessField.getText()));
        this.effectMod.setMinBrightness(Float.parseFloat(minBrightnessField.getText()));
        this.effectMod.setMaxTime(Long.parseLong(maxTimeField.getText()));
        this.effectMod.setMinTime(Long.parseLong(minTimeField.getText()));
        this.effectMod.setColorVariance(Float.parseFloat(colorVarianceField.getText()));

        if (effectType == EffectList.GRID && showGridIndex != -1) {
            this.effectMod.getShapes()[showGridIndex].setMovement(
                    new Point(Integer.parseInt(hMovementField.getText()),
                            Integer.parseInt(vMovementField.getText())));
            Iterator<LEDStrip> iterator = effectListener.onSelectionRequired().iterator();
            ArrayList<LEDStrip> list = new ArrayList<>();
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
            list.sort(Comparator.comparingInt((o) -> (int) o.getPerformer().currentLocation.getY()));
            // find width and height
            int i = 0;
            HashSet<Integer> xPositions = new HashSet<>();
            HashSet<Integer> yPositions = new HashSet<>();
            HashSet<Performer> performers = new HashSet<>();
            int widthOffset = 0;
            while (i < list.size()) {
                int x = (int) list.get(i).getPerformer().currentLocation.getX();
                if (xPositions.contains(x)) {
                    if (performers.contains(list.get(i).getPerformer())) {
                        widthOffset++;
                    }
                } else {
                    performers.add(list.get(i).getPerformer());
                }
                xPositions.add(x);
                yPositions.add((int) list.get(i).getPerformer().currentLocation.getY());

                i++;
            }
            effectMod.setWidth(xPositions.size() + widthOffset);
            effectMod.setHeight(yPositions.size());
        }


        this.effectMod.setUpOrSide(directionSelect.getSelectedItem().equals("Up") || directionSelect.getSelectedItem().equals("Down"));
        if (effectType == EffectList.CIRCLE_CHASE) {
            this.effectMod.setDirection(rotationSelect.getSelectedItem().equals("Clockwise"));
        } else {
            this.effectMod.setDirection(directionSelect.getSelectedItem().equals("Up") || directionSelect.getSelectedItem().equals("Right"));
        }
        if (this.effectType == EffectList.CHASE) {
            this.effectMod.setDirection(rotationSelect.getSelectedItem().equals("Clockwise"));
            ArrayList<Color> chaseSequence = new ArrayList<>();
            for (JButton button : colorButtons) {
                chaseSequence.add(button.getBackground());
            }
            this.effectMod.setChaseSequence(chaseSequence);
        }
//        if (this.effectType == EffectList.GRID) {
//            this.effectMod.getShapes()[showGridIndex].setColor();
//        }

    }

}