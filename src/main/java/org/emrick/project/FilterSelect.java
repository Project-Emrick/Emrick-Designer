package org.emrick.project;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class FilterSelect {

    private final JDialog frame;

    public FilterSelect(JFrame parent, SelectListener listener, HashSet<Integer> labels, HashSet<String> symbols) {
        this.frame = new JDialog(parent, true);
        this.frame.setTitle("Select By Label");
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.frame.setSize(400, 250);
        this.frame.setLocationRelativeTo(parent); // center on screen
        this.frame.setResizable(false); // resize window option

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // By Label
        JPanel labelPanel = new JPanel();
        JLabel selectLabelLabel = new JLabel("Select By Number");
        JTextField selectLabelTextField = new JTextField(10);
        JLabel selectLabelSyntaxLabel = new JLabel("Enter a list of numbers and/or ranges\n(Ex. 1,5-10,20)");
        labelPanel.add(selectLabelLabel);
        labelPanel.add(selectLabelTextField);

        // By Symbol
        JPanel symbolPanel = new JPanel();
        JLabel selectSymbolLabel = new JLabel("Select By Symbol");
        JTextField selectSymbolTextField = new JTextField(10);
        JLabel selectSymbolSyntaxLabel = new JLabel("Enter a list of instrument symbols\n(Ex. A,C,R,T)");
        symbolPanel.add(selectSymbolLabel);
        symbolPanel.add(selectSymbolTextField);

        panel.add(labelPanel);
        panel.add(selectLabelSyntaxLabel);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
        panel.add(symbolPanel);
        panel.add(selectSymbolSyntaxLabel);
        panel.add(Box.createRigidArea(new Dimension(0,5)));
        JLabel fullSyntaxLabel = new JLabel("Leave either field blank to select all from that field");
        panel.add(fullSyntaxLabel);
        panel.add(Box.createRigidArea(new Dimension(0,5)));

        // Cancel/Import buttons
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> frame.dispose());
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            HashSet<Integer> selectedLabels = new HashSet<>();
            HashSet<String> selectedSymbols = new HashSet<>();
            String[] labelList = selectLabelTextField.getText().split(",");
            if (!labelList[0].isEmpty()) {
                for (String label : labelList) {
                    if (label.contains("-")) {
                        int start = Integer.parseInt(label.substring(0, label.indexOf("-")));
                        int end;
                        try {
                            end = Integer.parseInt(label.substring(label.indexOf("-") + 1));
                        } catch (IndexOutOfBoundsException ex) {
                            for (int i : labels) {
                                if (i >= start) {
                                    selectedLabels.add(i);
                                }
                            }
                            continue;
                        }
                        for (int i : labels) {
                            if (i >= start && i <= end) {
                                selectedLabels.add(i);
                            }
                        }
                    } else {
                        selectedLabels.add(Integer.parseInt(label));
                    }
                }
            } else {
                selectedLabels.addAll(labels);
            }

            String[] symbolList = selectSymbolTextField.getText().split(",");
            if (!symbolList[0].isEmpty()) {
                selectedSymbols.addAll(Arrays.asList(symbolList));
            } else {
                selectedSymbols.addAll(symbols);
            }
            listener.onMultiSelect(selectedLabels, selectedSymbols);

            frame.dispose();
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(selectButton);


        frame.add(panel);
        frame.add(buttonPane, BorderLayout.SOUTH);
    }

    public void show() {
        frame.setVisible(true);
    }
}
