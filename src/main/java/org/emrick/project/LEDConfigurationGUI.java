package org.emrick.project;

import org.emrick.project.actions.ConfigLEDStripMap;
import org.emrick.project.actions.LEDConfig;
import org.emrick.project.actions.UndoableAction;
import org.emrick.project.actions.UpdateConfigsAction;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.Stack;

public class LEDConfigurationGUI extends JPanel {
    private Drill drill;
    private JScrollPane scrollPane;
    private ArrayList<PerformerConfigPanel> performerConfigPanels;
    private BoxLayout boxLayout;
    private Stack<UndoableAction> undoStack = new Stack<>();
    private Stack<UndoableAction> redoStack = new Stack<>();
    private PerformerConfig copiedConfig = null;

    public LEDConfigurationGUI(Drill drill) {
        this.drill = drill;
        this.performerConfigPanels = new ArrayList<>();
        this.setBackground(new Color(0, 0, 0, 0));
        this.boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        initializeLEDConfigPanel();
    }

    public void initializeLEDConfigPanel() {
        for (Performer p : drill.performers) {
            performerConfigPanels.add(new PerformerConfigPanel(p, drill.ledStrips));
        }
        this.setLayout(boxLayout);
        Border innerBorder = BorderFactory.createTitledBorder("LED View Configuration");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        this.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
        for (int i = 0; i < performerConfigPanels.size(); i++) {
            PerformerConfigPanel pc = performerConfigPanels.get(i);
            pc.initializePerformerConfigPanel();
            this.add(pc);
        }
        scrollPane = new JScrollPane(this);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.setVisible(true);
    }
    public void reinitializeLEDConfigPanel() {
        for (PerformerConfigPanel pc : performerConfigPanels) {
            pc.initializePerformerConfigPanel();
        }
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
                    }
                }
            }
        }
    }

    public void removeLEDStrip(LEDStrip ledStrip) {
        drill.ledStrips.remove(ledStrip);
        for (int i = drill.performers.size()-1; i >= 0; i--) {
            Performer p = drill.performers.get(i);
            boolean found = false;
            for (int j = 0; j < p.getLedStrips().size(); j++) {
                try {
                    LEDStrip l = drill.ledStrips.get(p.getLedStrips().get(j) - 1);
                    System.out.println(l.getId());
                    if (l.getId() <= ledStrip.getId()) {
                        p.getLedStrips().remove(j);
                        j--;
                        found = true;
                        break;
                    } else if (l.getId() >= ledStrip.getId()) {
                        l.setId(p.getLedStrips().get(j) - 1);
                        p.getLedStrips().set(j, l.getId());
                    }
                } catch (IndexOutOfBoundsException e) {
                    if (ledStrip.getId() == 0) {
                        p.getLedStrips().remove(j);
                    }
                }
            }
            if (found) {
                break;
            }
        }
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("LED Configuration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,500);
        Drill drill = new Drill();
        drill.performers.add(new Performer("a", 1, 0));
        drill.ledStrips.add(new LEDStrip(0, drill.performers.get(0), new LEDConfig()));
        drill.ledStrips.add(new LEDStrip(1, drill.performers.get(0), new LEDConfig()));
        drill.ledStrips.get(1).getLedConfig().sethOffset(1);
        drill.performers.add(new Performer("a", 2, 1));
        drill.ledStrips.add(new LEDStrip(2, drill.performers.get(1), new LEDConfig()));
        drill.ledStrips.add(new LEDStrip(3, drill.performers.get(1), new LEDConfig()));
        drill.ledStrips.get(3).getLedConfig().sethOffset(1);
        drill.performers.add(new Performer("a", 3, 2));
        drill.ledStrips.add(new LEDStrip(4, drill.performers.get(2), new LEDConfig()));
        drill.ledStrips.add(new LEDStrip(5, drill.performers.get(2), new LEDConfig()));
        drill.ledStrips.get(5).getLedConfig().sethOffset(1);
        drill.performers.add(new Performer("a", 4, 3));
        drill.ledStrips.add(new LEDStrip(6, drill.performers.get(3), new LEDConfig()));
        drill.ledStrips.add(new LEDStrip(7, drill.performers.get(3), new LEDConfig()));
        drill.ledStrips.get(7).getLedConfig().sethOffset(1);
        drill.performers.add(new Performer("a", 5, 4));
        drill.ledStrips.add(new LEDStrip(8, drill.performers.get(4), new LEDConfig()));
        drill.ledStrips.get(0).getLedConfig().setLabel("L");
        drill.ledStrips.get(1).getLedConfig().setLabel("R");
        drill.ledStrips.get(2).getLedConfig().setLabel("L");
        drill.ledStrips.get(3).getLedConfig().setLabel("R");
        drill.ledStrips.get(4).getLedConfig().setLabel("L");
        drill.ledStrips.get(5).getLedConfig().setLabel("R");
        drill.ledStrips.get(6).getLedConfig().setLabel("L");
        drill.ledStrips.get(7).getLedConfig().setLabel("R");
        drill.ledStrips.get(8).getLedConfig().setLabel("L");
        drill.performers.get(0).addLEDStrip(0);
        drill.performers.get(0).addLEDStrip(1);
        drill.performers.get(1).addLEDStrip(2);
        drill.performers.get(1).addLEDStrip(3);
        drill.performers.get(2).addLEDStrip(4);
        drill.performers.get(2).addLEDStrip(5);
        drill.performers.get(3).addLEDStrip(6);
        drill.performers.get(3).addLEDStrip(7);
        drill.performers.get(4).addLEDStrip(8);
        LEDConfigurationGUI ledConfigurationGUI = new LEDConfigurationGUI(drill);
        frame.add(ledConfigurationGUI.getScrollPane());
        frame.setVisible(true);
    }

    public class PerformerConfigPanel extends JPanel {

        private Performer performer;
        private JPanel performerPanel;
        private ArrayList<LEDStrip> ledStrips;
        private boolean showLEDs = false;
        private boolean selected = false;
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
            this.removeAll();
            performerPanel.removeAll();
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
                this.setPreferredSize(new Dimension(10000, 85 * ledStrips.size()));
                this.setMinimumSize(new Dimension(600, 85 * ledStrips.size()));
                this.setMaximumSize(new Dimension(10000, 85 * ledStrips.size()));
                reinitializeLEDConfigPanel();
            });

            JButton hideLedsBtn = new JButton("Hide LEDs");
            hideLedsBtn.addActionListener(e -> {
                showLEDs = !showLEDs;
                this.setPreferredSize(new Dimension(800, 60));
                this.setMinimumSize(new Dimension(600, 85 * ledStrips.size()));
                this.setMaximumSize(new Dimension(800, 60));
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

            JButton pasteConfigBtn = new JButton("Paste Config");
            pasteConfigBtn.addActionListener(e -> {
                pasteConfig();
            });

            JButton defaultConfigBtn = new JButton("Default Config");
            defaultConfigBtn.addActionListener(e -> {
                PerformerConfig tmp = copiedConfig;
                ArrayList<LEDConfig> configs = new ArrayList<>();
                configs.add(new LEDConfig());
                configs.add(new LEDConfig());
                configs.get(1).setLabel("R");
                configs.get(1).sethOffset(1);
                copiedConfig = new PerformerConfig(configs);
                pasteConfig();
                copiedConfig = tmp;
            });

            DrawLEDs drawLEDs = new DrawLEDs(ledStrips);

            performerPanel.add(selectBox);
            performerPanel.add(performerLabel);
            if (!showLEDs) {
                performerPanel.add(showLedsBtn);
            } else {
                performerPanel.add(hideLedsBtn);
            }
            performerPanel.add(copyConfigBtn);
            performerPanel.add(pasteConfigBtn);
            performerPanel.add(defaultConfigBtn);
            performerPanel.add(drawLEDs);

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
                        ArrayList<ConfigLEDStripMap> configLEDStripMaps = new ArrayList<>();
                        configLEDStripMaps.add(new ConfigLEDStripMap(ledStrip, newConfig, oldConfig));
                        UpdateConfigsAction updateConfigsAction = new UpdateConfigsAction(configLEDStripMaps);
                        undoStack.push(updateConfigsAction);
                        redoStack.clear();
                        updateConfigsAction.execute();

                        reinitializeLEDConfigPanel();
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

                    this.add(ledStripPanel);
                }
            } else {
                this.setPreferredSize(new Dimension(10000, 60));
                this.setMaximumSize(new Dimension(10000, 60));
            }
        }

        public void pasteConfig() {
            // TODO: Make this undoable

            if (copiedConfig != null) {
                int i;
                for (i = 0; i < Math.min(ledStrips.size(), copiedConfig.getLedConfigs().size()); i++) {
                    ledStrips.get(i).setLedConfig(copiedConfig.getLedConfigs().get(i));
                }

                while (ledStrips.size() > copiedConfig.getLedConfigs().size()) {
                    removeLEDStrip(ledStrips.get(i));
                    ledStrips.remove(ledStrips.get(i));
                }

                while (ledStrips.size() < copiedConfig.getLedConfigs().size()) {
                    LEDStrip newLEDStrip = new LEDStrip(performer.getLedStrips().get(performer.getLedStrips().size()-1)+1, performer, copiedConfig.getLedConfigs().get(i));
                    ledStrips.add(newLEDStrip);
                    addLEDStrip(newLEDStrip);
                    i++;
                }


                if (showLEDs) {
                    this.setPreferredSize(new Dimension(10000, 85 * ledStrips.size()));
                    this.setMinimumSize(new Dimension(600, 85 * ledStrips.size()));
                    this.setMaximumSize(new Dimension(10000, 85 * ledStrips.size()));
                    System.out.println(ledStrips.size());
                }
                reinitializeLEDConfigPanel();
            }
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
