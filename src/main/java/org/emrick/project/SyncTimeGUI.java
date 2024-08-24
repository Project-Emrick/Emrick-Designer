package org.emrick.project;

import org.emrick.project.audio.AudioPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

import static java.awt.event.KeyEvent.VK_SPACE;

public class SyncTimeGUI implements ActionListener {

    private static boolean IS_DEBUG = false;

    // String Constants
    private static final String PATH_INSTR_IMAGE = PathConverter.pathConverter("/src/main/resources/images/sync_time_instr.jpg");
    private static final String START_TIMESTAMP_INSTRUCTION = "Provide a start delay in seconds (optional):";
    private static final String BPM_INSTRUCTION = "<html><p>" +
            "Please enter the BPM (beats-per-minute) for each set. Example: '105' or '105.5'. " +
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
    private final Map<String, Integer> set2Count; // [Set PageTab]:[count] e.g., k:"2A", v:30 , From ScrubBarGUI
    private ArrayList<Map.Entry<String, JTextField>> set2TimestampField;
    private ArrayList<Map.Entry<String, JTextField>> set2DurationField;
    private ArrayList<Map.Entry<String, JTextField>> set2BpmField;
    private JTextField startDelayFieldDuration;
    private JTextField startDelayFieldBpm;

    // Tabbed Panes
    private final JTabbedPane tabbedPane;
    private final JPanel bpmPanel;
    private final JPanel timestampPanel;
    private final JPanel durationPanel;
    private final JPanel tapPanel;

    private final JDialog dialogWindow;
    private final JButton cancelButton;
    private final JButton syncButton;

    private final SyncListener syncListener;

    AudioPlayer audioPlayer;

    int currentCount = 0;
    long prevCountTime = 0;
    int totalCounts;
    long currentTime = 0;
    ArrayList<PairCountMS> counts;


    Action tapAction;

    public SyncTimeGUI(JFrame parent, SyncListener syncListener, Map<String, Integer> set2Count, AudioPlayer audioPlayer, int totalCounts)   {
        this.set2Count = set2Count;
        this.syncListener = syncListener;
        this.audioPlayer = audioPlayer;
        this.totalCounts = totalCounts;

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


        tapPanel = createTapPanel();
        tabbedPane.add("Tap", tapPanel);



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

        dialogWindow.setVisible(true);
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

        startDelayFieldBpm = new JTextField(6);

        startTimestampPanel.add(new JLabel(START_TIMESTAMP_INSTRUCTION));
        startTimestampPanel.add(startDelayFieldBpm);

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

            if (IS_DEBUG) {
                startDelayFieldBpm.setText("0.5");
                textField.setText("138.93");
                IS_DEBUG = false;
            }

            set2BpmField.add(new AbstractMap.SimpleEntry<>(entry.getKey(), textField)); // Keep a reference to text fields
            bpmPanel.add(textField);

            JLabel countLabel = new JLabel("  " + entry.getValue().toString());
            bpmPanel.add(countLabel); // Count e.g., "48"
        }

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(bpmScrollPane, BorderLayout.CENTER);

        return mainPanel;
    }
    private JPanel createTapPanel() {
        // A main panel for padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title panel for instructions
        JPanel titlePanel = new JPanel(new BorderLayout(0, 10));

        JLabel titleLabel = new JLabel("Tap Tempo");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        //TODO: Create a string object of the text below
        JLabel instrLabel = new JLabel("When ready, tap the spacebar at the starting tempo. The audio will start automatically." +
                " The window will close once all of the show counts have been tapped.");

        JPanel tapTempoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(instrLabel, BorderLayout.SOUTH);


        System.out.println(totalCounts);
        counts = new ArrayList<>();
        tapAction = new TapAction();

        tapTempoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(' '), "tapAction");
        tapTempoPanel.getActionMap().put("tapAction", tapAction);

        tapTempoPanel.add(new JLabel("TAP HERE"));
        tapTempoPanel.setFocusable(true);

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(tapTempoPanel, BorderLayout.SOUTH);


          //time in ms that the last beat occurred

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

        startDelayFieldDuration = new JTextField(6);

        startTimestampPanel.add(new JLabel(START_TIMESTAMP_INSTRUCTION));
        startTimestampPanel.add(startDelayFieldDuration);

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

    public static class Pair {
        private String key;
        private float value;

        public Pair(String key, float value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public float getValue() {
            return value;
        }
    }
    public static class PairCountMS {
        private int key;
        private long value;  //ms to the next count

        public PairCountMS(int key, long value) {
            this.key = key;
            this.value = value;
        }

        public int getKey() {
            return key;
        }
        public long  getValue() {
            return value;
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cancelButton)) {
            dialogWindow.dispose();
            if (audioPlayer.isAlive()) {
                audioPlayer.pauseAudio();
            }
        } else if (e.getSource().equals(syncButton)) {

            // This currently expects the text input to be a duration, despite the help text implying it needs to be a timestamp.
            //  this is planned to be changed to beats per minute, so plan accordingly!

            ArrayList<Pair> times = new ArrayList<>();

            boolean isSuccess = false;
            float startDelay = 0;
            if (tabbedPane.getSelectedComponent().equals(durationPanel)) {
                isSuccess = syncByDuration(times);
                startDelay = handleStartDelayInput(startDelayFieldDuration);
                if (startDelay == -1) return;
            }
            else if (tabbedPane.getSelectedComponent().equals(bpmPanel)) {
                isSuccess = syncByBpm(times);
                startDelay = handleStartDelayInput(startDelayFieldBpm);
                if (startDelay == -1) return;
            }
            else if (tabbedPane.getSelectedComponent().equals(timestampPanel)) {
                isSuccess = syncByTimestamp(times);
            }
            else if (tabbedPane.getSelectedComponent().equals((tapPanel))) {
                isSuccess = syncByTap(times);
            }
            if (isSuccess) {
                syncListener.onSync(times, startDelay);
                dialogWindow.dispose();
            }
        }
    }

    private float handleStartDelayInput(JTextField textField) {

        // If user enters a valid start delay (delay value in seconds)
        float startDelay = 0;
        if (!textField.getText().isEmpty()){
            try {
                startDelay = Float.parseFloat(textField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(dialogWindow, "The provided start delay is invalid.",
                        "Start Delay Error", JOptionPane.ERROR_MESSAGE);
                return -1;
            }
        }
        return startDelay;
    }

    private boolean syncByDuration(ArrayList<Pair> times) {
        for (Map.Entry<String, JTextField> ptField : set2DurationField) {
            String set = ptField.getKey();

            String in = ptField.getValue().getText();
            String[] stamp = in.split(":", 2);
            int time = 0;
            if (stamp.length == 0) {
                JOptionPane.showMessageDialog(dialogWindow, "Failed to parse sync time stamp for \"" + set + "\".",
                        "Duration Sync Error", JOptionPane.ERROR_MESSAGE);
                return false;
            } else if (stamp.length == 1) {
                try {
                    time += Integer.parseInt(stamp[0]);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialogWindow, "Failed to parse sync time stamp for \"" + set + "\".",
                            "Duration Sync Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else if (stamp.length == 2) {
                try {
                    time += Integer.parseInt(stamp[1]);
                    time += 60 * Integer.parseInt(stamp[0]);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialogWindow, "Failed to parse sync time stamp for \"" + set + "\".",
                            "Duration Sync Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            // Time in seconds
            //  Time is duration for the set.
            times.add(new Pair(set, time));
        }

        return true;
    }
    private boolean syncByTap(ArrayList<Pair> times) {

        List<Map.Entry<String, Integer>> ptCounts = ScrubBarGUI.sortMap(set2Count);
        ptCounts.sort(Map.Entry.comparingByKey());
        int i = 0;
        float totalTime = 0; //seconds
        for (int j = 0; j < ptCounts.size(); j++) {
            if (j == ptCounts.size() -1) {
                for (int k = 0; k < totalCounts - ptCounts.get(j).getValue(); k++) {
                    if (counts.size() == i) {
                        //less taps than counts
                        break;
                    }
                    //System.out.println((float) counts.get(i).getValue() / 1000.0);
                    totalTime += ((float) (counts.get(i).getValue())) / 1000.0; //get value in ms and convert to seconds
                    i++;
                }
            }
            else {
                for (int k = 0; k < ptCounts.get(j + 1).getValue() - ptCounts.get(j).getValue(); k++) {
                    if (counts.size() == i) {
                        //less taps than counts
                        break;
                    }
                    //System.out.println((float) counts.get(i).getValue() / 1000.0);
                    totalTime += ((float) (counts.get(i).getValue())) / 1000.0; //get value in ms and convert to seconds
                    i++;
                }
            }
            //System.out.println("Length of set " + ptCounts.get(j).getKey() + " is " + totalTime);
            times.add(new Pair(ptCounts.get(j).getKey(), totalTime));
            totalTime = 0;
        }

        return true;
    }

    private boolean syncByBpm(ArrayList<Pair> times)
    {

        // Check if BPM is consistent (where BPM is only entered for set 1)
        boolean isConsistent = true;

        for (Map.Entry<String, JTextField> bpmField : set2BpmField) {
            String fieldText = bpmField.getValue().getText();
            boolean isEmpty = fieldText.isEmpty();
            boolean isGoodFormat = true;
            try {
                Float.parseFloat(fieldText);
            } catch (NumberFormatException e) {
                isGoodFormat = false;
            }

            String set = bpmField.getKey();

            // BPM should always be entered for set 1
            if (set.equals("1") && (isEmpty || !isGoodFormat)) {
                JOptionPane.showMessageDialog(dialogWindow, "Failed to read BPM for set " + set,
                        "BPM Sync Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // BPM was provided for a different set. Do not assume BPM is consistent.
            if (!set.equals("1") && !isEmpty) {
                isConsistent = false;
                break;
            }
        }

        // Let BPM be a float. Not all songs fall on an integral value BPM
        float bpm = Float.parseFloat(set2BpmField.get(0).getValue().getText());
        if (bpm == 0) {
            JOptionPane.showMessageDialog(dialogWindow, "BPM cannot be 0",
                    "BPM Sync Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // If BPM is not necessarily consistent, ensure all fields have input
        if (!isConsistent) {
            for (Map.Entry<String, JTextField> bpmField : set2BpmField) {
                String fieldText = bpmField.getValue().getText();
                boolean isEmpty = fieldText.isEmpty();
                boolean isGoodFormat = true;
                try {
                    Float.parseFloat(fieldText);
                } catch (NumberFormatException e) {
                    isGoodFormat = false;
                }

                String set = bpmField.getKey();

                // BPM input is not valid
                if (isEmpty || !isGoodFormat) {
                    JOptionPane.showMessageDialog(dialogWindow, "Failed to read BPM for set " + set,
                            "BPM Sync Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        List<Map.Entry<String, Integer>> setCountsSorted = ScrubBarGUI.sortMap(set2Count);

        for (int i = 0; i < set2BpmField.size(); i++) {

            // Find number of counts in current set
            int counts;

            // Not the last page tab
            if (i + 1 <= set2BpmField.size() - 1) {
                counts = setCountsSorted.get(i + 1).getValue() - setCountsSorted.get(i).getValue();
            }
            // The last page tab -- end of last set
            else {
                counts = 0;
            }

            // Find the set and bpm entered
            Map.Entry<String, JTextField> bpmField = set2BpmField.get(i);
            String set = bpmField.getKey();
            if (!isConsistent) {
                bpm = Float.parseFloat(bpmField.getValue().getText());
            }

            // Calculate duration of set (in seconds) using counts and bpm
            //  Example: [ 16 ticks / 1 set ] * [ 1 min / 138 ticks ] * [ 60 sec / 1 min ] = 6.96 seconds
            float time = counts / bpm * 60;

            times.add(new Pair(set, time));
        }

        // Debugging
        float totalDuration = 0;
        for (Pair pair : times) {
            System.out.println("Set = " + pair.getKey() + " | Time = " + pair.getValue());
            totalDuration += pair.getValue();
        }
        System.out.println("totalDuration = " + totalDuration + " seconds, not including start delay time.");

        return true;
    }

    private boolean syncByTimestamp(ArrayList<Pair> times) {

        // Check that no fields are empty, and timestamp format is correct
        for (Map.Entry<String, JTextField> timestampField : set2TimestampField) {
            String fieldText = timestampField.getValue().getText();
            boolean isEmpty = fieldText.isEmpty();

            // Regular expression to match the pattern minutes:seconds
            String regex = "^[0-5]?\\d:[0-5]\\d$";
            boolean isGoodFormat = fieldText.matches(regex) && !fieldText.equals("0");
            String set = timestampField.getKey();

            if (isEmpty || !isGoodFormat) {
                JOptionPane.showMessageDialog(dialogWindow, "Failed to read timestamp for set " + set,
                        "Timestamp Sync Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        for (Map.Entry<String, JTextField> timestampField : set2TimestampField) {
            // TODO -- Implement timestamps if have time ?
        }

        return true;
    }
    public class TapAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {

            //System.out.println(prevCountTime + " ms");
            if (currentCount == 0) {
                prevCountTime = System.currentTimeMillis();
                audioPlayer.playAudio(0);
                currentCount++;
            } else if (currentCount >= totalCounts - 1) {
                currentTime = System.currentTimeMillis();
                counts.add(new PairCountMS(currentCount - 1, currentTime - prevCountTime));
                currentCount = 0;
                if (audioPlayer.isAlive()) {
                    audioPlayer.pauseAudio();
                }
                ArrayList<Pair> times = new ArrayList<>();
                boolean isSuccess = true;
                isSuccess = syncByTap(times);
                if (isSuccess) {
                    syncListener.onSync(times, 0);
                    dialogWindow.dispose();
                }
                else {
                    System.out.println("Womp womp");
                }
            }
            else {
                currentTime = System.currentTimeMillis();
                counts.add(new PairCountMS(currentCount - 1, currentTime - prevCountTime));
                //System.out.println(currentTime - prevCountTime);
                prevCountTime = currentTime;
                currentCount++;
            }
        }
    }





    public static void main(String[] args) {

        // Run Swing programs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame dummyParent = new JFrame();

                SyncListener dummySyncListener = new SyncListener() {
                    @Override
                    public void onSync(ArrayList<Pair> times, float startDelay) {
                        System.out.println("dummy onSync() called.");
                    }
                    public void onAutoSync(ArrayList<PairCountMS> counts, float startDelay) {
                        System.out.println("dummy onAutoSync() called.");
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
                AudioPlayer audioPlayerDummy = new AudioPlayer(new File(""));

                // Lots more data, making sure they fit on GUI
//                dummyData.put("6A", 168);
//                dummyData.put("7", 184);
//                dummyData.put("7A", 200);
//                dummyData.put("7B", 216);
//                dummyData.put("8", 228);
//                dummyData.put("9", 230);
//                dummyData.put("10", 232);
//                dummyData.put("11", 234);
//                dummyData.put("12", 236);
//                dummyData.put("13", 238);
//                dummyData.put("14", 240);
//                dummyData.put("15", 242);
//                dummyData.put("16", 280);

                new SyncTimeGUI(dummyParent, dummySyncListener, dummyData, audioPlayerDummy, 0); // Automatically visible

//                SyncTimeGUI syncTimeGUI = new SyncTimeGUI(dummyParent, dummySyncListener, dummyData);
//                syncTimeGUI.show();
            }
        });
    }
}

