package org.emrick.project;

import org.emrick.project.effect.Effect;
import org.emrick.project.effect.RFTrigger;
import org.emrick.project.effect.TimelineEvent;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.util.*;
import java.util.List;

public class TimelineGUI {
    private JPanel mainPanel;
    private ArrayList<Effect> effects;
    private final ArrayList<RFTrigger> triggers;
    private JScrollPane timelineScrollPane;
    private JPanel timelinePanel;
    List<List<TimeRange>> rows;
    
    // Zoom controls
    private JPanel zoomPanel;
    private static double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_STEP = 0.1;
    
    // Base dimensions
    private static final int ROW_HEIGHT = 70;
    private static final int TRIGGER_ROW_HEIGHT = 60;
    private static final int PIXELS_PER_SECOND = 20; // Base scale: 20 pixels per second at zoom 1.0
    
    // Track the total duration for scaling
    private double totalDurationMSec;
    private static double curMSec;

    public TimelineGUI(ArrayList<Effect> effects, HashMap<Integer, RFTrigger> count2RFTrigger) {
        this.effects = effects;
        if (this.effects == null) {
            this.effects = new ArrayList<>();
        }
        triggers = new ArrayList<>();
        ArrayList<Map.Entry<Long, Map.Entry<TimelineEvent, JPanel>>> timelineEvents = new ArrayList<>();
        
        // Sort and store all timeline events
        for (Map.Entry<Integer, RFTrigger> entry : count2RFTrigger.entrySet()) {
            RFTrigger trigger = entry.getValue();
            triggers.add(trigger);
            timelineEvents.add(new AbstractMap.SimpleEntry<>(
                trigger.getTimestampMillis(),
                new AbstractMap.SimpleEntry<>(trigger, trigger.getTimelineWidget())
            ));
        }
        
        // Sort triggers by timestamp
        triggers.sort(Comparator.comparingLong(RFTrigger::getTimestampMillis));
        
        for (Effect effect : effects) {
            timelineEvents.add(new AbstractMap.SimpleEntry<>(
                effect.getStartTimeMSec() + effect.getDuration().toMillis(),
                new AbstractMap.SimpleEntry<>(effect, effect.getTimelineWidget())
            ));
        }
        
        // Sort timeline events by timestamp
        timelineEvents.sort(Comparator.comparing(Map.Entry::getKey));
        
        // Set total duration from the last event
        totalDurationMSec = timelineEvents.isEmpty() ? 1000 : 
                           timelineEvents.get(timelineEvents.size() - 1).getKey() + 1000;
        
        createTimelinePane();
        createZoomControls();

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(zoomPanel, BorderLayout.NORTH);
        mainPanel.add(timelineScrollPane, BorderLayout.CENTER);

        scrubTimeline(curMSec);
        System.out.println("curMSec: " + curMSec);
    }

    private void createZoomControls() {
        zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton zoomInButton = new JButton("+");
        JButton zoomOutButton = new JButton("-");
        JButton resetZoomButton = new JButton("Reset Zoom");
        JLabel zoomLabel = new JLabel(String.format("Zoom: %.1fx", zoomFactor));
        
        zoomInButton.addActionListener(e -> {
            if (zoomFactor + ZOOM_STEP < MAX_ZOOM) {
                zoomFactor += ZOOM_STEP;
                updateZoom(zoomLabel);
            }
        });
        
        zoomOutButton.addActionListener(e -> {
            if (zoomFactor - ZOOM_STEP > MIN_ZOOM) {
                zoomFactor -= ZOOM_STEP;
                updateZoom(zoomLabel);
            }
        });
        
        resetZoomButton.addActionListener(e -> {
            zoomFactor = 1.0;
            updateZoom(zoomLabel);
        });
        
        zoomPanel.add(zoomOutButton);
        zoomPanel.add(zoomLabel);
        zoomPanel.add(zoomInButton);
        zoomPanel.add(resetZoomButton);
    }

    private void updateZoom(JLabel zoomLabel) {
        zoomLabel.setText(String.format("Zoom: %.1fx", zoomFactor));
        updateTimelineLayout();
        scrubTimeline(curMSec);
    }

    private void updateTimelineLayout() {
        int width = calculateTimelineWidth();
        timelinePanel.setPreferredSize(new Dimension(width, TRIGGER_ROW_HEIGHT + ROW_HEIGHT));
        updateComponentPositions();
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }

    private void updateComponentPositions() {
        timelinePanel.removeAll();
        
        // Add RF Triggers (first row)
        ArrayList<RFTrigger> sortedTriggers = new ArrayList<>(triggers);
        sortedTriggers.sort(Comparator.comparingLong(RFTrigger::getTimestampMillis));
        
        for (int i = 0; i < sortedTriggers.size(); i++) {
            RFTrigger trigger = sortedTriggers.get(i);
            JPanel triggerWidget = trigger.getTimelineWidget();
            int xPosition = calculateXPosition(trigger.getTimestampMillis());
            
            // Calculate width to next trigger or end
            int width;
            if (i < sortedTriggers.size() - 1) {
                long nextTriggerTime = sortedTriggers.get(i + 1).getTimestampMillis();
                width = calculateXPosition(nextTriggerTime) - xPosition;
            } else {
                width = calculateXPosition(totalDurationMSec) - xPosition;
            }
            
            // Ensure minimum width and prevent overlap
            width = width - 2; // Subtract 2 pixels to prevent overlap
            
            triggerWidget.setBounds(xPosition, 0, width, TRIGGER_ROW_HEIGHT);
            timelinePanel.add(triggerWidget);
        }
        
        // Add Effects (subsequent rows)
        List<List<TimeRange>> rows = new ArrayList<>();
        rows.add(new ArrayList<>()); // First effect row
        
        ArrayList<Effect> sortedEffects = new ArrayList<>(effects);
        sortedEffects.sort(Comparator.comparingLong(Effect::getStartTimeMSec));
        
        for (Effect effect : sortedEffects) {
            TimeRange effectRange = new TimeRange(
                effect.getStartTimeMSec(),
                effect.getStartTimeMSec() + effect.getDuration().toMillis()
            );
            
            int rowIndex = findAvailableRow(rows, effectRange);
            
            JPanel effectWidget = effect.getTimelineWidget();
            int xPosition = calculateXPosition(effect.getStartTimeMSec());
            int width = calculateEffectWidth(effect);
            int yPosition = TRIGGER_ROW_HEIGHT + (rowIndex * ROW_HEIGHT);
            
            effectWidget.setBounds(xPosition, yPosition, width, ROW_HEIGHT - 2);
            timelinePanel.add(effectWidget);
            
            while (rows.size() <= rowIndex) {
                rows.add(new ArrayList<>());
            }
            rows.get(rowIndex).add(effectRange);
        }
        timelinePanel.setPreferredSize(new Dimension(calculateTimelineWidth(), TRIGGER_ROW_HEIGHT + rows.size() * ROW_HEIGHT));
    }

    private void createTimelinePane() {
        timelinePanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
            
            @Override
            protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                
                // Draw the redline at current time position after all children are drawn
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red
                g2d.setStroke(new BasicStroke(2.0f));
                
                int xPosition = calculateXPosition(curMSec);
                g2d.drawLine(xPosition, 0, xPosition, getHeight());
            }
        };
        timelinePanel.setPreferredSize(new Dimension(calculateTimelineWidth(),  TRIGGER_ROW_HEIGHT + ROW_HEIGHT));
        updateComponentPositions();

        timelineScrollPane = new JScrollPane(timelinePanel);
        timelineScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        timelineScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        timelineScrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    private int calculateXPosition(double timestampMillis) {
        return (int)((timestampMillis * PIXELS_PER_SECOND * zoomFactor) / 1000);
    }

    private int calculateEffectWidth(Effect effect) {
        long durationMillis = effect.getDuration().toMillis();
        return (int)((durationMillis * PIXELS_PER_SECOND * zoomFactor) / 1000);
    }

    private int calculateTimelineWidth() {
        return (int)((totalDurationMSec * PIXELS_PER_SECOND * zoomFactor) / 1000);
    }

    private int findAvailableRow(List<List<TimeRange>> rows, TimeRange newRange) {
        for (int i = 0; i < rows.size(); i++) {
            if (isRowAvailable(rows.get(i), newRange)) {
                return i;
            }
        }
        rows.add(new ArrayList<>());
        return rows.size() - 1;
    }

    private boolean isRowAvailable(List<TimeRange> row, TimeRange newRange) {
        for (TimeRange range : row) {
            if (range.overlaps(newRange)) {
                return false;
            }
        }
        return true;
    }

    public JPanel getTimelineScrollPane() {
        return mainPanel;
    }

    private static class TimeRange {
        long start;
        long end;
        
        TimeRange(long start, long end) {
            this.start = start;
            this.end = end;
        }
        
        boolean overlaps(TimeRange other) {
            return !(this.end <= other.start || other.end <= this.start);
        }
    }

    public void scrubTimeline(double ms) {
        // Calculate the x position for the given time
        int xPosition = calculateXPosition(ms);
        // Get the viewport width
        int scrollPosition = (int) Math.max(0, xPosition - (PIXELS_PER_SECOND * 20));
        curMSec = ms;
        
        // Update the scroll position and redraw the timeline
        SwingUtilities.invokeLater(() -> {
            timelineScrollPane.getHorizontalScrollBar().setValue(scrollPosition);
            timelinePanel.repaint(); // Add this line to trigger redraw
        });
    }

    public static void setCurMSec(long curMSec) {
        TimelineGUI.curMSec = curMSec;
        System.out.println("TimelineGUI: setCurMSec = " + curMSec);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Timeline Demo");
        frame.setPreferredSize(new Dimension(900, 400));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        ArrayList<Effect> effects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long startTime = Duration.ofSeconds(i * 2).toMillis();
            Duration duration = Duration.ofSeconds(3);
            Duration delay = Duration.ofMillis(100);
            Duration timeout = Duration.ofSeconds(5);
            
            Effect e = new Effect(startTime, 
                                Color.GREEN, Color.RED,
                                delay, duration, timeout,
                                true, true, true, true, 0);
            effects.add(e);
        }

        // Add an overlapping effect to test row placement
        effects.add(new Effect(Duration.ofSeconds(1).toMillis(),
                             Color.BLUE, Color.YELLOW,
                             Duration.ZERO, Duration.ofSeconds(4), Duration.ofSeconds(5),
                             true, true, false, true, 0));

        HashMap<Integer, RFTrigger> triggers = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            RFTrigger t = new RFTrigger(i, 
                                      Duration.ofSeconds(i * 3).toMillis(),
                                      "Trigger " + i,
                                      "Description " + i,
                                      "Cue " + i);
            triggers.put(i, t);
        }

        TimelineGUI timelineGUI = new TimelineGUI(effects, triggers);
        frame.add(timelineGUI.getTimelineScrollPane());

        frame.pack();
        frame.setVisible(true);
    }
}
