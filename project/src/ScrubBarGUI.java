import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrubBarGUI {

    // String definitions
    public static final String X = "";

    private JPanel scrubBarPanel;
    private JCheckBox audioCheckbox;
    private JCheckBox fastPlayCheckbox;
    private JCheckBox fullPlayCheckbox;

    Map<String, Integer> pageTabCounts; // [pageTab]:[count] e.g., k:"2A", v:30
    int lastCount;
    int currSetStartCount;
    int currSetEndCount;

    public ScrubBarGUI(Map<String, Integer> pageTabCounts) {
        this.pageTabCounts = pageTabCounts;

        // Get the last count
        this.lastCount = Collections.max(
                pageTabCounts.entrySet(),
                Map.Entry.comparingByValue()
        ).getValue();

        // Start slides on first count of first set
        if (pageTabCounts.size() == 0) {
            System.out.println("Error: No page tabs found.");
            return;
        }
        else if (pageTabCounts.size() == 1) {
            System.out.println("Error: Only one page tab found.");
            return;
        }
        else if (pageTabCounts.get("1") == null) {
            System.out.println("Error: Can't find page tab 1.");
        }

        updateCurrSetCounts("1");

        initialize();
    }

    private void initialize() {

        scrubBarPanel = new JPanel(new BorderLayout(10, 10));
        scrubBarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel toolBarPanel = getToolBarPanel();

        scrubBarPanel.add(toolBarPanel, BorderLayout.WEST);

        // Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.setPreferredSize(new Dimension(100, 1));
        JLabel statusLabel = new JLabel("Set : 0", JLabel.CENTER);
        statusPanel.add(statusLabel);

        scrubBarPanel.add(statusPanel, BorderLayout.EAST);

        JPanel sliderPanel = new JPanel(new GridLayout(2, 1));

        // Slider for navigating different sets
        JSlider topSlider = new JSlider(JSlider.HORIZONTAL,0, pageTabCounts.size() - 1, 0);
        Hashtable<Integer, JLabel> labelTable = buildLabelTable();
        topSlider.setLabelTable(labelTable);
        topSlider.setMinorTickSpacing(1);
        topSlider.setPaintTicks(true);
        topSlider.setPaintLabels(true);

        // Slider for navigating within a set
        JSlider botSlider = new JSlider(JSlider.HORIZONTAL, currSetStartCount, currSetEndCount, 0);
        botSlider.setMinorTickSpacing(1);
        botSlider.setMajorTickSpacing(2);
        botSlider.setPaintTicks(true);
        botSlider.setPaintLabels(true);

        // Change Listeners

        topSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {

                // Status label
                int val = ((JSlider)e.getSource()).getValue();
                String set = labelTable.get(val).getText();
                statusLabel.setText("Set : " + set);

                updateCurrSetCounts(set);

                // Update bottom slider
                botSlider.setMinimum(currSetStartCount);
                botSlider.setMaximum(currSetEndCount);
                botSlider.setValue(currSetStartCount);
            }
        });

        sliderPanel.add(topSlider);
        sliderPanel.add(botSlider);

        scrubBarPanel.add(sliderPanel, BorderLayout.CENTER);
    }

    private Hashtable<Integer, JLabel> buildLabelTable() {

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        List<Map.Entry<String, Integer>> list = new ArrayList<>(pageTabCounts.entrySet());
        list.sort(Map.Entry.comparingByValue());

        int val = 0;
        for (Map.Entry<String, Integer> entry : list) {
            labelTable.put(val++, new JLabel(entry.getKey()));
        }

        return labelTable;
    }

    private JPanel getToolBarPanel() {
        JPanel toolBarPanel = new JPanel(new GridLayout(3, 1));

        JPanel topToolBarPanel = new JPanel(new GridLayout(1,3));

        // Scrub-bar Toolbar Buttons

        JButton syncButton = new JButton();
        syncButton.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/time_sync_flaticon.png")));
        syncButton.setToolTipText("Sync audio timing");

        JButton prevSetButton = new JButton();
        prevSetButton.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/prev_set_flaticon.png")));
        prevSetButton.setToolTipText("Previous set");

        JButton nextSetButton = new JButton();
        nextSetButton.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/next_set_flaticon.png")));
        nextSetButton.setToolTipText("Next set");

        topToolBarPanel.add(syncButton);
        topToolBarPanel.add(prevSetButton);
        topToolBarPanel.add(nextSetButton);

        JPanel midToolBarPanel = new JPanel(new GridLayout(1,3));

        JButton playPauseButton = new JButton();
        playPauseButton.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/play_flaticon.png")));
        playPauseButton.setToolTipText("Play/Pause playback");

        JButton prevCountButton = new JButton();
        prevCountButton.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/prev_count_flaticon.png")));
        prevCountButton.setToolTipText("Previous count");

        JButton nextCountButton = new JButton();
        nextCountButton.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/next_count_flaticon.png")));
        nextCountButton.setToolTipText("Next count");

        midToolBarPanel.add(playPauseButton);
        midToolBarPanel.add(prevCountButton);
        midToolBarPanel.add(nextCountButton);

        JPanel botToolBarPanel = new JPanel(new GridLayout(1,3));

        // Audio toggle
        this.audioCheckbox = new JCheckBox();
        this.audioCheckbox.setToolTipText("Toggle audio on/off");
        JLabel audioLabel = new JLabel();
        audioLabel.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/audio_flaticon.png")));
        JPanel audioPanel = new JPanel(new FlowLayout());
        audioPanel.add(this.audioCheckbox);
        audioPanel.add(audioLabel);

        // Fast playback toggle
        this.fastPlayCheckbox = new JCheckBox();
        this.fastPlayCheckbox.setToolTipText("Toggle fast playback on/off");
        JLabel fastPlayLabel = new JLabel();
        fastPlayLabel.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/fast_play_flaticon.png")));
        JPanel fastPlayPanel = new JPanel(new FlowLayout());
        fastPlayPanel.add(this.fastPlayCheckbox);
        fastPlayPanel.add(fastPlayLabel);

        // Full playback toggle
        this.fullPlayCheckbox = new JCheckBox();
        this.fullPlayCheckbox.setToolTipText("Toggle full playback on/off");
        JLabel fullPlayLabel = new JLabel();
        fullPlayLabel.setIcon(scaleImageIcon(
                new ImageIcon("./project/resources/icons/scrub-bar/double_arrow_flaticon.png")));
        JPanel fullPlayPanel = new JPanel(new FlowLayout());
        fullPlayPanel.add(this.fullPlayCheckbox);
        fullPlayPanel.add(fullPlayLabel);

        // Playback should always be done with synced times, because lighting effect params involve timing.
        //  Therefore, no option to disable synced time, for now...

        botToolBarPanel.add(audioPanel);
        botToolBarPanel.add(fastPlayPanel);
        botToolBarPanel.add(fullPlayPanel);

        toolBarPanel.add(topToolBarPanel);
        toolBarPanel.add(midToolBarPanel);
        toolBarPanel.add(botToolBarPanel);
        return toolBarPanel;
    }

    /**
     * Rescale ImageIcon to fit for toolbar icons, or for other purposes
     * @param imageIcon - ImageIcon object to rescale.
     * @return Altered ImageIcon with rescaled icon.
     */
    public ImageIcon scaleImageIcon(ImageIcon imageIcon, int width, int height) {
        Image image = imageIcon.getImage(); // transform it
        Image newImg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        return new ImageIcon(newImg);  // transform it back
    }

    // Overloaded
    private ImageIcon scaleImageIcon(ImageIcon imageIcon) {
        Image image = imageIcon.getImage(); // transform it
        Image newImg = image.getScaledInstance(15, 15, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        return new ImageIcon(newImg);  // transform it back
    }

    public JPanel getScrubBarPanel() {
        return scrubBarPanel;
    }

    public JCheckBox getAudioCheckbox() {
        return audioCheckbox;
    }

    public JCheckBox getFastPlayCheckbox() {
        return fastPlayCheckbox;
    }

    public JCheckBox getFullPlayCheckbox() {
        return fullPlayCheckbox;
    }

    /**
     * Will update currSetBeginCount and currSetEndCount.
     *
     * If the set's page tab is, e.g.,
     * "2", then consider "2A" or "3" as next.
     * "2A", then consider "2B" or "3" as next.
     * "2Z", then consider "3" as next.
     *
     * @param set e.g., "1", "2A", "5B"
     */
    private void updateCurrSetCounts(String set) {

        currSetStartCount = pageTabCounts.get(set);

        // There is no further page tab
        if (currSetStartCount == lastCount) {
            currSetEndCount = currSetStartCount;
            printSetStartEndCounts(set);
            return;
        }

        // Find end count of set
        Pattern digitPattern = Pattern.compile("\\d+"); // Digit, one or more
        Pattern letterPattern = Pattern.compile("\\D+"); // Non digit, one or more

        Matcher digitMatcher = digitPattern.matcher(set);
        Matcher letterMatcher = letterPattern.matcher(set);

        int setNum = digitMatcher.find() ? Integer.parseInt(digitMatcher.group()) : -1;
        char subSetChar = letterMatcher.find() ? letterMatcher.group().charAt(0) : '!';

        // The character is not 'Z'
        if (subSetChar != 'Z') {

            // There is a character
            if (subSetChar != '!') {

                // Consider existence of next sub-set (e.g., if "2A", consider "2B")
                subSetChar += 1;

                if (pageTabCounts.get(Integer.toString(setNum) + subSetChar) != null) {
                    currSetEndCount = pageTabCounts.get(Integer.toString(setNum) + subSetChar);
                    printSetStartEndCounts(set);
                    return;
                }
            }

            // There is NO character
            else {
                if (pageTabCounts.get(setNum + "A") != null) {
                    currSetEndCount = pageTabCounts.get(setNum + "A");
                    printSetStartEndCounts(set);
                    return;
                }
            }
        }

        currSetEndCount = pageTabCounts.get(Integer.toString(setNum + 1));
        printSetStartEndCounts(set);
    }

    public void printSetStartEndCounts(String set) {
        /*
        System.out.println("set = " + set);
        System.out.println("currSetStartCount = " + currSetStartCount);
        System.out.println("currSetEndCount = " + currSetEndCount);
        */
    }

    // For testing
    public static void main(String[] args) {

        // Run Swing programs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                // Dummy input
                Map<String, Integer> dummyData1 = new HashMap<>();
                dummyData1.put("1", 0); // Page tab 1 maps to count 0
                dummyData1.put("1A", 16); // Page tab 1A maps to count 16
                dummyData1.put("2", 32); // Page tab 2 maps to count 32
                dummyData1.put("2A", 48); // etc.
                dummyData1.put("3", 64);
                dummyData1.put("3A", 88);
                dummyData1.put("4", 96);
                dummyData1.put("4A", 112);
                dummyData1.put("4B", 128);
            }
        });
    }
}
