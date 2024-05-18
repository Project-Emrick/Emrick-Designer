package org.emrick.project;

import org.emrick.project.effect.Effect;
import org.emrick.project.effect.RFTrigger;
import org.emrick.project.effect.TimelineEvent;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.util.*;

public class TimelineGUI {

    private ArrayList<Effect> effects;
    private final ArrayList<RFTrigger> triggers;
    private JScrollPane timelineScrollPane;

    public TimelineGUI(ArrayList<Effect> effects, HashMap<Integer, RFTrigger> count2RFTrigger) {
        this.effects = effects;
        if (this.effects == null) {
            this.effects = new ArrayList<>();
        }
        triggers = new ArrayList<>();
        for (Map.Entry<Integer, RFTrigger> entry : count2RFTrigger.entrySet()) {
            triggers.add(entry.getValue());
        }
        createTimelinePane();
    }

    private void createTimelinePane() {

        // Store the effects and triggers into common timeline map
        ArrayList<Map.Entry<Long, TimelineEvent>> timelineEvents = new ArrayList<>();
        for (RFTrigger t : triggers) {
            timelineEvents.add(new AbstractMap.SimpleEntry<>(t.getTimestampMillis(), t));
        }
        for (Effect e : effects) {
            timelineEvents.add(new AbstractMap.SimpleEntry<>(e.getStartTimeMSec(), e));
        }

        // Sort by timestamp
        sortMap(timelineEvents);
        // Add the effect and trigger widgets to the scroll pane
        JPanel timelinePanel = new JPanel(new FlowLayout());
        for (Map.Entry<Long, TimelineEvent> event : timelineEvents) {
            timelinePanel.add(event.getValue().getTimelineWidget());
        }
        timelineScrollPane = new JScrollPane(timelinePanel);
        timelineScrollPane.setBorder(BorderFactory.createEmptyBorder());
        timelineScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public static void sortMap(ArrayList<Map.Entry<Long, TimelineEvent>> timelineEvents) {
        timelineEvents.sort(Map.Entry.comparingByKey());
    }

    public JScrollPane getTimelineScrollPane() {
        return timelineScrollPane;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setPreferredSize(new Dimension(120, 120));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        long startTimeMSec = Duration.ofMillis(500).toMillis();

        Color startColor = new Color(0, 255, 0);
        Color endColor = new Color(255, 0, 0);

        Duration delay = Duration.ofSeconds(1).plusMillis(105);
        Duration duration = Duration.ofSeconds(2).plusMillis(205);
        Duration timeout = Duration.ofSeconds(3).plusMillis(305);

        boolean TIME_GRADIENT = true;
        boolean SET_TIMEOUT = true;
        boolean DO_DELAY = true;
        boolean INSTANT_COLOR = true;

        // Create dummy effects for display
        ArrayList<Effect> effects = new ArrayList<>();
        for (int i = 0; i < 5; i += 1) {
            Effect e = new Effect(startTimeMSec + i, startColor, endColor, delay, duration, timeout, TIME_GRADIENT,
                    SET_TIMEOUT, DO_DELAY, INSTANT_COLOR, 0);
            effects.add(e);
        }

        // Create dummy triggers for display
        HashMap<Integer, RFTrigger> triggers = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            RFTrigger t = new RFTrigger(i, startTimeMSec + i);
            triggers.put(i, t);
        }

        TimelineGUI timelineGUI = new TimelineGUI(effects, triggers);
        frame.add(timelineGUI.getTimelineScrollPane());

        frame.pack();
        frame.setVisible(true);
    }

}
