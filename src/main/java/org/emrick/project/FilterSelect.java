package org.emrick.project;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class FilterSelect {

    private final JDialog frame;
//    private final JTextField selectLabelButton;
//    private final JTextField selectSymbolButton;
    private final ArrayList<JComboBox> selectLabelComboBoxes;
    private final ArrayList<JComboBox> selectSymbolComboBoxes;

    public FilterSelect(JFrame parent, HashSet<Integer> labels, HashSet<String> symbols) {
        this.frame = new JDialog(parent, true);
        this.frame.setTitle("Select by criteria");
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.frame.setSize(300, 400);
        this.frame.setLocationRelativeTo(null); // center on screen
        this.frame.setResizable(false); // resize window option

        this.selectLabelComboBoxes = new ArrayList<>();
        this.selectSymbolComboBoxes = new ArrayList<>();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // By Label
        JPanel labelPanel = new JPanel();
        JLabel selectLabelLabel = new JLabel("Select by Label");
        JButton selectLabelAddButton = new JButton("+");
        JPanel labelEntryPanel = new JPanel();
        labelEntryPanel.setLayout(new BoxLayout(labelEntryPanel, BoxLayout.Y_AXIS));
        selectLabelAddButton.addActionListener(e -> {
            JComboBox<Integer> c = new JComboBox<>();
            for (Integer l : labels) {
                c.addItem(l);
            }
            labelEntryPanel.add(c);
            selectLabelComboBoxes.add(c);
            panel.revalidate();
            panel.repaint();
        });
        labelPanel.add(selectLabelLabel);
        labelPanel.add(selectLabelAddButton);

        // By Symbol
        JPanel symbolPanel = new JPanel();
        JLabel selectSymbolLabel = new JLabel("Select by Symbol");
        JButton selectSymbolAddButton = new JButton("+");
        JPanel symbolEntryPanel = new JPanel();
        symbolEntryPanel.setLayout(new BoxLayout(symbolEntryPanel, BoxLayout.Y_AXIS));
        selectSymbolAddButton.addActionListener(e -> {
            JComboBox<String> c = new JComboBox<>();
            for (String s : symbols) {
                c.addItem(s);
            }
            symbolEntryPanel.add(c);
            selectSymbolComboBoxes.add(c);
            panel.revalidate();
            panel.repaint();
        });
        symbolPanel.add(selectSymbolLabel);
        symbolPanel.add(selectSymbolAddButton);

        panel.add(labelPanel);
        panel.add(labelEntryPanel);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
        panel.add(symbolPanel);
        panel.add(symbolEntryPanel);
        panel.add(Box.createRigidArea(new Dimension(0,10)));

        // Cancel/Import buttons
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> frame.dispose());
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            System.out.println("selecting!");
            // TODO: send data to main
            // TODO: dispose
//            frame.dispose();
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(selectButton);

        JScrollPane scrollPane = new JScrollPane(panel);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPane, BorderLayout.SOUTH);
    }

    public void show() {
        frame.setVisible(true);
    }
}
