package org.emrick.project;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectManager;
import org.emrick.project.effect.LightingDisplay;

public class LEDStripViewGUI extends JPanel {
    private ArrayList<LEDStrip> ledStrips;
    private ArrayList<LEDDisplay> ledDisplays;
    private long currentMS;
    private Set currentSet;
    private EffectManager effectManager;


    public LEDStripViewGUI(ArrayList<LEDStrip> ledStrips, EffectManager effectManager) {
        this.ledStrips = ledStrips;
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.ledDisplays = new ArrayList<>();
        this.currentMS = 0;
        this.effectManager = effectManager;
        Border innerBorder = BorderFactory.createTitledBorder("LED Strip Viewer");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        this.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
        for (LEDStrip ledStrip : ledStrips) {
            LEDDisplay ledDisplay = new LEDDisplay(ledStrip);
            this.ledDisplays.add(ledDisplay);
            this.add(ledDisplay);
        }
        if (ledDisplays.isEmpty()) {
            JLabel ledStripLabel = new JLabel("No LED Strips selected. Go back and select LED strips to use this menu");
            this.add(ledStripLabel);
        }
    }

    public ArrayList<LEDStrip> getLedStrips() {
        return ledStrips;
    }

    public void setLedStrips(ArrayList<LEDStrip> ledStrips) {
        this.ledStrips = ledStrips;
    }

    public long getCurrentMS() {
        return currentMS;
    }

    public void setCurrentMS(long currentMS) {
        this.currentMS = currentMS;
        this.repaint();
    }

    public Set getCurrentSet() {
        return currentSet;
    }

    public void setCurrentSet(Set currentSet) {
        this.currentSet = currentSet;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public void setEffectManager(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    public class LEDDisplay extends JPanel {
        private LEDStrip ledStrip;
        public LEDDisplay(LEDStrip ledStrip) {
            this.ledStrip = ledStrip;
            this.setPreferredSize(new Dimension(200,200));
            this.setMinimumSize(new Dimension(200,200));
            this.setMaximumSize(new Dimension(200,200));
            this.setOpaque(false);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            // TODO: get color list at current time
            ArrayList<Color> colors;
            long setMS = effectManager.getTimeManager().getSet2MSec().get(currentSet.index).getValue();
            long currMS = currentMS + setMS;
            Effect e = effectManager.getEffect(ledStrip, currMS);
            if (e != null) {
                switch (e.getEffectType()) {
                    case ALTERNATING_COLOR: {
                        colors = new ArrayList<>();
                        Color c = LightingDisplay.alternatingColorFunction(e, 0, currMS);
                        for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                            colors.add(c);
                        }
                        break;
                    }
                    case CHASE: {
                        colors = LightingDisplay.chaseFunction(e, ledStrip, 0, currMS);
                        break;
                    }
                    case NOISE : {
                        colors = new ArrayList<>();
                        Color c = LightingDisplay.randomNoiseFunction(e, 0, currMS);
                        for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                            colors.add(c);
                        }
                        break;
                    }
                    default: {
                        colors = new ArrayList<>();
                        Color c = LightingDisplay.defaultLEDFunction(e, 0, currMS);
                        for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                            colors.add(c);
                        }
                        break;
                    }
                }
            } else {
                colors = new ArrayList<>();
                for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                    colors.add(Color.black);
                }
            }
            // TODO: calculate position of every LED
            ArrayList<Point> locations = new ArrayList<>();
            int ledsPerSide = (int)Math.ceil((double)ledStrip.getLedConfig().getLEDCount() / 4.0);
            int ledWidth = getWidth() / (ledsPerSide + 2);
            int ledHeight = getHeight() / (ledsPerSide + 2);
            for (int i = 0; i < ledsPerSide; i++) {
                locations.add(new Point(ledWidth * (i+1),0));
            }
            for (int i = 0; i < ledsPerSide; i++) {
                locations.add(new Point(ledWidth * (ledsPerSide + 1),ledHeight * (i+1)));
            }
            for (int i = 0; i < ledsPerSide; i++) {
                locations.add(new Point(ledWidth * (ledsPerSide - i),ledHeight * (ledsPerSide + 1)));
            }
            for (int i = 0; i < ledsPerSide; i++) {
                locations.add(new Point(0, ledHeight * ((ledsPerSide - i))));
            }
            Graphics2D g2d = (Graphics2D) g;
            int arcSize = 10; // Controls the roundness of corners
            
            for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                g2d.setColor(colors.get(i));
                int index = (i+(int)Math.ceil((double)ledStrip.getLedConfig().getLEDCount() / 8.0)) % (ledsPerSide * 4);
                int xPos = locations.get(index).x;
                int yPos = locations.get(index).y;
                g2d.fillRoundRect(xPos, yPos, ledWidth-1, ledHeight-1, arcSize, arcSize);
            }
        }

        public LEDStrip getLedStrip() {
            return ledStrip;
        }

        public void setLedStrip(LEDStrip ledStrip) {
            this.ledStrip = ledStrip;
        }
    }
}
