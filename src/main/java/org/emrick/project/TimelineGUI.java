package org.emrick.project;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.emrick.project.SyncTimeGUI.Pair;
import org.emrick.project.effect.Effect;
import org.emrick.project.effect.RFTrigger;
import org.emrick.project.effect.TimelineEvent;

public class TimelineGUI {
    private final JPanel mainPanel;
    private ArrayList<Effect> effects;
    private final ArrayList<RFTrigger> triggers;
    private JScrollPane timelineScrollPane;
    private JPanel timelinePanel;
    
    // Button Groups for selection
    private ButtonGroup effectsButtonGroup;
    private ButtonGroup triggersButtonGroup;
    
    // Zoom controls
    private JPanel zoomPanel;
    private JLabel zoomLabel;
    private static double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;
    private static final double ZOOM_STEP = 0.1;
    
    // Base dimensions
    private static final int ROW_HEIGHT = 70;
    private static final int TRIGGER_ROW_HEIGHT = 60;
    private static final int PIXELS_PER_COUNT = 10; // Base scale: 10 pixels per count at zoom 1.0
    // Track the count position instead of time
    private final double totalDurationMSec; // Keeping for compatibility
    private static double curMS = 0;          // Keeping for compatibility
    private static double curCount = 0;  // Current count position with fractional part
    private int maxCount;                   // Maximum count in the timeline
    
    // Scrub bar component
    private TimelineScrubBar scrubBar;

    private TimeManager timeManager;
    private TimelineListener timelineListener;

    public TimelineGUI(ArrayList<Effect> effects, HashMap<Integer, RFTrigger> count2RFTrigger, TimeManager timeManager) {
        this.timeManager = timeManager;
        this.effects = effects;
        if (this.effects == null) {
            this.effects = new ArrayList<>();
        }
        triggers = new ArrayList<>();
        // Initialize button groups
        effectsButtonGroup = new ButtonGroup();
        triggersButtonGroup = new ButtonGroup();
        
        ArrayList<Map.Entry<Long, Map.Entry<TimelineEvent, JComponent>>> timelineEvents = new ArrayList<>();
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
        
        // Calculate the maximum count based on triggers
        maxCount = 0;
        for (RFTrigger trigger : triggers) {
            maxCount = Math.max(maxCount, trigger.getCount());
        }
        // Add some padding to the end
        maxCount += 5;
        
        createTimelinePane();
        createZoomControls();
        
        // Initialize scrub bar
        scrubBar = new TimelineScrubBar();
        
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(zoomPanel, BorderLayout.NORTH);
        
        // Create a panel to hold the scrub bar and timeline
        JPanel timelineContainer = new JPanel(new BorderLayout());
        
        // Wrap the scrub bar in a panel to ensure it doesn't get clipped
        JPanel scrubBarContainer = new JPanel(new BorderLayout());
        scrubBarContainer.add(scrubBar, BorderLayout.CENTER);
        scrubBarContainer.setPreferredSize(new Dimension(calculateTimelineWidth(), scrubBar.getPreferredSize().height));
        
        timelineContainer.add(scrubBarContainer, BorderLayout.NORTH);
        timelineContainer.add(timelineScrollPane, BorderLayout.CENTER);
        
        mainPanel.add(timelineContainer, BorderLayout.CENTER);
        
        // Add mouse wheel listener for zooming with Ctrl+Scroll
        addMouseWheelZoomSupport();
        
        // Add middle mouse button panning support
        addScrollClickPanning();

        // Add a small delay before scrubbing to ensure all components are properly initialized
        SwingUtilities.invokeLater(() -> {
            scrubToMS(curMS);
            System.out.println("curMSec: " + curMS + ", currentCount: " + curCount);
        });
    }

    public TimelineGUI(ArrayList<Effect> effects, HashMap<Integer, RFTrigger> count2RFTrigger, TimeManager timeManager, TimelineListener listener) {
        this(effects, count2RFTrigger, timeManager);
        this.timelineListener = listener;
    }
    
    /**
     * Sets the timeline listener for this timeline
     * @param listener The listener to receive timeline events
     */
    public void setTimelineListener(TimelineListener listener) {
        this.timelineListener = listener;
    }

    /**
     * Creates the zoom control panel with buttons and label
     */
    private void createZoomControls() {
        zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton zoomInButton = new JButton("+");
        JButton zoomOutButton = new JButton("-");
        JButton resetZoomButton = new JButton("Reset Zoom");
        zoomLabel = new JLabel(String.format("Zoom: %.1fx", zoomFactor));
        
        zoomInButton.addActionListener(e -> zoomIn());
        zoomOutButton.addActionListener(e -> zoomOut());
        resetZoomButton.addActionListener(e -> resetZoom());
        
        zoomPanel.add(zoomOutButton);
        zoomPanel.add(zoomLabel);
        zoomPanel.add(zoomInButton);
        zoomPanel.add(resetZoomButton);
    }
      /**
     * Adds support for zooming with Ctrl+Mouse wheel
     */
    private void addMouseWheelZoomSupport() {
        timelineScrollPane.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) {
                    // Scroll up - zoom in
                    zoomIn();
                } else {
                    // Scroll down - zoom out
                    zoomOut();
                }
                e.consume(); // Prevent the scroll event from being processed further
            }
        });
    }
    
    /**
     * Increases the zoom level by one step
     */
    private void zoomIn() {
        if (zoomFactor + ZOOM_STEP < MAX_ZOOM) {
            zoomFactor += ZOOM_STEP;
            updateZoom();
        }
    }
    
    /**
     * Decreases the zoom level by one step
     */
    private void zoomOut() {
        if (zoomFactor - ZOOM_STEP > MIN_ZOOM) {
            zoomFactor -= ZOOM_STEP;
            updateZoom();
        }
    }
    
    /**
     * Resets the zoom level to the default (1.0)
     */
    private void resetZoom() {
        zoomFactor = 1.0;
        updateZoom();
    }
    
    /**
     * Updates the zoom display and timeline layout
     */
    private void updateZoom() {
        zoomLabel.setText(String.format("Zoom: %.1fx", zoomFactor));
        updateTimelineLayout();
        scrubToMS(curMS);
    }

    private void updateTimelineLayout() {
        int width = calculateTimelineWidth();
        timelinePanel.setPreferredSize(new Dimension(width, TRIGGER_ROW_HEIGHT + ROW_HEIGHT));
        
        // Update scrub bar size
        scrubBar.updateSize();
        scrubBar.getParent().setPreferredSize(new Dimension(width, scrubBar.getPreferredSize().height));
        
        updateComponentPositions();
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }
    
    private void updateComponentPositions() {
        timelinePanel.removeAll();
        
        // Clear button groups before adding new buttons
        effectsButtonGroup = new ButtonGroup();
        triggersButtonGroup = new ButtonGroup();
        
        // Add RF Triggers (first row)
        ArrayList<RFTrigger> sortedTriggers = new ArrayList<>(triggers);
        sortedTriggers.sort(Comparator.comparingInt(RFTrigger::getCount));
        
        for (int i = 0; i < sortedTriggers.size(); i++) {
            RFTrigger trigger = sortedTriggers.get(i);
            JToggleButton triggerWidget = trigger.getTimelineWidget();
            
            // Add to button group
            triggersButtonGroup.add(triggerWidget);
            
            // Position based directly on count
            int count = trigger.getCount();
            int xPosition = (int)(count * PIXELS_PER_COUNT * zoomFactor);
            
            // Calculate width to next trigger or end
            int width;
            if (i < sortedTriggers.size() - 1) {
                int nextCount = sortedTriggers.get(i + 1).getCount();
                width = (int)((nextCount - count) * PIXELS_PER_COUNT * zoomFactor);
            } else {
                width = (int)(PIXELS_PER_COUNT * zoomFactor * 2); // Default width for last trigger
            }
            
            // Ensure minimum width and prevent overlap
            width = Math.max(width - 2, 10); // Subtract 2 pixels to prevent overlap, ensure minimum width
            
            triggerWidget.setBounds(xPosition, 0, width, TRIGGER_ROW_HEIGHT);
            timelinePanel.add(triggerWidget);
            
            // Make the trigger widget transparent to middle mouse events (pass through to timeline panel)
            panAdapters.add(PanningMouseAdapter.passMiddleMouseEvents(triggerWidget, timelinePanel));
        }
        
        // Add Effects (subsequent rows)
        java.util.List<java.util.List<TimeRange>> effectRows = new ArrayList<>();
        effectRows.add(new ArrayList<>()); // First effect row
        
        ArrayList<Effect> sortedEffects = new ArrayList<>(effects);
        sortedEffects.sort(Comparator.comparingLong(Effect::getStartTimeMSec));
        
        for (Effect effect : sortedEffects) {
            // Convert effect start time to count
            double effectStartCount = timeManager.MSec2CountPrecise((long)effect.getStartTimeMSec());
            double effectEndCount = timeManager.MSec2CountPrecise((long)(effect.getStartTimeMSec() + effect.getDuration().toMillis()));

            TimeRange effectRange = new TimeRange(
                effectStartCount,
                effectEndCount
            );
            
            int rowIndex = findAvailableRow(effectRows, effectRange);
            
            JToggleButton effectWidget = effect.getTimelineWidget();
            
            // Add to button group
            effectsButtonGroup.add(effectWidget);
            
            // Position based on counts
            int xPosition = (int)(effectStartCount * PIXELS_PER_COUNT * zoomFactor);
            int width = (int)((effectEndCount - effectStartCount) * PIXELS_PER_COUNT * zoomFactor);
            int yPosition = TRIGGER_ROW_HEIGHT + (rowIndex * ROW_HEIGHT);
            
            // Ensure minimum width
            width = Math.max(width, 10);
            
            effectWidget.setBounds(xPosition, yPosition, width, ROW_HEIGHT - 2);
            timelinePanel.add(effectWidget);
            
            // Make the effect widget transparent to middle mouse events (pass through to timeline panel)
            panAdapters.add(PanningMouseAdapter.passMiddleMouseEvents(effectWidget, timelinePanel));
            
            while (effectRows.size() <= rowIndex) {
                effectRows.add(new ArrayList<>());
            }
            effectRows.get(rowIndex).add(effectRange);
        }
        timelinePanel.setPreferredSize(new Dimension(calculateTimelineWidth(), TRIGGER_ROW_HEIGHT + effectRows.size() * ROW_HEIGHT));
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
                
                // Draw the redline at current count position after all children are drawn
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red
                g2d.setStroke(new BasicStroke(2.0f));
                
                // Draw based on current count instead of time
                int xPosition = (int)(curCount * PIXELS_PER_COUNT * zoomFactor);
                g2d.drawLine(xPosition, 0, xPosition, getHeight());
            }
        };
        timelinePanel.setPreferredSize(new Dimension(calculateTimelineWidth(),  TRIGGER_ROW_HEIGHT + ROW_HEIGHT));
        updateComponentPositions();

        timelineScrollPane = new JScrollPane(timelinePanel);
        timelineScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        timelineScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        timelineScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Make the scrub bar synchronized with the timeline horizontal scrolling
        timelineScrollPane.getHorizontalScrollBar().addAdjustmentListener(e -> {
            scrubBar.repaint();
        });
    }

    private int calculateXPosition(double ms) {
        double count = timeManager.MSec2CountPrecise((long)ms);
        return (int)(count * PIXELS_PER_COUNT * zoomFactor);
    }

    private int calculateEffectWidth(Effect effect) {
        long durationMillis = effect.getDuration().toMillis();
        // Convert duration to count-based width
        // This is an approximation - assumes 1 second per count
        return (int)((durationMillis / 1000.0) * PIXELS_PER_COUNT * zoomFactor);
    }

    private int calculateTimelineWidth() {
        // Use maxCount to determine the timeline width
        return (int)(maxCount * PIXELS_PER_COUNT * zoomFactor) + PIXELS_PER_COUNT; // Add extra space at the end
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
        double start;
        double end;

        TimeRange(double start, double end) {
            this.start = start;
            this.end = end;
        }
        
        boolean overlaps(TimeRange other) {
            return !(this.end <= other.start || other.end <= this.start);
        }
    }

    /**
     * A scrub bar component with tick marks for counts
     * Shows tick marks for each count with labels for every 4th count
     */
    private class TimelineScrubBar extends JPanel {
        private static final int SCRUB_BAR_HEIGHT = 20;
        private static final int TICK_HEIGHT = 3;
        private static final int MAJOR_TICK_HEIGHT = 6;
        private static final int LABEL_OFFSET = -2;
        
        // Track mouse interaction
        public boolean isDragging = false;
        
        public TimelineScrubBar() {
            setPreferredSize(new Dimension(calculateTimelineWidth(), SCRUB_BAR_HEIGHT));
            
            // Add mouse listeners for scrubbing
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    isDragging = true;
                    updateTimeFromMouse(e.getX(), e.isControlDown());
                }
                
                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    isDragging = false;
                }
                
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    updateTimeFromMouse(e.getX(), e.isControlDown());
                }
            });
            
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (isDragging) {
                        updateTimeFromMouse(e.getX(), e.isControlDown());
                    }
                }
            });
        }
        
        private void updateTimeFromMouse(int x, boolean ctrlPressed) {
            // Adjust x for scroll position
            int scrollX = timelineScrollPane.getHorizontalScrollBar().getValue();
            int adjustedX = x + scrollX;
            int viewportWidth = timelineScrollPane.getViewport().getWidth();
            
            // Convert x position directly to a count
            double clickedCount = (adjustedX / (PIXELS_PER_COUNT * zoomFactor));
            System.out.println("Clicked count: " + clickedCount + 
                              ", Mouse X: " + x + 
                              ", Scroll X: " + scrollX + 
                              ", Viewport Width: " + viewportWidth);
            // Ensure clickedCount is within bounds
            clickedCount = Math.max(0, Math.min(clickedCount, maxCount));
            
            // if ctrl is held down dont snap
            if (ctrlPressed) {
                // Directly scrub to the clicked count without snapping
                scrubToCount(clickedCount);
                return;
            }
            clickedCount = Math.round(clickedCount); // Round to nearest count
            
            // Find the nearest trigger within a threshold (for snapping)
                RFTrigger nearestTrigger = null;
                double minDistance = Double.MAX_VALUE;
                
            for (RFTrigger trigger : triggers) {
                double distance = Math.abs(trigger.getCount() - clickedCount);
                if (distance < minDistance && distance <= 1) { // Threshold of 1 count for snapping
                    minDistance = distance;
                    nearestTrigger = trigger;
                }
            }
        
            // If we found a nearby trigger, scrub to that count
            if (nearestTrigger != null) {
                scrubToCount(nearestTrigger.getCount());
            } else {
                // Directly scrub to the clicked count
                scrubToCount(clickedCount);
            }
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Get scroll position
            int scrollX = timelineScrollPane.getHorizontalScrollBar().getValue();
            
            // Draw the timeline background
            g2d.setColor(UIManager.getColor("Panel.background"));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // Draw the tick marks and count labels
            g2d.setColor(UIManager.getColor("Component.borderColor"));

            // Calculate the visible range in counts
            int visibleWidth = getWidth();
            int startX = scrollX;
            int endX = startX + visibleWidth;
            
            // Calculate the count range to draw
            int startCount = Math.max(0, (int)(startX / (PIXELS_PER_COUNT * zoomFactor)));
            int endCount = Math.min(maxCount, (int)(endX / (PIXELS_PER_COUNT * zoomFactor)) + 2);
            
            // Draw count markers
            for (int count = startCount; count <= endCount; count++) {
                // Calculate x position for this count
                int x = (int)(count * PIXELS_PER_COUNT * zoomFactor) - scrollX;
                
                // Check if this count has a trigger
                boolean hasTrigger = false;
                for (RFTrigger trigger : triggers) {
                    if (trigger.getCount() == count) {
                        hasTrigger = true;
                        break;
                    }
                }
                
                // Draw a tick mark
                if (count % 4 == 0) {
                    // Draw a major tick mark with label for every 4th count
                    g2d.drawLine(x, 0, x, MAJOR_TICK_HEIGHT);
                    
                    // Draw the count label
                    String countLabel = Integer.toString(count);
                    java.awt.FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(countLabel);
                    
                    g2d.drawString(countLabel, x - labelWidth / 2, MAJOR_TICK_HEIGHT + LABEL_OFFSET + fm.getAscent());
                } else {
                    // Draw a minor tick mark
                    g2d.drawLine(x, 0, x, TICK_HEIGHT);
                }
                
                // Highlight counts with triggers
                if (hasTrigger) {
                    g2d.setColor(new Color(0, 0, 255, 40)); // Light blue
                    g2d.fillRect(x - 2, 0, 4, MAJOR_TICK_HEIGHT);
                    g2d.setColor(UIManager.getColor("Component.borderColor"));
                }
            }
            
            // Draw the current position indicator (red line)
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2.0f));
            
            // Draw based on current count
            int xPosition = (int)(curCount * PIXELS_PER_COUNT * zoomFactor) - scrollX;
            
            // Only draw if in visible range
            if (xPosition >= 0 && xPosition <= visibleWidth) {
                g2d.drawLine(xPosition, 0, xPosition, getHeight());
            }
        }
        
        public void updateSize() {
            setPreferredSize(new Dimension(calculateTimelineWidth(), SCRUB_BAR_HEIGHT));
            revalidate();
            repaint();
        }
    }

    public void scrubToMS(double ms) {
        if (scrubBar.isDragging) { return; } // Don't scrub if dragging the scrub bar
        // Calculate the x position for the given time
        int xPosition = calculateXPosition(ms);
        // Set current position
        curMS = ms;
        // Update current count based on the timestamp
        curCount = timeManager.MSec2CountPrecise((long)ms);
        
        // Calculate scroll position to ensure current position is visible
        // Aim to position the current count with some padding to the left based on frame width
        int scrollPosition = (int) Math.max(0, xPosition - (timelineScrollPane.getWidth() / 2));

        // Update the scroll position and redraw the timeline
        SwingUtilities.invokeLater(() -> {
            timelineScrollPane.getHorizontalScrollBar().setValue(scrollPosition);
            timelinePanel.repaint(); // Redraw timeline
            scrubBar.repaint();      // Redraw scrub bar
        });
    }

    /**
     * Scrub to a specific count
     */
    public void scrubToCount(double count) {
        curMS = timeManager.getCount2MSec().get((int)Math.round(count));
        curCount = count;
        
        // Notify listener of the timeline scrub action to keep other components in sync
        if (timelineListener != null) {
            timelineListener.onTimelineScrub(count);
        }
        
        // Calculate x position
        int xPosition = (int)(curCount * PIXELS_PER_COUNT * zoomFactor);
        
        // Get current scroll position and viewport width
        JScrollPane scrollPane = timelineScrollPane;
        int currentScrollX = scrollPane.getHorizontalScrollBar().getValue();
        int viewportWidth = scrollPane.getViewport().getWidth();
        
        // Calculate the offset of the xPosition within the visible viewport
        int offsetInViewport = xPosition - currentScrollX;
        
        // Only scroll if position is outside the "safe zone" (more than 50px from edges)
        int scrollPosition = currentScrollX;
        
        // If position is less than 50 pixels from left edge, scroll left to give 50px margin
        if (offsetInViewport < 50) {
            scrollPosition = Math.max(0, xPosition - 50);
        } 
        // If position is less than 50 pixels from right edge, scroll right to give 50px margin
        else if (offsetInViewport > viewportWidth - 50) {
            scrollPosition = xPosition - (viewportWidth - 50);
        }
        // Otherwise, don't change the scroll position
        
        // Ensure we don't go beyond the bounds
        scrollPosition = Math.max(0, scrollPosition);
        
        // Save the final scroll position to use in the invokeLater call
        final int finalScrollPosition = scrollPosition;
        
        // Update UI
        SwingUtilities.invokeLater(() -> {
            timelineScrollPane.getHorizontalScrollBar().setValue(finalScrollPosition);
            timelinePanel.repaint();
            scrubBar.repaint();
        });
    }
    
    // List to store references to mouse adapters to prevent garbage collection
    private final List<MouseAdapter> panAdapters = new ArrayList<>();
    
    /**
     * Adds support for panning the timeline using the middle mouse button (scroll wheel)
     * This implementation uses a specialized adapter that ensures smooth, precise panning
     * with the content following exactly under the mouse cursor as you drag.
     */
    private void addScrollClickPanning() {
        // Add panning support to timeline panel with a callback to repaint the scrub bar
        PanningMouseAdapter timelinePanAdapter = new PanningMouseAdapter(
            timelinePanel, 
            timelineScrollPane,
            () -> scrubBar.repaint() // Update scrub bar whenever timeline is panned
        );
        panAdapters.add(timelinePanAdapter);
        
        // Add panning support to scrub bar
        PanningMouseAdapter scrubPanAdapter = new PanningMouseAdapter(
            scrubBar, 
            timelineScrollPane
        );
        panAdapters.add(scrubPanAdapter);
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Timeline Demo");
        frame.setPreferredSize(new Dimension(900, 400));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        ArrayList<Effect> effects = new ArrayList<>();
        
        // Create effects at specific count positions
        // Time values are less important now, but still needed for compatibility
        Effect e1 = new Effect(Duration.ofSeconds(4).toMillis(), 
                             Color.GREEN, Color.RED,
                             Duration.ofMillis(100), Duration.ofSeconds(3), Duration.ofSeconds(5),
                             true, true, true, true, 1);
        
        Effect e2 = new Effect(Duration.ofSeconds(8).toMillis(), 
                             Color.BLUE, Color.YELLOW,
                             Duration.ofMillis(100), Duration.ofSeconds(4), Duration.ofSeconds(5),
                             true, true, true, true, 2);
                             
        Effect e3 = new Effect(Duration.ofSeconds(16).toMillis(), 
                             Color.RED, Color.WHITE,
                             Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofSeconds(5),
                             true, true, true, true, 3);
        
        effects.add(e1);
        effects.add(e2);
        effects.add(e3);

        // Create triggers with explicit counts (preferred approach)
        HashMap<Integer, RFTrigger> triggers = new HashMap<>();
        
        // Create a trigger at every 4 counts for demonstration
        for (int i = 0; i <= 20; i += 4) {
            // Set count explicitly, timestamp is less important now but still needed
            RFTrigger t = new RFTrigger(i, 
                                      i * 1000, // milliseconds, 1 second per count
                                      "Count " + i,
                                      "Description " + i,
                                      "Cue " + i);
            triggers.put(i, t);
        }
        
        // Add a few more triggers at specific counts
        RFTrigger t1 = new RFTrigger(2, 2000, "Count 2", "Special trigger", "Cue 2");
        RFTrigger t2 = new RFTrigger(6, 6000, "Count 6", "Special trigger", "Cue 6");
        RFTrigger t3 = new RFTrigger(10, 10000, "Count 10", "Special trigger", "Cue 10");
        
        triggers.put(2, t1);
        triggers.put(6, t2);
        triggers.put(10, t3);
        // Create a TimeManager instance (dummy for this example)
        Map<String, Integer> dummyMap = new HashMap<>();
        ArrayList<Pair> dummyList = new ArrayList<>();
        float dummyFloat = 120.0f;
        TimeManager timeManager = new TimeManager(dummyMap, dummyList, dummyFloat);

        TimelineGUI timelineGUI = new TimelineGUI(effects, triggers, timeManager);
        frame.add(timelineGUI.getTimelineScrollPane());

        frame.pack();
        frame.setVisible(true);
    }
}
