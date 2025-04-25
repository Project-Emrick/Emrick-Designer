package org.emrick.project.effect;

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

        // Create main panel with border layout
        createDeletePnl = new JPanel(new BorderLayout());
        
        // Create content panel that will hold all components
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.NORTH;

        // Initialize components
        JLabel titleLabel = new JLabel("Title:");
        JLabel descLabel = new JLabel("Description:");
        JLabel cueLabel = new JLabel("Cue:");
        titleField = new JTextField(10);
        descField = new JTextArea(3, 10);
        descField.setLineWrap(true);
        descField.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descField);
        cueField = new JTextField(10);

        // Add components using GridBagLayout
        gbc.gridy = 0;
        if (rfTrigger != null) {
            long minutesStart = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(rfTrigger.getTimestampMillis());
            long secondsStart = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(rfTrigger.getTimestampMillis()) % 60;
            long millisecondsStart = rfTrigger.getTimestampMillis() % 1000;
            addComponentPair(new JLabel("Count:"), new JLabel(String.format("%d", rfTrigger.getCount())), contentPanel, gbc);
            addComponentPair(new JLabel("Time:"), new JLabel(String.format("%d:%02d:%03d", minutesStart, secondsStart, millisecondsStart)), contentPanel, gbc);
        }
        addComponentPair(titleLabel, titleField, contentPanel, gbc);
        addComponentPair(cueLabel, cueField, contentPanel, gbc);
        addComponentPair(descLabel, descScrollPane, contentPanel, gbc);

        // Add a dummy component at the bottom to push everything up
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        JPanel filler = new JPanel();
        contentPanel.add(filler, gbc);

        // Create scroll pane for the content
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null); // Remove scroll pane border

        // Set up the title border
        Border innerBorder = BorderFactory.createTitledBorder("RF Trigger");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        createDeletePnl.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        if (rfTrigger == null) {
            createDeleteBtn = new JButton("Create RF Trigger");
            createDeleteBtn.addActionListener(e -> {
                rfTrigger = new RFTrigger(count, timestampMillis, titleField.getText(), descField.getText(), cueField.getText());
                rfTriggerListener.onCreateRFTrigger(rfTrigger);
            });
            buttonPanel.add(createDeleteBtn);
        } else {
            updateBtn = new JButton("Update RF Trigger");
            updateBtn.addActionListener(e -> {
                rfTrigger = new RFTrigger(count, timestampMillis, titleField.getText(), descField.getText(), cueField.getText());
                rfTriggerListener.onUpdateRFTrigger(rfTrigger, count);
            });

            createDeleteBtn = new JButton("Delete RF Trigger");
            createDeleteBtn.setBackground(new Color(32, 136, 203));
            createDeleteBtn.setForeground(Color.WHITE);
            createDeleteBtn.addActionListener(e -> {
                rfTriggerListener.onDeleteRFTrigger(count);
            });

            titleField.setText(rfTrigger.getTitle());
            descField.setText(rfTrigger.getDescription());
            cueField.setText(rfTrigger.getCue());

            buttonPanel.add(updateBtn);
            buttonPanel.add(createDeleteBtn);
        }

        createDeletePnl.add(scrollPane, BorderLayout.CENTER);
        createDeletePnl.add(buttonPanel, BorderLayout.SOUTH);
        createDeletePnl.setPreferredSize(new Dimension(300, 250));
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

    // Helper method to add component pairs
    private void addComponentPair(JComponent comp1, JComponent comp2, JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 0.4;
        if (comp1 instanceof JLabel) {
            ((JLabel) comp1).setHorizontalAlignment(SwingConstants.RIGHT);
        }
        panel.add(comp1, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        panel.add(comp2, gbc);
        
        gbc.gridy++;
    }
}
