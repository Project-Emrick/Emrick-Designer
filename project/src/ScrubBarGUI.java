import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrubBarGUI {

    JPanel scrubBarPanel;
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
        topSlider.setPaintLabels(true);

        // Slider for navigating within a set
        JSlider botSlider = new JSlider(JSlider.HORIZONTAL, currSetStartCount, currSetEndCount, 0);
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

    private static JPanel getToolBarPanel() {
        JPanel toolBarPanel = new JPanel(new GridLayout(2, 1));

        JPanel topToolBarPanel = new JPanel(new BorderLayout());

        JButton backButton1 = new JButton("Prev");
        JButton playPauseButton1 = new JButton("Play");
        JButton forwardButton1 = new JButton("Next");

        topToolBarPanel.add(backButton1, BorderLayout.WEST);
        topToolBarPanel.add(playPauseButton1, BorderLayout.CENTER);
        topToolBarPanel.add(forwardButton1, BorderLayout.EAST);

        JPanel botToolBarPanel = new JPanel(new BorderLayout());

        JButton backButton2 = new JButton("Prev");
        JButton playPauseButton2 = new JButton("Play");
        JButton forwardButton2 = new JButton("Next");

        botToolBarPanel.add(backButton2, BorderLayout.WEST);
        botToolBarPanel.add(playPauseButton2, BorderLayout.CENTER);
        botToolBarPanel.add(forwardButton2, BorderLayout.EAST);

        toolBarPanel.add(topToolBarPanel);
        toolBarPanel.add(botToolBarPanel);
        return toolBarPanel;
    }

    public JPanel getScrubBarPanel() {
        return scrubBarPanel;
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
