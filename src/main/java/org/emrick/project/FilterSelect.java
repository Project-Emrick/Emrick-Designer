package org.emrick.project;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class FilterSelect {

    private final JDialog frame;
//    private final JTextField selectLabelButton;
//    private final JTextField selectSymbolButton;
    private final ArrayList<JComboBox> selectLabelComboBoxes;
    private final ArrayList<JComboBox> selectSymbolComboBoxes;
    private final JButton cancelButton;
    private final JButton selectButton;

    public FilterSelect(JFrame parent) {
        this.frame = new JDialog(parent, true);
        this.frame.setTitle("Select by criteria");
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.frame.setSize(300, 200);
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
        selectLabelAddButton.addActionListener(e -> {
            System.out.println("adding new label to check");
        });
        labelPanel.add(selectLabelLabel);
        labelPanel.add(selectLabelAddButton);

        // By Symbol
        JPanel symbolPanel = new JPanel();
        JLabel selectSymbolLabel = new JLabel("Select by Symbol");
        JButton selectSymbolAddButton = new JButton("+");
        selectSymbolAddButton.addActionListener(e -> {
            System.out.println("adding new symbol to check");
        });
        symbolPanel.add(selectSymbolLabel);
        symbolPanel.add(selectSymbolAddButton);

        panel.add(labelPanel);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
        panel.add(symbolPanel);
        panel.add(Box.createRigidArea(new Dimension(0,10)));

        // Cancel/Import buttons
        this.cancelButton = new JButton("Cancel");
        this.selectButton = new JButton("Select");

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
