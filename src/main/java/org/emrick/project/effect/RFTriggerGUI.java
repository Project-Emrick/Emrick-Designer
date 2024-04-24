package org.emrick.project.effect;

import org.emrick.project.SelectFileGUI;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class RFTriggerGUI {

    private final int count;
    private RFTrigger rfTrigger;
    private final RFTriggerListener rfTriggerListener;
    private JPanel rfTriggerPanel;

    public static String noProjectSyncMsg = "<html><body style='text-align: center;'>Load a time-synced Emrick project to get started using RF triggers.</body></html>";

    /**
     * In the case that the project has not yet been loaded and synced.
     * @param placeholderText Placeholder message.
     */
    public RFTriggerGUI(String placeholderText) {
        this.count = 0;
        this.rfTrigger = null;
        this.rfTriggerListener = null;

        this.rfTriggerPanel = new JPanel();
        this.rfTriggerPanel.setPreferredSize(new Dimension(300, 120));

        Border innerBorder = BorderFactory.createTitledBorder("RF Trigger");
        Border outerBorder = BorderFactory.createEmptyBorder(0,5,5,5);
        Border innerBorder2 = BorderFactory.createEmptyBorder(20,20,20,20);
        Border outerBorder2 = BorderFactory.createCompoundBorder(outerBorder, innerBorder);

        this.rfTriggerPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder2, innerBorder2));

        this.rfTriggerPanel.setLayout(new GridBagLayout());

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
        this.rfTriggerPanel.add(placeholderLabel, gc);
    }

    public RFTriggerGUI(int count, RFTrigger rfTrigger, RFTriggerListener rfTriggerListener) {
        this.count = count;
        this.rfTrigger = rfTrigger;
        this.rfTriggerListener = rfTriggerListener;
        setupGUI();
    }

    private void setupGUI() {
        this.rfTriggerPanel = new JPanel();
        this.rfTriggerPanel.setPreferredSize(new Dimension(300, 70));

        Border innerBorder = BorderFactory.createTitledBorder("RF Trigger");
        Border outerBorder = BorderFactory.createEmptyBorder(0,5,5,5);

        this.rfTriggerPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        this.rfTriggerPanel.setLayout(new GridBagLayout());

        if (rfTrigger == null) {
            JButton createButton = new JButton("Create RF Trigger");
            createButton.setBackground(new Color(175, 215, 241));
            this.rfTriggerPanel.add(createButton);

            createButton.addActionListener(e -> {
                rfTrigger = new RFTrigger(count);
                rfTriggerListener.onCreateRFTrigger(rfTrigger);
                JOptionPane.showMessageDialog(null,
                        "RF trigger created successfully at count " + count, "RF Trigger Create: Success",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        } else {
            JButton deleteButton = new JButton("Delete RF Trigger");
            deleteButton.setBackground(new Color(241, 177, 177));
            this.rfTriggerPanel.add(deleteButton);

            deleteButton.addActionListener(e -> {
                rfTriggerListener.onDeleteRFTrigger(count);
                JOptionPane.showMessageDialog(null,
                        "RF trigger at count " + count + " deleted successfully", "RF Trigger Delete: Success",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    public JPanel getRfTriggerPanel() {
        return rfTriggerPanel;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(300, 100);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        RFTriggerListener rfTriggerListener1 = new RFTriggerListener() {
            @Override
            public void onCreateRFTrigger(RFTrigger rfTrigger) {
                System.out.println("RF Create");
            }

            @Override
            public void onDeleteRFTrigger(int count) {
                System.out.println("RF Delete");
            }
        };

        RFTrigger rfTrigger1 = new RFTrigger(10);
//        RFTriggerGUI rfTriggerGUI = new RFTriggerGUI(10, rfTrigger1, rfTriggerListener1);
        RFTriggerGUI rfTriggerGUI2 = new RFTriggerGUI(RFTriggerGUI.noProjectSyncMsg);
        frame.add(rfTriggerGUI2.getRfTriggerPanel());

        frame.setVisible(true);
    }

}
