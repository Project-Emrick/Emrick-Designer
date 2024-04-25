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

    public RFTriggerGUI(int count, long timestampMillis, RFTrigger rfTrigger, RFTriggerListener rfTriggerListener) {
        this.count = count;
        this.timestampMillis = timestampMillis;
        this.rfTrigger = rfTrigger;
        this.rfTriggerListener = rfTriggerListener;
        setupGUI();
    }

    private void setupGUI() {
        if (rfTrigger == null) {
            createDeleteBtn = new JButton("Create RF Trigger");

            createDeleteBtn.addActionListener(e -> {
                rfTrigger = new RFTrigger(count, timestampMillis);
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

    public JButton getCreateDeleteBtn() {
        return createDeleteBtn;
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

        long timestampMillis = 1000;
        RFTrigger rfTrigger1 = new RFTrigger(1, 1050);
        RFTriggerGUI rfTriggerGUI = new RFTriggerGUI(10, timestampMillis, rfTrigger1, rfTriggerListener1);
        frame.add(rfTriggerGUI.getCreateDeleteBtn());

        frame.setVisible(true);
    }

}
