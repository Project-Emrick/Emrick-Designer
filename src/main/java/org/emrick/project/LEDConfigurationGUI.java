package org.emrick.project;

import org.emrick.project.actions.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.Stack;

public class LEDConfigurationGUI extends JPanel {
    private Drill drill;
    private JScrollPane scrollPane;
    private JPanel scrollablePanel;
    private JPanel bottomBar;
    private ArrayList<PerformerConfigPanel> performerConfigPanels;
    private Stack<UndoableAction> undoStack = new Stack<>();
    private Stack<UndoableAction> redoStack = new Stack<>();
    private PerformerConfig copiedConfig = null;
    private LEDConfigListener ledConfigListener;

    public LEDConfigurationGUI(Drill drill, LEDConfigListener ledConfigListener) {
        this.drill = drill;
        this.performerConfigPanels = new ArrayList<>();
        this.scrollablePanel = new JPanel();
        scrollablePanel.setBackground(new Color(0, 0, 0, 0));
        this.bottomBar = new JPanel();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.ledConfigListener = ledConfigListener;
        initializeLEDConfigPanel();
    }

    public void initializeLEDConfigPanel() {
        for (Performer p : drill.performers) {
            performerConfigPanels.add(new PerformerConfigPanel(p, drill.ledStrips));
        }
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        Border innerBorder = BorderFactory.createTitledBorder("LED View Configuration");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        scrollablePanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
        for (int i = 0; i < performerConfigPanels.size(); i++) {
            PerformerConfigPanel pc = performerConfigPanels.get(i);
            pc.initializePerformerConfigPanel();
            scrollablePanel.add(pc);
        }
        scrollPane = new JScrollPane(scrollablePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.setVisible(true);
        // this listener gets rid of some nasty visual bugs, however it also introduces some lag when using the scroll wheel
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            reinitializeLEDConfigPanel();
        });

        JButton selectAllButton = new JButton("Select All");

        JButton deselectAllButton = new JButton("Deselect All");

        selectAllButton.addActionListener(e -> {
            for (PerformerConfigPanel pc : performerConfigPanels) {
                pc.selected = true;
            }
            reinitializeLEDConfigPanel();
            deselectAllButton.setEnabled(true);
            selectAllButton.setEnabled(false);
        });

        deselectAllButton.addActionListener(e -> {
            for (PerformerConfigPanel pc : performerConfigPanels) {
                pc.selected = false;
            }
            reinitializeLEDConfigPanel();
            selectAllButton.setEnabled(true);
            deselectAllButton.setEnabled(false);

        });

        JButton pasteToSelectedButton = new JButton("Paste To Selected");
        pasteToSelectedButton.addActionListener(e -> {
            ArrayList<PerformerConfigPanel> selectedConfigPanels = new ArrayList<>();
            for (PerformerConfigPanel pc : performerConfigPanels) {
                if (pc.selected) {
                    selectedConfigPanels.add(pc);
                }
            }

            ArrayList<PerformerConfigMap> performerConfigMaps = new ArrayList<>();
            for (PerformerConfigPanel pc : selectedConfigPanels) {
                ArrayList<LEDConfig> ledConfigs = new ArrayList<>();
                for (LEDStrip l : pc.getLedStrips()) {
                    ledConfigs.add(l.getLedConfig());
                }
                performerConfigMaps.add(new PerformerConfigMap(new PerformerConfig(ledConfigs), copiedConfig, pc));
            }
            UpdateConfigsAction updateConfigsAction = new UpdateConfigsAction(performerConfigMaps, this);
            undoStack.push(updateConfigsAction);
            redoStack.clear();
            updateConfigsAction.execute();
        });

        JButton pasteToAllButton = new JButton("Paste To All");
        pasteToAllButton.addActionListener(e -> {
            ArrayList<PerformerConfigMap> performerConfigMaps = new ArrayList<>();
            for (PerformerConfigPanel pc : performerConfigPanels) {
                ArrayList<LEDConfig> ledConfigs = new ArrayList<>();
                for (LEDStrip l : pc.getLedStrips()) {
                    ledConfigs.add(l.getLedConfig());
                }
                performerConfigMaps.add(new PerformerConfigMap(new PerformerConfig(ledConfigs), copiedConfig, pc));
            }
            UpdateConfigsAction updateConfigsAction = new UpdateConfigsAction(performerConfigMaps, this);
            undoStack.push(updateConfigsAction);
            redoStack.clear();
            updateConfigsAction.execute();
        });

        JButton setSelectedToDefaultButton = new JButton("Set Selected To Default");
        setSelectedToDefaultButton.addActionListener(e -> {
            ArrayList<PerformerConfigPanel> selectedConfigPanels = new ArrayList<>();
            for (PerformerConfigPanel pc : performerConfigPanels) {
                if (pc.selected) {
                    selectedConfigPanels.add(pc);
                }
            }

            ArrayList<LEDConfig> defaultLEDConfigs = new ArrayList<>();
            defaultLEDConfigs.add(new LEDConfig(50, 12, 6, -6, -6, "L"));
            defaultLEDConfigs.add(new LEDConfig(50, 12, 6, 1, -6, "R"));
            PerformerConfig defaultConfig = new PerformerConfig(defaultLEDConfigs);

            ArrayList<PerformerConfigMap> performerConfigMaps = new ArrayList<>();
            for (PerformerConfigPanel pc : selectedConfigPanels) {
                ArrayList<LEDConfig> ledConfigs = new ArrayList<>();
                for (LEDStrip l : pc.getLedStrips()) {
                    ledConfigs.add(l.getLedConfig());
                }
                performerConfigMaps.add(new PerformerConfigMap(new PerformerConfig(ledConfigs), defaultConfig, pc));
            }
            UpdateConfigsAction updateConfigsAction = new UpdateConfigsAction(performerConfigMaps, this);
            undoStack.push(updateConfigsAction);
            redoStack.clear();
            updateConfigsAction.execute();
        });

        JButton setAllToDefaultButton = new JButton("Set All to Default");
        setAllToDefaultButton.addActionListener(e -> {
            ArrayList<LEDConfig> defaultLEDConfigs = new ArrayList<>();
            defaultLEDConfigs.add(new LEDConfig(50, 12, 6, -6, -6, "L"));
            defaultLEDConfigs.add(new LEDConfig(50, 12, 6, 1, -6, "R"));
            PerformerConfig defaultConfig = new PerformerConfig(defaultLEDConfigs);

            ArrayList<PerformerConfigMap> performerConfigMaps = new ArrayList<>();
            for (PerformerConfigPanel pc : performerConfigPanels) {
                ArrayList<LEDConfig> ledConfigs = new ArrayList<>();
                for (LEDStrip l : pc.getLedStrips()) {
                    ledConfigs.add(l.getLedConfig());
                }
                performerConfigMaps.add(new PerformerConfigMap(new PerformerConfig(ledConfigs), defaultConfig, pc));
            }
            UpdateConfigsAction updateConfigsAction = new UpdateConfigsAction(performerConfigMaps, this);
            undoStack.push(updateConfigsAction);
            redoStack.clear();
            updateConfigsAction.execute();
        });

        JButton exitConfigButton = new JButton("Exit");
        exitConfigButton.addActionListener(e -> ledConfigListener.onExitConfig());


        bottomBar.setLayout(new BoxLayout(bottomBar, BoxLayout.X_AXIS));

        bottomBar.add(selectAllButton);
        bottomBar.add(deselectAllButton);
        bottomBar.add(pasteToSelectedButton);
        bottomBar.add(pasteToAllButton);
        bottomBar.add(setSelectedToDefaultButton);
        bottomBar.add(setAllToDefaultButton);
        bottomBar.add(exitConfigButton);

        bottomBar.setPreferredSize(new Dimension(10000, 50));
        bottomBar.setMinimumSize(new Dimension(600, 50));

        this.add(scrollPane);
        this.add(bottomBar);
        this.setVisible(true);
    }
    public void reinitializeLEDConfigPanel() {
        scrollablePanel.removeAll();
        ArrayList<PerformerConfigPanel> newPerformerConfigPanels = new ArrayList<>();
        for (PerformerConfigPanel pc : performerConfigPanels) {
            PerformerConfigPanel newPC = new PerformerConfigPanel(pc.getPerformer(), drill.ledStrips);
            newPC.setSelected(pc.isSelected());
            newPC.setShowLEDs(pc.isShowLEDs());
            newPC.setPreferredSize(pc.getPreferredSize());
            newPC.setMinimumSize(pc.getMinimumSize());
            newPC.setMaximumSize(pc.getMaximumSize());
            newPC.initializePerformerConfigPanel();
            newPerformerConfigPanels.add(newPC);
            scrollablePanel.add(newPC);
        }
        performerConfigPanels = newPerformerConfigPanels;
        scrollPane.revalidate();
        scrollPane.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoableAction undo = undoStack.pop();
            redoStack.push(undo);
            undo.undo();
            reinitializeLEDConfigPanel();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            UndoableAction redo = redoStack.pop();
            undoStack.push(redo);
            redo.redo();
            reinitializeLEDConfigPanel();
        }
    }

    public Drill getDrill() {
        return drill;
    }

    public void setDrill(Drill drill) {
        this.drill = drill;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public void setScrollPane(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
    }

    public ArrayList<PerformerConfigPanel> getPerformerConfigsPanels() {
        return performerConfigPanels;
    }

    public void setPerformerConfigPanels(ArrayList<PerformerConfigPanel> performerConfigPanels) {
        this.performerConfigPanels = performerConfigPanels;
    }

    public int getLastID() {
        ArrayList<Integer> ids = drill.performers.get(drill.performers.size()-1).getLedStrips();
        return ids.get(ids.size()-1);
    }

    public int getPrevID(int id) {
        Performer prev = drill.performers.get(id - 1);
        if (!prev.getLedStrips().isEmpty()) {
            return prev.getLedStrips().get(prev.getLedStrips().size() - 1)+1;
        } else {
            if (id > 1) {
                return getPrevID(id - 1);
            } else {
                return 0;
            }
        }
    }

    public void addLEDStrip(LEDStrip ledStrip) {
        boolean incrementing = false;
        int id = ledStrip.getId();
        Performer performerMod = null;
        for (Performer p : drill.performers) {
            if (p.equals(ledStrip.getPerformer())) {
                for (int j = 0; j < p.getLedStrips().size(); j++) {
                    Integer i = p.getLedStrips().get(j);
                    if (i > id) {
                        p.getLedStrips().set(j, p.getLedStrips().get(j) + 1);
                    }
                }
                p.addLEDStrip(ledStrip.getId());
                performerMod = p;
            }
        }
        if (performerMod == null) {
            return;
        }
        drill.ledStrips.add(ledStrip.getId(), ledStrip);
        for (Performer p : drill.performers) {
            for (int j = 0; j < p.getLedStrips().size(); j++) {
                if (!p.equals(performerMod)) {
                    Integer i = p.getLedStrips().get(j);
                    if (i >= id) {
                        LEDStrip l = drill.ledStrips.get(i+1);
                        l.setId(i + 1);
                        p.getLedStrips().set(j, i + 1);
                    }
                }
            }
        }
    }

    public void removeLEDStrip(LEDStrip ledStrip) {
        for (int i = drill.performers.size()-1; i >= 0; i--) {

            Performer p = drill.performers.get(i);
            boolean found = false;
            for (int j = p.getLedStrips().size() - 1; j >= 0; j--) {
                try {
                    LEDStrip l = drill.ledStrips.get(p.getLedStrips().get(j));
                    if (l.getId() == ledStrip.getId()) {
                        p.getLedStrips().remove(j);
                        found = true;
                        j--;
                        break;
                    } else if (l.getId() >= ledStrip.getId()) {
                        l.setId(p.getLedStrips().get(j) - 1);
                        p.getLedStrips().set(j, l.getId());
                    }
                } catch (IndexOutOfBoundsException e) {
                    if (ledStrip.getId() == 0) {
                        p.getLedStrips().remove(j);
                        j--;
                    }
                }
            }
            if (found) {
                break;
            }
        }
        drill.ledStrips.remove(ledStrip);

    }

    public class PerformerConfigPanel extends JPanel {

        private Performer performer;
        private JPanel performerPanel;
        private ArrayList<LEDStrip> ledStrips;
        private boolean showLEDs = false;
        public boolean selected = false;
        private int edit = -1;

        public PerformerConfigPanel(Performer performer, ArrayList<LEDStrip> ledStrips) {
            this.performer = performer;
            this.performerPanel = new JPanel();
            performerPanel.setLayout(new BoxLayout(performerPanel, BoxLayout.X_AXIS));
            performerPanel.setPreferredSize(new Dimension(10000, 60));
            performerPanel.setMaximumSize(new Dimension(10000, 60));
            this.setOpaque(true);
            this.ledStrips = new ArrayList<>();
            for (LEDStrip l : ledStrips) {
                if (performer.getLedStrips().contains(l.getId())) {
                    this.ledStrips.add(l);
                }
            }
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setPreferredSize(new Dimension(10000, 60));
            this.setMinimumSize(new Dimension(600, 60));
            this.setMaximumSize(new Dimension(10000, 60));
            this.setOpaque(true);
        }

        public void initializePerformerConfigPanel() {
            JCheckBox selectBox = new JCheckBox();
            selectBox.setOpaque(false);
            selectBox.setSelected(selected);
            selectBox.addActionListener(e -> {
                selected = !selected;
                reinitializeLEDConfigPanel();
            });
            if (selected) {
                performerPanel.setBackground(Color.BLUE);
                this.setBackground(Color.BLUE);
            } else {
                this.setBackground(new Color(0xEEEEEE));
                performerPanel.setBackground(new Color(0xEEEEEE));
            }

            JLabel performerLabel = new JLabel(performer.getIdentifier());
            performerLabel.setOpaque(false);

            JButton showLedsBtn = new JButton("Show LEDs");
            showLedsBtn.addActionListener(e -> {
                showLEDs = !showLEDs;
                reinitializeLEDConfigPanel();
            });

            JButton hideLedsBtn = new JButton("Hide LEDs");
            hideLedsBtn.addActionListener(e -> {
                showLEDs = !showLEDs;
                reinitializeLEDConfigPanel();
            });

            JButton copyConfigBtn = new JButton("Copy Config");
            copyConfigBtn.addActionListener(e -> {
                ArrayList<LEDConfig> configs = new ArrayList<>();
                for (LEDStrip l : ledStrips) {
                    configs.add(l.getLedConfig());
                }
                copiedConfig = new PerformerConfig(configs);
            });

            DrawLEDs drawLEDs = new DrawLEDs(ledStrips);

            performerPanel.add(selectBox);
            performerPanel.add(performerLabel);
            if (!showLEDs) {
                this.setPreferredSize(new Dimension(800, 60));
                this.setMinimumSize(new Dimension(600, 60));
                this.setMaximumSize(new Dimension(800, 60));
                performerPanel.add(showLedsBtn);
            } else {
                this.setPreferredSize(new Dimension(10000, 85 * ledStrips.size()+60));
                this.setMinimumSize(new Dimension(600, 85 * ledStrips.size()+60));
                this.setMaximumSize(new Dimension(10000, 85 * ledStrips.size()+60));
                performerPanel.add(hideLedsBtn);
            }
            performerPanel.add(copyConfigBtn);
            if (!ledStrips.isEmpty()) {
                performerPanel.add(drawLEDs);
            }

            this.add(performerPanel);

            if (showLEDs) {
                for (int y = 0; y < ledStrips.size(); y++) {
                    LEDStrip ledStrip = ledStrips.get(y);
                    JPanel ledStripPanel = new JPanel();
                    ledStripPanel.setLayout(new BoxLayout(ledStripPanel, BoxLayout.X_AXIS));
                    ledStripPanel.setPreferredSize(new Dimension(10000, 60));
                    ledStripPanel.setMaximumSize(new Dimension(10000, 60));
                    ledStripPanel.setOpaque(false);
                    ArrayList<LEDStrip> load1Strip = new ArrayList<>();
                    load1Strip.add(ledStrip);
                    DrawLEDs d1 = new DrawLEDs(load1Strip);

                    JLabel ledLabel = new JLabel("Label: " + performer.getIdentifier());

                    JLabel idLabel = new JLabel("ID: " + ledStrip.getId());

                    JTextField labelField = new JTextField(ledLabel.getText());
                    labelField.setEditable(true);
                    labelField.setText(ledStrip.getLedConfig().getLabel());
                    labelField.setPreferredSize(new Dimension(40, 20));
                    labelField.setMaximumSize(new Dimension(40, 20));

                    JLabel ledCountLabel = new JLabel("LED Count: ");

                    JTextField ledCountField = new JTextField(3);
                    ledCountField.setEditable(true);
                    ledCountField.setText(Integer.toString(ledStrip.getLedConfig().getLEDCount()));
                    ledCountField.setPreferredSize(new Dimension(40, 20));
                    ledCountField.setMaximumSize(new Dimension(40, 20));

                    JLabel heightLabel = new JLabel("Height: ");
                    JTextField heightField = new JTextField(3);
                    heightField.setEditable(true);
                    heightField.setText(Integer.toString(ledStrip.getLedConfig().getHeight()));
                    heightField.setPreferredSize(new Dimension(40, 20));
                    heightField.setMaximumSize(new Dimension(40, 20));

                    JLabel widthLabel = new JLabel("Width: ");
                    JTextField widthField = new JTextField(3);
                    widthField.setEditable(true);
                    widthField.setText(Integer.toString(ledStrip.getLedConfig().getWidth()));
                    widthField.setPreferredSize(new Dimension(40, 20));
                    widthField.setMaximumSize(new Dimension(40, 20));

                    JLabel hOffsetLabel = new JLabel("Horizontal Offset: ");
                    JTextField hOffsetField = new JTextField(3);
                    hOffsetField.setEditable(true);
                    hOffsetField.setText(Integer.toString(ledStrip.getLedConfig().gethOffset()));
                    hOffsetField.setPreferredSize(new Dimension(40, 20));
                    hOffsetField.setMaximumSize(new Dimension(40, 20));

                    JLabel vOffsetLabel = new JLabel("Vertical Offset: ");
                    JTextField vOffsetField = new JTextField(3);
                    vOffsetField.setEditable(true);
                    vOffsetField.setText(Integer.toString(ledStrip.getLedConfig().getvOffset()));
                    vOffsetField.setPreferredSize(new Dimension(40, 20));
                    vOffsetField.setMaximumSize(new Dimension(40, 20));

                    JButton applyButton = new JButton("Apply");
                    applyButton.addActionListener(e -> {
                        LEDConfig newConfig = new LEDConfig();
                        newConfig.setLabel(labelField.getText());
                        newConfig.setLEDCount(Integer.parseInt(ledCountField.getText()));
                        newConfig.setHeight(Integer.parseInt(heightField.getText()));
                        newConfig.setWidth(Integer.parseInt(widthField.getText()));
                        newConfig.sethOffset(Integer.parseInt(hOffsetField.getText()));
                        newConfig.setvOffset(Integer.parseInt(vOffsetField.getText()));
                        LEDConfig oldConfig = ledStrip.getLedConfig();
                        ArrayList<LEDConfigLEDStripMap> LEDConfigLEDStripMaps = new ArrayList<>();
                        LEDConfigLEDStripMaps.add(new LEDConfigLEDStripMap(ledStrip, newConfig, oldConfig));
                        UpdateConfigAction updateConfigAction = new UpdateConfigAction(LEDConfigLEDStripMaps);
                        undoStack.push(updateConfigAction);
                        redoStack.clear();
                        updateConfigAction.execute();

                        reinitializeLEDConfigPanel();
                    });

                    JButton deleteButton = new JButton("Delete");
                    deleteButton.addActionListener(e -> {
                        delete(ledStrip);
                    });


                    ledStripPanel.add(d1);
                    ledStripPanel.add(ledLabel);
                    ledStripPanel.add(labelField);
                    ledStripPanel.add(idLabel);
                    ledStripPanel.add(ledCountLabel);
                    ledStripPanel.add(ledCountField);
                    ledStripPanel.add(heightLabel);
                    ledStripPanel.add(heightField);
                    ledStripPanel.add(widthLabel);
                    ledStripPanel.add(widthField);
                    ledStripPanel.add(hOffsetLabel);
                    ledStripPanel.add(hOffsetField);
                    ledStripPanel.add(vOffsetLabel);
                    ledStripPanel.add(vOffsetField);
                    ledStripPanel.add(applyButton);
                    ledStripPanel.add(deleteButton);

                    this.add(ledStripPanel);

                }
                JPanel addButtonPanel = new JPanel();
                addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.X_AXIS));
                addButtonPanel.setPreferredSize(new Dimension(10000, 60));
                addButtonPanel.setMaximumSize(new Dimension(10000, 60));
                addButtonPanel.setOpaque(false);

                JButton addButton = new JButton("Add");
                addButton.addActionListener(e -> {
                    ArrayList<PerformerConfigMap> performerConfigMaps = new ArrayList<>();
                    ArrayList<LEDConfig> addConfigs = new ArrayList<>();
                    for (LEDStrip l : ledStrips) {
                        addConfigs.add(l.getLedConfig());
                    }

                    addConfigs.add(new LEDConfig());
                    PerformerConfig addPerformerConfig = new PerformerConfig(addConfigs);

                    ArrayList<LEDConfig> oldConfigs = new ArrayList<>();
                    for (LEDStrip l : ledStrips) {
                        oldConfigs.add(l.getLedConfig());
                    }
                    PerformerConfig oldPerformerConfig = new PerformerConfig(oldConfigs);
                    performerConfigMaps.add(new PerformerConfigMap(oldPerformerConfig, addPerformerConfig, this));
                    UpdateConfigsAction updateConfigsAction = new UpdateConfigsAction(performerConfigMaps, null);
                    undoStack.push(updateConfigsAction);
                    redoStack.clear();
                    updateConfigsAction.execute();

                    reinitializeLEDConfigPanel();
                });

                addButtonPanel.add(addButton);
                addButtonPanel.setPreferredSize(new Dimension(100, 50));

                this.add(addButtonPanel);
            } else {
                this.setPreferredSize(new Dimension(10000, 60));
                this.setMaximumSize(new Dimension(10000, 60));
            }
        }

        public void delete(LEDStrip ledStrip) {
            ArrayList<PerformerConfigMap> performerConfigMaps = new ArrayList<>();
            ArrayList<LEDConfig> deleteConfigs = new ArrayList<>();
            for (LEDStrip l : ledStrips) {
                if (!l.equals(ledStrip)) {
                    deleteConfigs.add(l.getLedConfig());
                }
            }
            PerformerConfig deletePerformerConfig = new PerformerConfig(deleteConfigs);

            ArrayList<LEDConfig> oldConfigs = new ArrayList<>();
            for (LEDStrip l : ledStrips) {
                oldConfigs.add(l.getLedConfig());
            }
            PerformerConfig oldPerformerConfig = new PerformerConfig(oldConfigs);
            performerConfigMaps.add(new PerformerConfigMap(oldPerformerConfig, deletePerformerConfig, this));
            UpdateConfigsAction updateConfigsAction = new UpdateConfigsAction(performerConfigMaps, null);
            undoStack.push(updateConfigsAction);
            redoStack.clear();
            updateConfigsAction.execute();

            reinitializeLEDConfigPanel();
        }

        public void pasteConfig(PerformerConfig performerConfig) {

            if (performerConfig != null) {
                int i;
                for (i = 0; i < Math.min(ledStrips.size(), performerConfig.getLedConfigs().size()); i++) {
                    ledStrips.get(i).setLedConfig(performerConfig.getLedConfigs().get(i));
                }

                while (ledStrips.size() > performerConfig.getLedConfigs().size()) {
                    removeLEDStrip(ledStrips.get(i));
                    ledStrips.remove(ledStrips.get(i));
                }

                while (ledStrips.size() < performerConfig.getLedConfigs().size()) {
                    LEDStrip newLEDStrip;

                    if (ledStrips.isEmpty()) {
                        if (performer.getPerformerID() == 0) {
                            newLEDStrip = new LEDStrip(0, performer, performerConfig.getLedConfigs().get(i));
                        } else {
                            int id = getPrevID(performer.getPerformerID());
                            newLEDStrip = new LEDStrip(id, performer, performerConfig.getLedConfigs().get(i));
                        }
                    } else {
                        newLEDStrip = new LEDStrip(performer.getLedStrips().get(performer.getLedStrips().size()-1)+1, performer, performerConfig.getLedConfigs().get(i));
                    }

                    addLEDStrip(newLEDStrip);
                    ledStrips.add(newLEDStrip);
                    i++;
                }
            }
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isShowLEDs() {
            return showLEDs;
        }

        public void setShowLEDs(boolean showLEDs) {
            this.showLEDs = showLEDs;
        }

        public Performer getPerformer() {
            return performer;
        }

        public void setPerformer(Performer performer) {
            this.performer = performer;
        }

        public ArrayList<LEDStrip> getLedStrips() {
            return ledStrips;
        }

        public void setLedStrips(ArrayList<LEDStrip> ledStrips) {
            this.ledStrips = ledStrips;
        }

        public boolean isSelected() {
            return selected;
        }

        private class DrawLEDs extends JPanel {
            private ArrayList<LEDStrip> ledStrips;

            public DrawLEDs(ArrayList<LEDStrip> ledStrips) {
                this.ledStrips = ledStrips;
                this.setPreferredSize(new Dimension(50, 50));
                this.setMinimumSize(new Dimension(50,50));
                this.setMaximumSize(new Dimension(50,50));
                this.setOpaque(false);
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (LEDStrip l : ledStrips) {
                    g.setColor(Color.white);
                    g.fillRect(25 + l.getLedConfig().gethOffset(), 25 + l.getLedConfig().getvOffset(), l.getLedConfig().getWidth(), l.getLedConfig().getHeight());

                    g.setColor(Color.BLACK);
                    g.drawRect(25 + l.getLedConfig().gethOffset(), 25 + l.getLedConfig().getvOffset(), l.getLedConfig().getWidth(), l.getLedConfig().getHeight());
                }
            }
        }
    }
}
