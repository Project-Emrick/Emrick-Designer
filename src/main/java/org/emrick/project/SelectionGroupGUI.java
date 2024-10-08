package org.emrick.project;

import org.emrick.project.effect.Effect;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class SelectionGroupGUI implements ActionListener {
    private ArrayList<SelectionGroup> groups;
    private JPanel selectionPanel;
    private SelectListener selectListener;
    private boolean controlHeld = false;
    public SelectionGroupGUI(SelectListener selectListener) {
        groups = new ArrayList<>();
        selectionPanel = null;
        this.selectListener = selectListener;
    }

    public SelectionGroupGUI(ArrayList<SelectionGroup> groups, SelectListener selectListener) {
        this.groups = groups;
        selectionPanel = null;
        this.selectListener = selectListener;
    }

    public JPanel getSelectionPanel() {

        return selectionPanel;
    }

    public void setSelectionPanel(JPanel panel) {
        this.selectionPanel = panel;
    }

    public void initializeSelectionPanel() {
        selectionPanel = new JPanel();

        Border innerBorder = BorderFactory.createTitledBorder("Save Groups");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        selectionPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        selectionPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        Insets spacedInsets = new Insets(0, 0, 5, 0);
        Insets noSpacedInsets = new Insets(0, 0, 0, 0);
        Insets separatorInsets = new Insets(0,0,30,0);

        JLabel titleFieldLabel = new JLabel("Group Name: ");

        gc.weightx = 0;
        gc.weighty = 0;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = spacedInsets;
        selectionPanel.add(titleFieldLabel, gc);

        JTextField titleField = new JTextField();

        gc.weightx = 1;
        gc.weighty = 0;

        gc.gridx = 1;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = spacedInsets;
        selectionPanel.add(titleField, gc);

        JButton saveGroupButton = new JButton("Save Group");
        saveGroupButton.addActionListener(e -> {
            groups.add(new SelectionGroup(selectListener.onSaveGroup(), titleField.getText(), this));
            selectListener.onUpdateGroup();
        });

        gc.weightx = 1;
        if (groups.size() == 0) {
            gc.weighty = 1;
        } else {
            gc.weighty = 0;
        }

        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = separatorInsets;
        selectionPanel.add(saveGroupButton, gc);

        for (int i = 0; i < groups.size(); i++) {
            SelectionGroup group = groups.get(i);
            gc.weightx = 1;
            if (i == groups.size()-1) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }

            gc.gridx = 0;
            gc.gridy = i+2;
            gc.fill = GridBagConstraints.NONE;
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = spacedInsets;
            selectionPanel.add(groups.get(i).getTitleButton(), gc);

            JButton deleteGroup = new JButton("Delete Group");
            deleteGroup.addActionListener(e -> {
                groups.remove(group);
                selectListener.onUpdateGroup();
            });

            gc.weightx = 1;
            if (i == groups.size()-1) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }

            gc.gridx = 1;
            gc.gridy = i+2;
            gc.fill = GridBagConstraints.NONE;
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = spacedInsets;
            selectionPanel.add(deleteGroup, gc);

        }
    }

    public void addGroup(LEDStrip[] group, String title) {
        groups.add(new SelectionGroup(group, title, this));
    }

    public ArrayList<SelectionGroup> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<SelectionGroup> groups, ArrayList<LEDStrip> ledStrips) {
        this.groups = groups;
        for (SelectionGroup group : groups) {
            ArrayList<LEDStrip> copyList = new ArrayList<>();
            for (LEDStrip ledStrip : group.getLEDStrips()) {
                for (LEDStrip l : ledStrips) {
                    if (ledStrip.equals(l)) {
                        copyList.add(l);
                    }
                }
            }
            for (int i = 0; i < group.getLEDStrips().length; i++) {
                group.getLEDStrips()[i] = copyList.get(i);
            }
        }
    }

    public void initializeButtons(){
        for (SelectionGroup group : groups) {
            group.setTitleButton(new JButton(group.getTitle()));
            group.getTitleButton().addActionListener(this);
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        for (SelectionGroup group : groups) {
            if (group.getTitleButton().equals(e.getSource())) {
                if((e.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
                    selectListener.ctrlGroupSelection(group.ledStrips);
                }
                else{
                    selectListener.onGroupSelection(group.ledStrips);
                }
            }
        }
    }

    public class SelectionGroup {
        private LEDStrip[] ledStrips;
        private String title;
        private JButton titleButton;
        public SelectionGroup(LEDStrip[] ledStrips, String title, ActionListener actionListener) {
            this.ledStrips = ledStrips;
            this.title = title;
            this.titleButton = new JButton(title);
            titleButton.addActionListener(actionListener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SelectionGroup that = (SelectionGroup) o;
            return Arrays.equals(ledStrips, that.ledStrips) && Objects.equals(title, that.title);
        }


        public JButton getTitleButton() {
            return titleButton;
        }

        public LEDStrip[] getLEDStrips() {
            return ledStrips;
        }

        public void setLEDStrips(LEDStrip[] ledStrips) {
            this.ledStrips = ledStrips;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public SelectionGroup clone() {
            return new SelectionGroup(ledStrips,title, titleButton.getActionListeners()[0]);
        }

        public void setTitleButton(JButton titleButton) {
            this.titleButton = titleButton;
        }
    }
}
