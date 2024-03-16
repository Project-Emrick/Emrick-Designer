package org.emrick.project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class SyncTimeGUI implements ActionListener {

    // String Constants
    private static final String PATH_INSTR_IMAGE = "./src/main/resources/images/sync_time_instr.jpg";
    private static final String START_TIMESTAMP_INSTRUCTION = "Starting timestamp (Example: '0:00')";
    private static final String BPM_INSTRUCTION = "<html><p>" +
            "Please enter the BPM (beats-per-minute) for each set. Example: '105'. " +
            "If the BPM is consistent, you may enter it for Set 1 alone." +
            "<br><br>" +
            "</p></html>";
    private static final String DURATION_INSTRUCTION = "<html><p>" +
            "Please enter the duration in seconds for each set. Example: '7', or '0:07'" +
            "<br><br>" +
            "</p></html>";
    private static final String TIMESTAMP_INSTRUCTION = "<html><p>" +
//            "Assuming that your Pyware drill was synced to music, " +
//            "in order to play each set at the appropriate rate, " +
            "Please enter the timestamps for each set, exactly as shown in the Pyware count track. Example: '0:07'" +
            "<br><br>" +
//            "For example, in the image above, a Count Track shows Page Tab 1A is placed at count 16 and has timestamp 0:07. " +
//            "If this was your Count Track, you would enter 0:07 (exactly as shown) in the field corresponding to Page Tab 1A." +
//            "<br><br>" +
            "</p></html>";

    // Page Tab / Count / Times
    private Map<String, Integer> set2Count; // [Set PageTab]:[count] e.g., k:"2A", v:30 , From ScrubBarGUI
    private Map<String, Integer> set2Time; // [Set PageTab]:[time] e.g., k:"2A", v:21 , time (sec) since start
    private ArrayList<Map.Entry<String, JTextField>> set2TimestampField;
    private ArrayList<Map.Entry<String, JTextField>> set2DurationField;
    private ArrayList<Map.Entry<String, JTextField>> set2BpmField;
    private JTextField startTimestampFieldDuration;
    private JTextField startTimestampFieldBpm;

    // Tabbed Panes
    private JTabbedPane tabbedPane;
    private JPanel bpmPanel;
    private JPanel timestampPanel;
    private JPanel durationPanel;

    private JDialog dialogWindow;
    private JButton cancelButton;
    private JButton syncButton;

    private SyncListener syncListener;

    public SyncTimeGUI(JFrame parent, SyncListener syncListener, Map<String, Integer> set2Count) {
        this.set2Count = set2Count;
        this.syncListener = syncListener;

        dialogWindow = new JDialog(parent, true);
        dialogWindow.setTitle("Sync Time to Original Drill");
        dialogWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialogWindow.setSize(400, 400);
        dialogWindow.setLayout(new BorderLayout(10, 10));
        dialogWindow.setResizable(false);
        dialogWindow.setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBounds(50,50,200,200);

        bpmPanel = createBpmPanel();
        tabbedPane.add("BPM", bpmPanel);

        durationPanel = createDurationPanel();
        tabbedPane.add("Duration", durationPanel);

        timestampPanel = createTimestampPanel();
        tabbedPane.add("Timestamp", timestampPanel);

        // Cancel/Import buttons
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        syncButton = new JButton("Sync");
        syncButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(syncButton);

        dialogWindow.add(tabbedPane);
        dialogWindow.add(buttonPane, BorderLayout.SOUTH);

        show();
    }

    private JPanel createBpmPanel() {

        // A main panel for padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title panel for instructions
        JPanel titlePanel = new JPanel(new BorderLayout(0, 10));

        JLabel titleLabel = new JLabel("BPM (Tempo)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel instrLabel = new JLabel(BPM_INSTRUCTION);

        JPanel startTimestampPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        startTimestampFieldBpm = new JTextField(6);

        startTimestampPanel.add(new JLabel(START_TIMESTAMP_INSTRUCTION));
        startTimestampPanel.add(startTimestampFieldBpm);

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(startTimestampPanel);
        titlePanel.add(instrLabel, BorderLayout.SOUTH);

        // Panel for user to enter BPM per set
        JPanel bpmPanel = new JPanel(new GridLayout(set2Count.size() + 1, 3, 2, 2));
        bpmPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));

        JScrollPane bpmScrollPane = new JScrollPane(bpmPanel);
        bpmScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        bpmPanel.add(new JLabel("Set"));
        bpmPanel.add(new JLabel("BPM"));
        bpmPanel.add(new JLabel("Count"));

        List<Map.Entry<String, Integer>> ptCounts = ScrubBarGUI.sortMap(set2Count);

        set2BpmField = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : ptCounts) {
            JLabel setLabel = new JLabel("  " + entry.getKey());
            bpmPanel.add(setLabel); // Set e.g., "2A"

            JTextField textField = new JTextField();
            textField.setToolTipText("Enter BPM of Set " + entry.getKey());

            set2BpmField.add(new AbstractMap.SimpleEntry<>(entry.getKey(), textField)); // Keep a reference to text fields
            bpmPanel.add(textField);

            JLabel countLabel = new JLabel("  " + entry.getValue().toString());
            bpmPanel.add(countLabel); // Count e.g., "48"
        }

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(bpmScrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createDurationPanel() {

        // A main panel for padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title panel for instructions
        JPanel titlePanel = new JPanel(new BorderLayout(0, 10));

        JLabel titleLabel = new JLabel("Duration");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel instrLabel = new JLabel(DURATION_INSTRUCTION);

        JPanel startTimestampPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        startTimestampFieldDuration = new JTextField(6);

        startTimestampPanel.add(new JLabel(START_TIMESTAMP_INSTRUCTION));
        startTimestampPanel.add(startTimestampFieldDuration);

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(startTimestampPanel);
        titlePanel.add(instrLabel, BorderLayout.SOUTH);

        // Panel for user to enter BPM per set
        JPanel bpmPanel = new JPanel(new GridLayout(set2Count.size() + 1, 3, 2, 2));
        bpmPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));

        JScrollPane bpmScrollPane = new JScrollPane(bpmPanel);
        bpmScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        bpmPanel.add(new JLabel("Set"));
        bpmPanel.add(new JLabel("Duration"));
        bpmPanel.add(new JLabel("Count"));

        List<Map.Entry<String, Integer>> ptCounts = ScrubBarGUI.sortMap(set2Count);

        set2DurationField = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : ptCounts) {
            JLabel setLabel = new JLabel("  " + entry.getKey());
            bpmPanel.add(setLabel); // Set e.g., "2A"

            JTextField textField = new JTextField();
            textField.setToolTipText("Enter Duration of Set " + entry.getKey());

            set2DurationField.add(new AbstractMap.SimpleEntry<>(entry.getKey(), textField)); // Keep a reference to text fields
            bpmPanel.add(textField);

            JLabel countLabel = new JLabel("  " + entry.getValue().toString());
            bpmPanel.add(countLabel); // Count e.g., "48"
        }

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(bpmScrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createTimestampPanel() {
        // A main Panel for padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title panel for instructions
        JPanel titlePanel = new JPanel(new BorderLayout(0, 10));

        JLabel titleLabel = new JLabel("Timestamp");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Add instruction image
//        try {
//            BufferedImage instrImage = ImageIO.read(new File(PATH_INSTR_IMAGE));
//
//            int newWidth = dialogWindow.getWidth() - 20; // minus 20 for padding
//            int newHeight = (int) (float) newWidth * instrImage.getHeight() / instrImage.getWidth();
//
//            Image scaledInstrImage = instrImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
//            JLabel instrImageLabel = new JLabel(new ImageIcon(scaledInstrImage));
//            instrImageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
//            titlePanel.add(instrImageLabel, BorderLayout.CENTER);
//        } catch (IOException e) {
//            System.out.println("SyncTimeGUI: initialize() " + e.getMessage());
//        }

        JLabel instrLabel = new JLabel(TIMESTAMP_INSTRUCTION);

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(instrLabel, BorderLayout.SOUTH);

        // Panel for User to enter page tab times
        JPanel entryPanel = new JPanel(new GridLayout(set2Count.size() + 1, 3, 2, 2));
        entryPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
//        entryPanel.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));

        JScrollPane entryScrollPane = new JScrollPane(entryPanel);
        entryScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        entryPanel.add(new JLabel("Set"));
        entryPanel.add(new JLabel("Timestamp"));
        entryPanel.add(new JLabel("Count"));

        List<Map.Entry<String, Integer>> ptCounts = ScrubBarGUI.sortMap(set2Count);

        set2TimestampField = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : ptCounts) {
            JLabel setLabel = new JLabel("  " + entry.getKey());
            entryPanel.add(setLabel); // Set e.g., "2A"

            JTextField textField = new JTextField();
            textField.setToolTipText("Enter Timestamp of Set " + entry.getKey());

            set2TimestampField.add(new AbstractMap.SimpleEntry<>(entry.getKey(), textField)); // Keep a reference to text fields
            entryPanel.add(textField);

            JLabel countLabel = new JLabel("  " + entry.getValue().toString());
            entryPanel.add(countLabel); // Count e.g., "48"
        }

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(entryScrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    public void show() {
        dialogWindow.setVisible(true);
    }

    public static class Pair {
        private String key;
        private Integer value;

        public Pair(String key, Integer value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Integer getValue() {
            return value;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cancelButton)) {
            dialogWindow.dispose();
        } else if (e.getSource().equals(syncButton)) {

            // This currently expects the text input to be a duration, despite the help text implying it needs to be a timestamp.
            //  this is planned to be changed to beats per minute, so plan accordingly!

            ArrayList<Pair> times = new ArrayList<>();

            if (tabbedPane.getSelectedComponent().equals(durationPanel)) {
                syncByDuration(times);
            }
            else if (tabbedPane.getSelectedComponent().equals(bpmPanel)) {
                syncByBpm(times);
            }
            else if (tabbedPane.getSelectedComponent().equals(timestampPanel)) {
                syncByTimestamp(times);
            }

            syncListener.onSync(times);

            dialogWindow.dispose();
        }
    }

    private void syncByDuration(ArrayList<Pair> times) {
        for (Map.Entry<String, JTextField> ptField : set2DurationField) {
            String set = ptField.getKey();

            String in = ptField.getValue().getText();
            String[] stamp = in.split(":", 2);
            int time = 0;
            if (stamp.length == 0) {
                JOptionPane.showMessageDialog(dialogWindow, "Failed to parse sync time stamp for \"" + set + "\".", "Sync Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (stamp.length == 1) {
                try {
                    time += Integer.parseInt(stamp[0]);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialogWindow, "Failed to parse sync time stamp for \"" + set + "\".", "Sync Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (stamp.length == 2) {
                try {
                    time += Integer.parseInt(stamp[1]);
                    time += 60 * Integer.parseInt(stamp[0]);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialogWindow, "Failed to parse sync time stamp for \"" + set + "\".", "Sync Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Time in seconds
            //  Time is duration for the set.
            times.add(new Pair(set, time));
        }
    }

    private void syncByBpm(ArrayList<Pair> times) {
        // TODO

        for (Map.Entry<String, JTextField> bpmField : set2BpmField) {

        }
    }

    private void syncByTimestamp(ArrayList<Pair> times) {
        // TODO

        for (Map.Entry<String, JTextField> timestampField : set2TimestampField) {

        }
    }

    // For testing
    public static void main(String[] args) {

        // Run Swing programs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame dummyParent = new JFrame();

                SyncListener dummySyncListener = new SyncListener() {
                    @Override
                    public void onSync(ArrayList<Pair> times) {
                        System.out.println("dummy onSync() called.");
                    }
                };

                // Dummy input
                Map<String, Integer> dummyData = new HashMap<>();
                dummyData.put("1", 0); // Page tab 1 maps to count 0
                dummyData.put("1A", 16); // Page tab 1A maps to count 16
                dummyData.put("2", 32); // Page tab 2 maps to count 32
                dummyData.put("2A", 48); // etc.
                dummyData.put("3", 64);
                dummyData.put("3A", 88);
                dummyData.put("4", 96);
                dummyData.put("4A", 112);
                dummyData.put("4B", 128);
                dummyData.put("5", 136);
                dummyData.put("6", 152);

                // Lots more data, making sure they fit on GUI
                dummyData.put("6A", 168);
                dummyData.put("7", 184);
                dummyData.put("7A", 200);
                dummyData.put("7B", 216);
                dummyData.put("8", 228);
                dummyData.put("9", 230);
                dummyData.put("10", 232);
                dummyData.put("11", 234);
                dummyData.put("12", 236);
                dummyData.put("13", 238);
                dummyData.put("14", 240);
                dummyData.put("15", 242);
                dummyData.put("16", 244);

                new SyncTimeGUI(dummyParent, dummySyncListener, dummyData); // Automatically visible

//                SyncTimeGUI syncTimeGUI = new SyncTimeGUI(dummyParent, dummySyncListener, dummyData);
//                syncTimeGUI.show();
            }
        });
    }
}

