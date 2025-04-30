package org.emrick.project.effect;

import org.emrick.project.MediaEditorGUI;
import org.emrick.project.TimeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
    public JPanel getTimelineWidget() {
        Border outerBorder = BorderFactory.createLineBorder(Color.lightGray);
        Border innerBorder = BorderFactory.createEmptyBorder(2,2,2,2);

        JPanel widgetPanel = new JPanel(new GridLayout(4,1));
        widgetPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        JLabel titleLabel = new JLabel("<html><b>" + (((title != null) && !title.trim().isEmpty()) ? 
            title : "<p text=\"gray\"> RF Trigger </p>") + "</b></html>");
        JLabel countLabel = new JLabel("Count: " + count);
        JLabel timeLabel = new JLabel("Time: " + TimeManager.getFormattedTime(timestampMillis));

        widgetPanel.add(titleLabel);
        widgetPanel.add(countLabel);
        widgetPanel.add(timeLabel);

        widgetPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                widgetPanel.setBorder
                    (BorderFactory.createCompoundBorder(outerBorder, BorderFactory.createLineBorder(Color.blue, 2, true)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                widgetPanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // signals scrub to this rf trigger on press
                rfTriggerListener.onPressRFTrigger(RFTrigger.this);
            }
        });

        return widgetPanel;
    }
}
