package org.emrick.project;

import org.emrick.project.audio.AudioPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrubBarGUI extends JComponent implements ActionListener {

    // String definitions
    private static final String PATH_SYNC_ICON = PathConverter.pathConverter("res/images/scrub/time_sync_flaticon.png", true);
    private static final String PATH_PREV_SET_ICON = PathConverter.pathConverter("res/images/scrub/prev_set_flaticon.png", true);
    private static final String PATH_NEXT_SET_ICON = PathConverter.pathConverter("res/images/scrub/next_set_flaticon.png", true);
    public static final String PATH_PLAY_ICON = PathConverter.pathConverter("res/images/scrub/play_flaticon.png", true);
    private static final String PATH_PAUSE_ICON = PathConverter.pathConverter("res/images/scrub/pause_flaticon.png", true);
    private static final String PATH_PREV_COUNT_ICON = PathConverter.pathConverter("res/images/scrub/prev_count_flaticon.png", true);
    private static final String PATH_NEXT_COUNT_ICON = PathConverter.pathConverter("res/images/scrub/next_count_flaticon.png", true);
    private static final String PATH_AUDIO_ICON = PathConverter.pathConverter("res/images/scrub/audio_flaticon.png", true);
    private static final String PATH_FAST_PLAY_ICON = PathConverter.pathConverter("res/images/scrub/fast_play_flaticon.png", true);
    private static final String PATH_FULL_PLAY_ICON = PathConverter.pathConverter("res/images/scrub/double_arrow_flaticon.png", true);

    // Swing
    private JPanel scrubBarPanel;
    private final JFrame parent;

    private ArrayList<AudioPlayer> audioPlayers;
    private AudioPlayer currAudioPlayer;

    // Status
    private long currTimeMSec;

    // Scrub Bars / Sliders
    private JSlider topSlider;

    private JSlider volumeSlider;

    // Scrub-Bar Status
    private boolean isReady; // Whether the drill is ready to play
    private boolean isPlaying; // Whether the drill is being played

    // Scrub-Bar Toolbar Buttons
    private JButton syncButton;
    private JButton prevSetButton;
    private JButton nextSetButton;
    private JButton playPauseButton;
    private JButton prevCountButton;
    private JButton nextCountButton;

    // Additional settings
    JComboBox<String> playbackSpeedsBox;
    private JCheckBox audioCheckbox;

    // Page Tabs / Counts
    private Map<String, Integer> pageTab2Count; // [pageTab]:[count] e.g., k:"2A", v:30
    private int lastCount;
    private int currSetStartCount;
    private int currSetEndCount;
    private final FootballFieldPanel footballFieldPanel;
    private int totalCounts; //total counts in the show

    // Listener
    private final ScrubBarListener scrubBarListener;
    private final SyncListener syncListener;
    private final ImageIcon PLAY_ICON;
    private final ImageIcon PAUSE_ICON;

    // Frame rate
    private double fps = 30;
    private double time = 0;
    private boolean useFps = false;
    private ArrayList<SyncTimeGUI.Pair> timeSync = null;

    // Volume

    public int volume = 50;

    public ScrubBarGUI(JFrame parent, ScrubBarListener scrubBarListener, SyncListener syncListener,
                       FootballFieldPanel footballFieldPanel, ArrayList<AudioPlayer> audioPlayers) {
        this.parent = parent;

        this.audioPlayers = audioPlayers;
        // Placeholder. E.g., When Emrick Designer is first opened, no project is loaded.
        this.pageTab2Count = new HashMap<>();
        this.pageTab2Count.put("1", 0);

        updateLastCount();
        updateCurrSetCounts("1");

        this.scrubBarListener = scrubBarListener;
        this.syncListener = syncListener;

        this.isReady = false;
        this.isPlaying = false;
        this.footballFieldPanel = footballFieldPanel;

        PLAY_ICON = scaleImageIcon(new ImageIcon(PATH_PLAY_ICON));
        PAUSE_ICON = scaleImageIcon(new ImageIcon(PATH_PAUSE_ICON));

        initialize();
    }

    public void updatePageTabCounts(Map<String, Integer> pageTabCounts, int totalCounts) {

        this.pageTab2Count = pageTabCounts;
        this.totalCounts = totalCounts;

        // There should exist at least a first Page Tab, for logic purposes
        if (pageTabCounts.isEmpty()) {
            System.out.println("Note: No page tabs found.");
            return;
        }

        // Can't find Page Tab 1
        else if (pageTabCounts.get("1-1") == null && pageTabCounts.get("1") == null) {
            System.out.println("Note: Can't find page tab 1.");
            return;
        }

        reinitialize();
    }

    /**
     * Upon receiving new pageTab2Count data, call this method to update the Scrub Bar.
     */
    private void reinitialize() {
        updateLastCount();
        updateCurrSetCounts(footballFieldPanel.drill.sets.get(0).label);

        // Debugging - Existing components cause UI bugging
        scrubBarPanel.removeAll();

        initialize();
    }

    /**
     * Update lastCount field. Important for managing display of the bottom slider.
     * Call this whenever the Scrub Bar receives a new set of Page Tab : Count data.
     * i.e., upon receiving new data for pageTabCount.
     */
    private void updateLastCount() {
        this.lastCount = Collections.max(
                pageTab2Count.entrySet(),
                Map.Entry.comparingByValue()
        ).getValue();
    }

    private void initialize() {

        scrubBarPanel = new JPanel(new BorderLayout(10, 10));

        // Toolbar Panel.
        JPanel toolBarPanel = getToolBarPanel();

        scrubBarPanel.add(toolBarPanel, BorderLayout.WEST);

        // Status Panel.
        // TODO: Add more info (like start/end times?)
        JPanel statusPanel = new JPanel(new GridLayout(4, 1));
        statusPanel.setPreferredSize(new Dimension(100, 1));
        JLabel statusLabel = new JLabel("Set: 0", JLabel.CENTER);
        JLabel timeLabel = new JLabel("0:00.000", JLabel.CENTER);

        String[] playbackFps = { "30 fps", "15 fps", "Count" };
        JComboBox<String> fpsChanger = new JComboBox<>(playbackFps);
        this.useFps = true;
        footballFieldPanel.setUseFps(useFps);
        this.fps = 30.0;
        fpsChanger.addActionListener(e -> {
            // Added: give user ability to not use FPS option
            String selected = (String) fpsChanger.getSelectedItem();
            assert(selected != null);
            if (selected.equals("Count")) {
                useFps = false;
                footballFieldPanel.setUseFps(useFps);
                return;
            }
            useFps = true;
            footballFieldPanel.setUseFps(useFps);
            String selectedFps = selected.split(" ")[0];
            this.fps = Double.parseDouble(selectedFps);
        });

        statusPanel.add(statusLabel);
        statusPanel.add(timeLabel);
        statusPanel.add(fpsChanger);

        scrubBarPanel.add(statusPanel, BorderLayout.EAST);

        JPanel sliderPanel = new JPanel(new BorderLayout());

        // Slider for navigating different sets
        topSlider = new JSlider(JSlider.HORIZONTAL,0, lastCount, 0);
        Hashtable<Integer, JLabel> labelTable = buildLabelTable();
        topSlider.setLabelTable(labelTable);
        topSlider.setMinorTickSpacing(1);
        topSlider.setPaintTicks(false);
        topSlider.setPaintLabels(true);

        // Change Listeners

        topSlider.addChangeListener(e -> {
            // Status label
            int val = ((JSlider)e.getSource()).getValue();
            String set = labelTable.get(getCurrentSetStart()).getText();
            statusLabel.setText("Set: " + set);
            if (!isPlaying) {
                scrubBarListener.onSetChange(getCurrentSetIndex());
                footballFieldPanel.setCurrentSetStartCount(getCurrentSetStart());
                footballFieldPanel.setCurrentCount(val);

                long currTimeMSec = scrubBarListener.onScrub();
                if (!isUseFps()) {
                    float pastSetTime = 0;
                    for (int i = 0; i < getCurrentSetIndex(); i++) {
                        pastSetTime += timeSync.get(i).getValue();
                    }
                    time = ((float) currTimeMSec + pastSetTime) / 1000;
                    scrubBarListener.onTimeChange((long) ((time - pastSetTime) * 1000));
                }
                timeLabel.setText(TimeManager.getFormattedTime(currTimeMSec));

                setPlaybackTime();
            } else {
                timeLabel.setText(TimeManager.getFormattedTime((long)(time * 1000)));
            }
        });

        // Volume Slider
        volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        volumeSlider.addChangeListener(e -> {
            volume = volumeSlider.getValue();
            scrubBarListener.onVolumeChange();
        });

        sliderPanel.add(topSlider, BorderLayout.CENTER);
        sliderPanel.add(volumeSlider, BorderLayout.SOUTH);

        scrubBarPanel.add(sliderPanel, BorderLayout.CENTER);
    }

    public boolean setScrub(int count) {
        if (!isPlaying) {
            topSlider.setValue(count);
            scrubBarListener.onScrub();
            return true;
        }
        return false;
    }

    public void setPlaybackTime() {
        //TODO rewrite
        float setSyncDuration = timeSync.get(getCurrentSetIndex()).getValue();
        float setDuration = this.getCurrSetDuration(); // in counts
        float pastSetTime = 0;
        for (int i = 0; i < getCurrentSetIndex(); i++) {
            pastSetTime += timeSync.get(i).getValue();
        }
        time = (float) (topSlider.getValue() - getCurrentSetStart()) / setDuration * setSyncDuration + pastSetTime;
        scrubBarListener.onTimeChange((long) ((Math.round(time * 1000.0) / 1000.0 - pastSetTime) * 1000));
        double ratio = (time - pastSetTime) / setSyncDuration;
        scrubBarListener.onSetChange(getCurrentSetIndex());
        footballFieldPanel.setCurrentSetRatio(Math.min(ratio, 1));
        footballFieldPanel.repaint();
    }

    public boolean nextStep(double playbackSpeed) {
        double step = (1 / fps) / playbackSpeed;

        time += step;
        float pastSetTime = 0;
        int currentSetIndex = getCurrentSetIndex();
        for (int i = 0; i < currentSetIndex; i++) {
            pastSetTime += timeSync.get(i).getValue();
        }

        float setSyncDuration = timeSync.get(getCurrentSetIndex()).getValue();
        if (setSyncDuration == 0) {
            nextSet();
            currentSetIndex = getCurrentSetIndex();
            setSyncDuration = timeSync.get(getCurrentSetIndex()).getValue();
        }
        float setDuration = getCurrSetDuration(); // in counts

        double ratio = (time - pastSetTime) / setSyncDuration;
        double setCount = ratio * setDuration;
        topSlider.setValue(getCurrentSetStart() + (int) Math.floor(setCount));

        // account for adjusting time into another set
        if (currentSetIndex != getCurrentSetIndex()) {
            pastSetTime += timeSync.get(currentSetIndex).getValue();
            setSyncDuration = timeSync.get(getCurrentSetIndex()).getValue();
            ratio = (time - pastSetTime) / setSyncDuration;
        }
        scrubBarListener.onTimeChange((long) ((time - pastSetTime) * 1000));

        scrubBarListener.onSetChange(getCurrentSetIndex());
        footballFieldPanel.setCurrentSetRatio(Math.min(ratio, 1));
        footballFieldPanel.repaint();


        if (this.isAtLastSet()) {
            this.setIsPlayingPlay();
            System.out.println("we've stopped!");
            return true;
        }

        return false;
    }

    private Hashtable<Integer, JLabel> buildLabelTable() {

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        List<Map.Entry<String, Integer>> list = sortMap(pageTab2Count);

        for (Map.Entry<String, Integer> entry : list) {
            JLabel label = new JLabel(entry.getKey());
            label.setFont(new Font(Font.SERIF, Font.PLAIN, 8));
            labelTable.put(entry.getValue(), label);
        }

        return labelTable;
    }

    /**
     * Takes in a map of [String]:[Integer] entries and returns a list of those entries, sorted by the
     * value [Integer] in ascending order. Useful for a variety of situations, not only within this class.
     *
     * @param map - The map of [String]:[Integer] entries
     * @return a list of map entries, sorted by the value [Integer] in ascending order.
     */
    public static List<Map.Entry<String, Integer>> sortMap(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        return list;
    }

    private JPanel getToolBarPanel() {
        JPanel toolBarPanel = new JPanel(new GridLayout(3, 1));

        JPanel topToolBarPanel = new JPanel(new GridLayout(1,3));

        // Scrub-bar Toolbar Buttons

        // Sync
        syncButton = new JButton();
        syncButton.setIcon(scaleImageIcon(new ImageIcon(PATH_SYNC_ICON)));
        syncButton.setToolTipText("Sync audio timing");
        syncButton.addActionListener(this);

        // Previous Set
        prevSetButton = new JButton();
        prevSetButton.setIcon(scaleImageIcon(new ImageIcon(PATH_PREV_SET_ICON)));
        prevSetButton.setToolTipText("Previous set");
        prevSetButton.addActionListener(this);

        // Next Set
        nextSetButton = new JButton();
        nextSetButton.setIcon(scaleImageIcon(new ImageIcon(PATH_NEXT_SET_ICON)));
        nextSetButton.setToolTipText("Next set");
        nextSetButton.addActionListener(this);

        topToolBarPanel.add(syncButton);
        topToolBarPanel.add(prevSetButton);
        topToolBarPanel.add(nextSetButton);

        JPanel midToolBarPanel = new JPanel(new GridLayout(1,3));

        // Play or Pause
        playPauseButton = new JButton();
        playPauseButton.setIcon(scaleImageIcon(new ImageIcon(PATH_PLAY_ICON)));
        playPauseButton.setToolTipText("Play/Pause playback");
        playPauseButton.addActionListener(this);

        // Previous Count
        prevCountButton = new JButton();
        prevCountButton.setIcon(scaleImageIcon(new ImageIcon(PATH_PREV_COUNT_ICON)));
        prevCountButton.setToolTipText("Previous count");
        prevCountButton.addActionListener(this);

        // Next Count
        nextCountButton = new JButton();
        nextCountButton.setIcon(scaleImageIcon(new ImageIcon(PATH_NEXT_COUNT_ICON)));
        nextCountButton.setToolTipText("Next count");
        nextCountButton.addActionListener(this);

        midToolBarPanel.add(playPauseButton);
        midToolBarPanel.add(prevCountButton);
        midToolBarPanel.add(nextCountButton);

        JPanel botToolBarPanel = new JPanel(new GridLayout(1,3));

        // Audio
        this.audioCheckbox = new JCheckBox();
//        this.audioCheckbox.setSelected(true);
        this.audioCheckbox.setToolTipText("Toggle audio on/off");
        JLabel audioLabel = new JLabel();
        audioLabel.setIcon(scaleImageIcon(new ImageIcon(PATH_AUDIO_ICON)));
        JPanel audioPanel = new JPanel(new FlowLayout());
        audioPanel.add(this.audioCheckbox);
        audioPanel.add(audioLabel);

        // Speed
        String[] playbackSpeeds = { "0.5", "Normal", "1.5", "2.0", "4.0" };
        playbackSpeedsBox = new JComboBox<>(playbackSpeeds);
        playbackSpeedsBox.setToolTipText("Select playback speed");
        playbackSpeedsBox.setSelectedIndex(1);
        playbackSpeedsBox.addActionListener(this);
        JPanel playbackSpeedsPanel = new JPanel();
        playbackSpeedsPanel.add(playbackSpeedsBox);

        botToolBarPanel.add(playbackSpeedsPanel);
        botToolBarPanel.add(audioPanel);

        toolBarPanel.add(topToolBarPanel);
        toolBarPanel.add(midToolBarPanel);
        toolBarPanel.add(botToolBarPanel);

        return toolBarPanel;
    }

    /**
     * Rescale ImageIcon to fit for toolbar icons, or for other purposes
     *
     * @param imageIcon - ImageIcon object to rescale.
     * @return Altered ImageIcon with rescaled icon.
     */
    public static ImageIcon scaleImageIcon(ImageIcon imageIcon, int width, int height) {
        Image image = imageIcon.getImage(); // transform it
        Image newImg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        return new ImageIcon(newImg);  // transform it back
    }

    // Overloaded
    private ImageIcon scaleImageIcon(ImageIcon imageIcon) {
        return scaleImageIcon(imageIcon, 16, 16);
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    public JPanel getScrubBarPanel() {
        return scrubBarPanel;
    }

    public JCheckBox getAudioCheckbox() {
        return audioCheckbox;
    }

    public boolean isUseFps() {
        return useFps;
    }

    /**
     * Call this method to update currSetBeginCount and currSetEndCount, which are important
     * for displaying Count values on the bottom Scrub Bar.
     * Provide the Page Tab of the Set (a Page Tab denotes the start of a new Set).
     * Implementation Details:
     * If you provide:
     * "2", then consider "2A" or "3" as next Page Tab,
     * "2A", then consider "2B" or "3" as next Page Tab,
     * "2Z", then consider "3" as next Page Tab,
     * By knowing the current Page Tab and next Page Tab, we find the beginning and end Counts
     * for the given set. (Technically the parameter is the Page Tab String).
     *
     * @param set - For example, "1", "2A", "5B"
     */
    private void updateCurrSetCounts(String set) {

        currSetStartCount = pageTab2Count.get(set);

        // There is no further page tab
        if (currSetStartCount == lastCount) {
            currSetEndCount = currSetStartCount;
            return;
        }

        ArrayList<String> sets = new ArrayList<>(pageTab2Count.keySet());
        sets.sort((o1,o2) -> {
            String[] thisComponents = o1.split("-");
            String[] thatComponents = o2.split("-");
            if (thisComponents.length != thatComponents.length) {
                if (thisComponents.length < thatComponents.length) {
                    return -1;
                } else {
                    return 1;
                }
            }
            int thisSetIndex;
            int thatSetIndex;
            if (thisComponents.length > 1) {
                int thisMovementIndex = Integer.parseInt(thisComponents[0]);
                int thatMovementIndex = Integer.parseInt(thatComponents[0]);
                if (thisMovementIndex != thatMovementIndex) {
                    return thisMovementIndex - thatMovementIndex;
                }
                String thisSetLabel = thisComponents[1].replaceAll("[^0-9.]", "");
                String thatSetLabel = thatComponents[1].replaceAll("[^0-9.]", "");
                thisSetIndex = Integer.parseInt(thisSetLabel);
                thatSetIndex = Integer.parseInt(thatSetLabel);
                if (thisSetIndex != thatSetIndex) {
                    return thisSetIndex - thatSetIndex;
                } else {
                    return thisSetLabel.compareTo(thatSetLabel);
                }
            }
            String thisSetLabel = thisComponents[0].replaceAll("[^0-9.]", "");
            String thatSetLabel = thatComponents[0].replaceAll("[^0-9.]", "");
            thisSetIndex = Integer.parseInt(thisSetLabel);
            thatSetIndex = Integer.parseInt(thatSetLabel);
            if (thisSetIndex != thatSetIndex) {
                return thisSetIndex - thatSetIndex;
            } else {
                return thisSetLabel.compareTo(thatSetLabel);
            }
        });
        int index = sets.indexOf(set) + 1;
        currSetEndCount = pageTab2Count.get(sets.get(index));
    }



    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public void actionPerformed(ActionEvent e)   {

        if (e.getSource().equals(playPauseButton)) {
            if (!isReady) {
                return;
            }
            if (isPlaying) {
                // Pause
                if (scrubBarListener.onPause()) {
                    setIsPlayingPlay();
                    isPlaying = false;
                }
            } else {
                // Play
                if (scrubBarListener.onPlay()) {
                    setIsPlayingPause();
                    isPlaying = true;
                }
            }
        }
        else if (e.getSource().equals(prevSetButton)) {
            prevSet();
        }
        else if (e.getSource().equals(nextSetButton)) {
            nextSet();
        }
        else if (e.getSource().equals(prevCountButton)) {
            prevCount();
        }
        else if (e.getSource().equals(nextCountButton)) {
            nextCount();
        }
        else if (e.getSource().equals(syncButton)) {
            System.out.println(isReady);
            if (isReady) {
                new SyncTimeGUI(parent, syncListener, pageTab2Count, audioPlayers.get(0), totalCounts);
            }
        }
        else if (e.getSource().equals(playbackSpeedsBox)) {
            String selectedItem = (String) playbackSpeedsBox.getSelectedItem();
            assert selectedItem != null;
            if (selectedItem.equals("Normal")) {
                selectedItem = "1.0";
            }
            float playbackSpeed = Float.parseFloat(selectedItem);
            scrubBarListener.onSpeedChange(playbackSpeed);
        }

    }

    // these might be misleading, fix?
    public void setIsPlayingPause() {
        playPauseButton.setIcon(PAUSE_ICON);
        isPlaying = true;

        System.out.println("ScrubBarGUI: isPlaying = " + isPlaying + ", time = " + scrubBarListener.onScrub() / 1000.0);
    }
    public void setIsPlayingPlay() {
        playPauseButton.setIcon(PLAY_ICON);
        isPlaying = false;

        System.out.println("ScrubBarGUI: isPlaying = " + isPlaying + ", time = " + scrubBarListener.onScrub() / 1000.0);
    }

    public double getTime() {
        return time;
    }

    public void prevSet() {
        Iterator<Integer> iterator = pageTab2Count.values().iterator();
        ArrayList<Integer> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        list.sort(Comparator.comparingInt(Integer::intValue));
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i) <= topSlider.getValue() && list.get(i+1) > topSlider.getValue()) {
                if (topSlider.getValue() == list.get(i)) {
                    topSlider.setValue(list.get(i - 1));
                } else {
                    topSlider.setValue(list.get(i));
                }
                return;
            }
        }
        topSlider.setValue(list.get(list.size() - 2));
    }

    public void nextSet() {
        Iterator<Integer> iterator = pageTab2Count.values().iterator();
        ArrayList<Integer> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        list.sort(Comparator.comparingInt(Integer::intValue));
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i) <= topSlider.getValue() && list.get(i+1) > topSlider.getValue()) {
                topSlider.setValue(list.get(i+1));
                break;
            }
        }
    }

    public void prevCount() {
        topSlider.setValue(topSlider.getValue() - 1);
    }

    public void nextCount() {
        topSlider.setValue(topSlider.getValue() + 1);
    }

    public double getFps() {
        return fps;
    }

    public int getCurrentSetIndex() {
        Iterator<Integer> iterator = pageTab2Count.values().iterator();
        ArrayList<Integer> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        list.sort(Comparator.comparingInt(Integer::intValue));
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i) <= topSlider.getValue() && list.get(i+1) > topSlider.getValue()) {
                return i;
            }
        }
        return list.size()-1;
    }

    public int getCurrentSetStart() {
        Iterator<Integer> iterator = pageTab2Count.values().iterator();
        ArrayList<Integer> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        list.sort(Comparator.comparingInt(Integer::intValue));
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i) <= topSlider.getValue() && list.get(i+1) > topSlider.getValue()) {
                return list.get(i);
            }
        }
        return list.get(list.size()-1);
    }


    public boolean isAtLastSet() {
        return topSlider.getValue() == topSlider.getMaximum();
    }

    public boolean isAtStartOfSet() {
        Iterator<Integer> iterator = pageTab2Count.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(topSlider.getValue())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAtFirstSet() {
        Iterator<Integer> iterator = pageTab2Count.values().iterator();
        ArrayList<Integer> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        list.sort(Comparator.comparingInt(Integer::intValue));
        return topSlider.getValue() < list.get(1);
    }

    public int getCurrSetDuration() {
        Iterator<Integer> iterator = pageTab2Count.values().iterator();
        ArrayList<Integer> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        list.sort(Comparator.comparingInt(Integer::intValue));
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i) <= topSlider.getValue() && list.get(i+1) > topSlider.getValue()) {
                return list.get(i+1) - list.get(i);
            }
        }
        return 0;
    }

    public void setTimeSync(ArrayList<SyncTimeGUI.Pair> timeSync) {
        this.timeSync = timeSync;
        this.timeSync.sort((o1, o2) -> {
            String[] thisComponents = o1.getKey().split("-");
            String[] thatComponents = o2.getKey().split("-");
            if (thisComponents.length != thatComponents.length) {
                if (thisComponents.length < thatComponents.length) {
                    return -1;
                } else {
                    return 1;
                }
            }
            int thisSetIndex;
            int thatSetIndex;
            if (thisComponents.length > 1) {
                int thisMovementIndex = Integer.parseInt(thisComponents[0]);
                int thatMovementIndex = Integer.parseInt(thatComponents[0]);
                if (thisMovementIndex != thatMovementIndex) {
                    return thisMovementIndex - thatMovementIndex;
                }
                String thisSetLabel = thisComponents[1].replaceAll("[^0-9.]", "");
                String thatSetLabel = thatComponents[1].replaceAll("[^0-9.]", "");
                thisSetIndex = Integer.parseInt(thisSetLabel);
                thatSetIndex = Integer.parseInt(thatSetLabel);
                if (thisSetIndex != thatSetIndex) {
                    return thisSetIndex - thatSetIndex;
                } else {
                    return thisSetLabel.compareTo(thatSetLabel);
                }
            }
            String thisSetLabel = thisComponents[0].replaceAll("[^0-9.]", "");
            String thatSetLabel = thatComponents[0].replaceAll("[^0-9.]", "");
            thisSetIndex = Integer.parseInt(thisSetLabel);
            thatSetIndex = Integer.parseInt(thatSetLabel);
            if (thisSetIndex != thatSetIndex) {
                return thisSetIndex - thatSetIndex;
            } else {
                return thisSetLabel.compareTo(thatSetLabel);
            }
        });
    }
    public void setAudioPlayer(ArrayList<AudioPlayer> audioPlayers) {
        this.audioPlayers = audioPlayers;
    }
    public void setCurrAudioPlayer(AudioPlayer ap) {
        this.currAudioPlayer = ap;
    }
    public void setTime(int time) {
        this.time = time;
    }

    public JButton getSyncButton() {
        return syncButton;
    }

}
