package org.emrick.project.effect;

import org.emrick.project.SelectFileGUI;
import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * This class really just sets up a button to create an RF trigger. If there's already an RF trigger on the current
 * count, then set up a delete button. If there's not, then set up a create button.
 */
public class RFTriggerGUI {

    private final int count;
    private long timestampMillis;
    private RFTrigger rfTrigger;
    private final RFTriggerListener rfTriggerListener;
    private JButton createDeleteBtn;
    private JButton updateBtn;
    private JTextField titleField;
    private JTextArea descField;
    private JTextField cueField;
    private JPanel createDeletePnl;

    public RFTriggerGUI(int count, long timestampMillis, RFTrigger rfTrigger, RFTriggerListener rfTriggerListener) {
        this.count = count;
        this.timestampMillis = timestampMillis;
        this.rfTrigger = rfTrigger;
        this.rfTriggerListener = rfTriggerListener;
        //setupGUI();
        setupPanelGUI();
    }

    private void setupGUI() { //replaced by setupPanelGUI
        if (rfTrigger == null) {
            createDeleteBtn = new JButton("Create RF Trigger");

            createDeleteBtn.addActionListener(e -> {
                rfTrigger = new RFTrigger(count, timestampMillis, "mytitle", "mydesc", "mycue");
                rfTriggerListener.onCreateRFTrigger(rfTrigger);
            });
        } else {
            createDeleteBtn = new JButton("Delete RF Trigger");
            createDeleteBtn.setBackground(new Color(32, 136, 203));
            createDeleteBtn.setForeground(Color.WHITE);

            createDeleteBtn.addActionListener(e -> {
                rfTriggerListener.onDeleteRFTrigger(count);
            });
        }
    }

    private void setupPanelGUI() {
        if (createDeletePnl != null) {
            createDeletePnl.remove(createDeleteBtn);
        }

        JLabel titleLabel = new JLabel("Title:");
        JLabel descLabel = new JLabel("Description:");
        JLabel cueLabel = new JLabel("Cue:");
        titleField = new JTextField(20);
        descField = new JTextArea(3,20);
        cueField = new JTextField(20);

        createDeletePnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        createDeletePnl.setPreferredSize(new Dimension(300, 300));
        createDeletePnl.add(titleLabel, BorderLayout.WEST);
        createDeletePnl.add(titleField, BorderLayout.EAST);
        createDeletePnl.add(cueLabel, BorderLayout.WEST);
        createDeletePnl.add(cueField, BorderLayout.EAST);
        createDeletePnl.add(descLabel, BorderLayout.WEST);
        createDeletePnl.add(descField, BorderLayout.EAST);

        if (rfTrigger == null) {
            createDeleteBtn = new JButton("Create RF Trigger");

            createDeleteBtn.addActionListener(e -> {
                rfTrigger = new RFTrigger(count, timestampMillis, titleField.getText() ,descField.getText(), cueField.getText());

                rfTriggerListener.onCreateRFTrigger(rfTrigger);
            });

            createDeletePnl.add(createDeleteBtn, BorderLayout.SOUTH);
        } else {
            createDeleteBtn = new JButton("Delete RF Trigger");
            createDeleteBtn.setBackground(new Color(32, 136, 203));
            createDeleteBtn.setForeground(Color.WHITE);

            updateBtn = new JButton("Update RF Trigger");

            updateBtn.addActionListener(e -> {
                rfTrigger = new RFTrigger(count, timestampMillis, titleField.getText(), descField.getText(), cueField.getText());
                rfTriggerListener.onUpdateRFTrigger(rfTrigger, count);
            });



            createDeleteBtn.addActionListener(e -> {
                rfTriggerListener.onDeleteRFTrigger(count);
            });

            titleField.setText(rfTrigger.getTitle());
            descField.setText(rfTrigger.getDescription());
            cueField.setText(rfTrigger.getCue());

            createDeletePnl.add(updateBtn);
            createDeletePnl.add(createDeleteBtn);
        }
    }

    public JButton getCreateDeleteBtn() {
        return createDeleteBtn;
    } //TODO: Make this a panel instead, retain functionality but add three text fields if creating

    public JPanel getCreateDeletePnl() {
        return createDeletePnl;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        RFTriggerListener rfTriggerListener1 = new RFTriggerListener() {
            @Override
            public void onCreateRFTrigger(RFTrigger rfTrigger) {
                System.out.println("RF Create");
            }

            @Override
            public void onUpdateRFTrigger(RFTrigger rfTrigger, int count) {System.out.println("RF Update");}

            @Override
            public void onDeleteRFTrigger(int count) {
                System.out.println("RF Delete");
            }

            @Override
            public void onPressRFTrigger(RFTrigger rfTrigger) {
                System.out.println("RF Pressed");
            }
        };

        long timestampMillis = 1000;
        RFTrigger rfTrigger1 = new RFTrigger(1, 1050, "mytitle", "mydesc", "mycue");
        RFTriggerGUI rfTriggerGUI = new RFTriggerGUI(10, timestampMillis, rfTrigger1, rfTriggerListener1);
        frame.add(rfTriggerGUI.getCreateDeleteBtn());
        System.out.println("Setting Visible");
        frame.setVisible(true);
    }

}
