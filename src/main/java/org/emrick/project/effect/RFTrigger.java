package org.emrick.project.effect;

import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JToggleButton;

import org.emrick.project.TimeManager;

public class RFTrigger implements TimelineEvent {

    int count;
    long timestampMillis;
    public static RFTriggerListener rfTriggerListener;

    String title;
    String description;
    String cue;

    public RFTrigger(int count, long timestampMillis, String title, String description, String cue) {
        this.count = count;
        this.timestampMillis = timestampMillis;
        this.title = title;
        this.description = description;
        this.cue = cue;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    public String getTitle() { return title; }
    public void setTitle(String title ) {this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description ) {this.description = description; }

    public String getCue() { return cue; }
    public void setCue(String cue ) {this.cue = cue; }

    @Override
    public JToggleButton getTimelineWidget() {
        JToggleButton widgetButton = new JToggleButton();
        widgetButton.setLayout(new GridLayout(4,1));
        widgetButton.setMargin(new Insets(1, 2, 1, 2)); // Remove the default button margin
        //widgetButton.setBorderPainted(false); // Don't paint the button's default border
        //widgetButton.setContentAreaFilled(false); // Don't fill the content area (transparent background)
        widgetButton.setFocusPainted(false); // Don't paint the focus indicator

        JLabel titleLabel = new JLabel("<html><nobr><b>" + (((title != null) && !title.trim().isEmpty()) ? 
            title : "<p text=\"gray\"> RF Trigger </p>") + "</b></nobr></html>");
        JLabel countLabel = new JLabel("Count: " + count);
        JLabel timeLabel = new JLabel("Time: " + TimeManager.getFormattedTime(timestampMillis));

        widgetButton.add(titleLabel);
        widgetButton.add(countLabel);
        widgetButton.add(timeLabel);

        widgetButton.addActionListener(e -> {
            // signals scrub to this rf trigger on press
            rfTriggerListener.onPressRFTrigger(RFTrigger.this);
        });

        return widgetButton;
    }
}
