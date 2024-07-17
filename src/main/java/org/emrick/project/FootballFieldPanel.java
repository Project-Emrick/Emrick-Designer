package org.emrick.project;

import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectManager;
import org.emrick.project.effect.LightingDisplay;
import org.emrick.project.effect.RFTrigger;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class FootballFieldPanel extends JPanel implements RepaintListener {
    public Drill drill;
    public HashMap<String,Performer> selectedPerformers;
    private double fieldWidth = 720; // Width of the football field
    private double fieldHeight = 360;
    private Point frontSideline50 = new Point(360,360);

    private Color colorChosen;
    private final int margin = 15;

    // Loading field decor.
    private BufferedImage surfaceImage;
    private BufferedImage floorCoverImage;
    private final BufferedImage dummyImage = new BufferedImage(2196, 1157, BufferedImage.TYPE_INT_ARGB);
    private boolean showSurfaceImage = true;
    private boolean showFloorCoverImage = true;

    private boolean ctrlHeld = false;
    private Set currentSet;
    private double currentSetRatio = 0.0;
    public long currentMS = 0;
    private int currentCount = 0;
    private int currentSetStartCount = 0;
    private boolean showLabels = false;

    // Effects utility
    private final FootballFieldListener footballFieldListener;
    private EffectManager effectManager;
    private int effectTransparency = 255;
    private boolean useFps;
    private HashMap<Integer, RFTrigger> count2RFTrigger;

    public void setUseFps(boolean useFps) {
        this.useFps = useFps;
    }

    public FootballFieldPanel(FootballFieldListener footballFieldListener, HashMap<Integer, RFTrigger> count2RFTrigger) {
//        setPreferredSize(new Dimension(fieldWidth + 2*margin, fieldHeight + 2*margin)); // Set preferred size for the drawing area
        setMinimumSize(new Dimension(1042, 548));
        drill = new Drill();
        selectedPerformers = new HashMap<>();
        this.addMouseMotionListener(new MouseInput(this));
        this.addMouseListener(new MouseInput(this));
        colorChosen = Color.BLACK;
        this.footballFieldListener = footballFieldListener;
        this.count2RFTrigger = count2RFTrigger;
    }

    public FootballFieldPanel(Color colorChosen, FootballFieldListener footballFieldListener) {
        this.footballFieldListener = footballFieldListener;
        this.colorChosen = colorChosen;
    }

    /**
     * @param effectManager Utility class that handles many functionalities regarding effects. Note that EffectManager
     *                      has a reference to FootballFieldPanel. They are closely related and tightly coupled, but
     *                      this introduces a circular dependency. May want to somehow refactor in the future.
     */
    public void setEffectManager(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    public void setEffectTransparency(int effectTransparency) {
        this.effectTransparency = effectTransparency;
    }

    public HashMap<Integer, RFTrigger> getCount2RFTrigger() {
        return count2RFTrigger;
    }

    public void setCount2RFTrigger(HashMap<Integer, RFTrigger> count2RFTrigger) {
        this.count2RFTrigger = count2RFTrigger;
    }

    public void addSetToField(Set set) {
        currentSet = set;
        if (!set.equals("0")) {
            for (Performer p : drill.performers) {
                for (Coordinate c : p.getCoordinates()) {
                    if (c.set.equals(set)) {
                        p.currentLocation = dotToPoint(c.x, c.y);
                        break;
                    }
                }
            }
        } else {
            for (Performer p : drill.performers) {
                p.currentLocation = new Point2D.Double(-20,-20);
            }
        }
        repaint();
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
        repaint();
    }

    public int getCurrentSetStartCount() {
        return currentSetStartCount;
    }

    public void setCurrentSetStartCount(int currentSetStartCount) {
        this.currentSetStartCount = currentSetStartCount;
    }

    public Set getCurrentSet() {
        return currentSet;
    }

    public Point2D dotToPoint(double x, double y) {
        double newY = frontSideline50.y - y/84 * fieldHeight;
        double newX = frontSideline50.x + x/160 * fieldWidth;
        return new Point2D.Double(newX,newY);
    }

    public double getFieldWidth() {
        return fieldWidth;
    }

    public double getFieldHeight() {
        return fieldHeight;
    }

    protected Color calculateColor(Effect e) {
        long setMS = effectManager.getTimeManager().getSet2MSec().get(currentSet.index).getValue();
        long currMS = currentMS + setMS;

        switch(e.getFunction()) {
            case ALTERNATING_COLOR : return LightingDisplay.alternatingColorFunction(e, setMS, currMS);
            default : return LightingDisplay.defaultLEDFunction(e, setMS, currMS);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        long currMS = 0;
        if (effectManager != null) {
            long setMS = effectManager.getTimeManager().getSet2MSec().get(currentSet.index).getValue();
            currMS = currentMS + setMS;
        }
        // Case: User is not using FPS playback mode
        if (!useFps) currentSetRatio = 1;
        if (count2RFTrigger != null) {
            Iterator<RFTrigger> iterator = count2RFTrigger.values().iterator();
            ArrayList<RFTrigger> triggers = new ArrayList<>();
            while (iterator.hasNext()) {
                triggers.add(iterator.next());
            }
            triggers.sort(Comparator.comparingInt(RFTrigger::getCount));
            for (int i = 0; i < triggers.size(); i++) {
                RFTrigger trigger = triggers.get(i);
                long start = trigger.getTimestampMillis();
                long nextFrameMS = start + (long) (1.0 / footballFieldListener.getFrameRate() * 1000);
                if (currMS >= start && currMS < nextFrameMS) {
                    footballFieldListener.onRFSignal(i);
                    break;
                }
            }
        }

        // Draw performers with their colors
        for (Performer p : drill.performers) {
            Coordinate c1 = p.getCoordinateFromSet(currentSet.label);
            if (currentSet.index < drill.sets.size() - 1) {
                Coordinate c2 = p.getCoordinateFromSet(drill.sets.get(currentSet.index + 1).label);
                if (c1.x == c2.x && c1.y == c2.y) {
                    p.currentLocation = dotToPoint(c1.x, c1.y);
                } else {
                    if (useFps) {
                        p.currentLocation = dotToPoint(
                                (c2.x - c1.x) * currentSetRatio + c1.x,
                                (c2.y - c1.y) * currentSetRatio + c1.y
                        );
                    } else {
                        int duration = drill.sets.get(currentSet.index + 1).duration;
                        p.currentLocation = dotToPoint((c2.x - c1.x) * (double) (currentCount - currentSetStartCount) /
                                duration + c1.x, (c2.y - c1.y) * (double) (currentCount - currentSetStartCount) / duration + c1.y);
                    }
                }
            } else {
                p.currentLocation = dotToPoint(c1.x, c1.y);
            }
            double x = p.currentLocation.getX();
            double y = p.currentLocation.getY();


            for (Integer i : p.getLedStrips()) {
                LEDStrip l = drill.ledStrips.get(i);

                if (effectManager != null) {
                    Effect currentEffect = effectManager.getEffect(l, currMS);

                    // No effect is present at the current count
                    if (currentEffect == null) {
                        g.setColor(new Color(0, 0, 0, effectTransparency));
                    } else {
                        Color effectColor = calculateColor(currentEffect);
                        //System.out.println(effectColor);
                        Color displayColor = new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), effectTransparency);
                        g.setColor(displayColor);
                    }
                }

                g.fillRect((int) x + l.gethOffset(), (int) y + l.getvOffset(), 6, 12);
                if (selectedPerformers.get(p.getIdentifier()) != null) {
                    g.setColor(Color.GREEN);
                } else {
                    g.setColor(Color.BLACK);
                }
                g.drawRect((int) x + l.gethOffset(), (int) y + l.getvOffset(), 6, 12);
            }

            if (showLabels) {
                g.setFont(new Font("TimesRoman", Font.BOLD, (int) Math.ceil(fieldWidth / 120)));
                g.drawString(p.getIdentifier(), (int) x - 7, (int) y + 16);
            }

        }

        if (selecting) {
            int w = Math.abs(selectStartX - selectEndX);
            int h = Math.abs(selectStartY - selectEndY);
            int x = Math.min(selectStartX, selectEndX);
            int y = Math.min(selectEndY, selectStartY);
            g.setColor(new Color(0, 100, 100, 100));
            g.fillRect(x, y, w, h);
        }
        footballFieldListener.onFinishRepaint();
    }

    public void setColorChosen(Color color) {
        this.colorChosen = color;
    }

    // Draw image while maintaining aspect ratio (don't let field stretch/compress)
    private void drawBetterImage(Graphics g, BufferedImage image) {
        assert image != null;

        // Calculate the best width and height to maintain aspect ratio
        double widthRatio = (double) getWidth() / image.getWidth();
        double heightRatio = (double) getHeight() / image.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int width = (int) (image.getWidth() * ratio);
        int height = (int) (image.getHeight() * ratio);

        this.fieldWidth = (image.getWidth() * ratio);
        this.fieldHeight = (image.getHeight() * ratio);

        // Center the image
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;
        frontSideline50 = new Point(x + (int)fieldWidth / 2,y + (int)fieldHeight);

        g.drawImage(image, x, y, width, height, this);
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    public void setFloorCoverImage(Image floorCoverImage) {
        this.floorCoverImage = (BufferedImage) floorCoverImage;
    }

    public void setSurfaceImage(Image surfaceImage) {
        this.surfaceImage = (BufferedImage) surfaceImage;
    }

    public Image getFloorCoverImage() {
        return floorCoverImage;
    }

    public Image getSurfaceImage() {
        return surfaceImage;
    }

    // this is a sin, but it is my sin - LHD
    private boolean selecting = false;
    private int selectStartX = 0;
    private int selectStartY = 0;
    private int selectEndX = 0;
    private int selectEndY = 0;

    @Override
    public void onRepaintCall() {
        repaint();
    }

    private class MouseInput implements MouseInputListener {

        private final RepaintListener repaintListener;
        public MouseInput(RepaintListener repaintListener) {
            this.repaintListener = repaintListener;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }
        @Override
        public void mousePressed(MouseEvent e) {
            selecting = true;
            selectStartX = e.getX();
            selectStartY = e.getY();
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            selecting = false;
            selectEndX = e.getX();
            selectEndY = e.getY();

            // TODO: select all boxes inside box

            int axmin = Math.min(selectStartX, selectEndX);
            int aymin = Math.min(selectStartY, selectEndY);
            int axmax = Math.max(selectStartX, selectEndX);
            int aymax = Math.max(selectStartY, selectEndY);

            boolean isControlDown = e.isControlDown();
            if (!isControlDown) {
                selectedPerformers.clear();
                footballFieldListener.onPerformerDeselect();
            }
            boolean select = false;
            for (Performer p : drill.performers) {
                double px = p.currentLocation.getX();
                double py = p.currentLocation.getY();

                int bxmin = (int) (px - 7);
                int bymin = (int) (py - 7);
                int bxmax = (int) (px + 7);
                int bymax = (int) (py + 7);

                boolean intersecting = AABB(axmin, aymin, axmax, aymax, bxmin, bymin, bxmax, bymax);

                if (intersecting) {
                    String key = p.getIdentifier();
                    if (isControlDown) {
                        if (selectedPerformers.containsKey(key)) {
                            selectedPerformers.remove(key); // Deselect if already selected
                            footballFieldListener.onPerformerDeselect();
                        }
                        else {
                            selectedPerformers.put(key, p); // Select if not already selected
                            select = true;
                        }
                    } else {
                        selectedPerformers.put(key, p);
                        select = true;
                    }
                }
            }
            if (select) {
                footballFieldListener.onPerformerSelect();
            }
            this.repaintListener.onRepaintCall();
        }

        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {
            selectEndX = e.getX();
            selectEndY = e.getY();

            this.repaintListener.onRepaintCall();
        }
        @Override
        public void mouseMoved(MouseEvent e) {}

        private static boolean AABB(
                int axmin, int aymin, int axmax, int aymax,
                int bxmin, int bymin, int bxmax, int bymax
        ) {
            int[] A = {axmin, aymin, axmax, aymax};
            int[] B = {bxmin, bymin, bxmax, bymax};
            return !(A[0] >= B[2] || A[2] <= B[0] || A[1] >= B[3] || A[3] <= B[1]);
        }
    }
    public void setCurrentSet(Set currentSet) {
        this.currentSet = currentSet;
    }

    public void setCurrentSetRatio(double currentSetRatio) {
        this.currentSetRatio = currentSetRatio;
    }

    public void setShowSurfaceImage(boolean showSurfaceImage) {
        this.showSurfaceImage = showSurfaceImage;
    }

    public boolean getShowSurfaceImage() {
        return showSurfaceImage;
    }

    public void setShowFloorCoverImage(boolean showFloorCoverImage) {
        this.showFloorCoverImage = showFloorCoverImage;
    }

    public boolean getShowFloorCoverImage() {
        return showFloorCoverImage;
    }

    public int getNumSelectedPerformers() {
        return this.selectedPerformers.size();
    }

    public void setFieldWidth(double fieldWidth) {
        this.fieldWidth = fieldWidth;
    }

    public void setFieldHeight(double fieldHeight) {
        this.fieldHeight = fieldHeight;
    }

    public Point getFrontSideline50() {
        return frontSideline50;
    }

    public void setFrontSideline50(Point frontSideline50) {
        this.frontSideline50 = frontSideline50;
    }
}
