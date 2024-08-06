package org.emrick.project;

import org.emrick.project.actions.LEDConfig;
import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectList;
import org.emrick.project.effect.EffectManager;
import org.emrick.project.effect.LightingDisplay;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;

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
        for (LEDStrip ledStrip : ledStrips) {
            LEDDisplay ledDisplay = new LEDDisplay(ledStrip);
            this.ledDisplays.add(ledDisplay);
            this.add(ledDisplay);
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
                        for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                            colors.add(LightingDisplay.alternatingColorFunction(e, 0, currMS));
                        }
                        break;
                    }
                    case CHASE: {
                        colors = LightingDisplay.chaseFunction(e, ledStrip, 0, currMS);
                        break;
                    }
                    default: {
                        colors = new ArrayList<>();
                        for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                            colors.add(LightingDisplay.defaultLEDFunction(e, 0, currMS));
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
            for (int i = 0; i < ledStrip.getLedConfig().getLEDCount(); i++) {
                g.setColor(colors.get(i));
                g.fillRect(locations.get((i+(int)Math.ceil((double)ledStrip.getLedConfig().getLEDCount() / 8.0)) % (ledsPerSide * 4)).x, locations.get((i+(int)Math.ceil((double)ledStrip.getLedConfig().getLEDCount() / 8.0)) % (ledsPerSide * 4)).y, ledWidth-1, ledHeight-1);
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
