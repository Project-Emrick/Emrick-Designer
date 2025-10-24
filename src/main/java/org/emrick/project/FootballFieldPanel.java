package org.emrick.project;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import org.emrick.project.effect.Effect;
import org.emrick.project.effect.EffectManager;
import org.emrick.project.effect.LightingDisplay;
import org.emrick.project.effect.RFTrigger;

public class FootballFieldPanel extends JPanel implements RepaintListener {
    public enum SelectionMethod {
        BOX,
        LASSO
    }

    public Drill drill;
    public HashSet<LEDStrip> selectedLEDStrips;
    public HashSet<LEDStrip> innerSelectedLEDStrips;
    public boolean innerSelect;
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

    private Set currentSet;
    private double currentSetRatio = 0.0;
    public long currentMS = 0;
    private int currentCount = 0;
    private int currentSetStartCount = 0;
    private boolean showLabels = false;
    private boolean selectAllLEDs = true;
    public SelectionMethod selectionMethod = SelectionMethod.BOX;
    private ArrayList<Point> lassoPoints;

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
        drill = new Drill();
        selectedLEDStrips = new HashSet<>();
        innerSelectedLEDStrips = new HashSet<>();
        innerSelect = false;
        this.addMouseMotionListener(new MouseInput(this));
        this.addMouseListener(new MouseInput(this));
        colorChosen = Color.BLACK;
        this.footballFieldListener = footballFieldListener;
        this.count2RFTrigger = count2RFTrigger;
        lassoPoints = new ArrayList<>();
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

    public boolean isSelectAllLEDs() {
        return selectAllLEDs;
    }

    public void setSelectAllLEDs(boolean selectAllLEDs) {
        this.selectAllLEDs = selectAllLEDs;
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
            case NOISE : return LightingDisplay.randomNoiseFunction(e, setMS, currMS);
            default : return LightingDisplay.defaultLEDFunction(e, setMS, currMS);
        }
    }

    public Point2D calculatePosition(Performer p, Set s, double setRatio, int count, int setStartCount) {
        Point2D location;
        Coordinate c1 = p.getCoordinateFromSet(s.label);
        if (s.index < drill.sets.size() - 1) {
            Coordinate c2 = p.getCoordinateFromSet(drill.sets.get(s.index + 1).label);
            if (c1.x == c2.x && c1.y == c2.y) {
                location = dotToPoint(c1.x, c1.y);
            } else {
                if (useFps) {
                    location = dotToPoint(
                            (c2.x - c1.x) * setRatio + c1.x,
                            (c2.y - c1.y) * setRatio + c1.y
                    );
                } else {
                    int duration = drill.sets.get(s.index + 1).duration;
                    location = dotToPoint((c2.x - c1.x) * (double) (count - setStartCount) /
                            duration + c1.x, (c2.y - c1.y) * (double) (count - setStartCount) / duration + c1.y);
                }
            }
        } else {
            location = dotToPoint(c1.x, c1.y);
        }
        return location;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        long currMS = 0;
        if (effectManager != null && effectManager.getTimeManager().getSet2MSec().get(currentSet.index).getValue() != null) {
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
                if (currMS >= start && currMS < nextFrameMS && footballFieldListener.isPlaying()) {
                    //footballFieldListener.onRFSignal(i);
                    break;
                }
            }
        }

        // Draw performers with their colors
        for (Performer p : drill.performers) {
            if (!p.getLedStrips().isEmpty()) {
                p.currentLocation = calculatePosition(p, currentSet, currentSetRatio, currentCount, currentSetStartCount);
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
                    } else {
                        g.setColor(new Color(0, 0, 0, effectTransparency));
                    }

                    // Use Graphics2D to draw rounded rectangles for LED strips
                    Graphics2D g2d = (Graphics2D) g;
                    
                    // Enable antialiasing for smoother rendering
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    
                    int arcSize = 4; // Controls the roundness of corners
                    int xPos = (int) x + l.getLedConfig().gethOffset();
                    int yPos = (int) y + l.getLedConfig().getvOffset();
                    int width = l.getLedConfig().getWidth();
                    int height = l.getLedConfig().getHeight();

                    g2d.fillRoundRect(xPos, yPos, width, height, arcSize, arcSize);

                    if (innerSelectedLEDStrips.contains(l)) {
                        g2d.setColor(Color.BLUE);
                    }else if (selectedLEDStrips.contains(l)) {
                        g2d.setColor(Color.GREEN);
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                    g2d.drawRoundRect(xPos, yPos, width, height, arcSize, arcSize);
                }

                if (showLabels) {
                    g.setFont(new Font("TimesRoman", Font.BOLD, (int) Math.ceil(fieldWidth / 120)));
                    g.drawString(p.getIdentifier(), (int) x - 7, (int) y + 16);
                }
            }
        }

        if (selecting) {
            if (selectionMethod == SelectionMethod.BOX) {
                int w = Math.abs(selectStartX - selectEndX);
                int h = Math.abs(selectStartY - selectEndY);
                int x = Math.min(selectStartX, selectEndX);
                int y = Math.min(selectEndY, selectStartY);
                g.setColor(new Color(0, 100, 100, 100));
                g.fillRect(x, y, w, h);
            } else if (selectionMethod == SelectionMethod.LASSO) {
                g.setColor(new Color(0, 100, 100, 100));
                int[] xpoints = new int[lassoPoints.size()];
                int[] ypoints = new int[lassoPoints.size()];
                for (int i = 0; i < lassoPoints.size(); i++) {
                    xpoints[i] = lassoPoints.get(i).x;
                    ypoints[i] = lassoPoints.get(i).y;
                }
                g.fillPolygon(new Polygon(xpoints, ypoints, lassoPoints.size()));
            }
        }
    }

    public void setColorChosen(Color color) {
        this.colorChosen = color;
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
            if (selectionMethod == SelectionMethod.BOX) {
                selectStartX = e.getX();
                selectStartY = e.getY();
                selectEndX = e.getX();
                selectEndY = e.getY();
            } else if (selectionMethod == SelectionMethod.LASSO) {
                lassoPoints.add(e.getPoint());
            }
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            selecting = false;
            selectEndX = e.getX();
            selectEndY = e.getY();

            int axmin = Math.min(selectStartX, selectEndX);
            int aymin = Math.min(selectStartY, selectEndY);
            int axmax = Math.max(selectStartX, selectEndX);
            int aymax = Math.max(selectStartY, selectEndY);

            boolean isControlDown = e.isControlDown();
            if (!isControlDown) {
                if (innerSelect) {
                    innerSelectedLEDStrips.clear();
                } else {
                    selectedLEDStrips.clear();
                    footballFieldListener.onPerformerDeselect();
                }
            }
            boolean select = false;
            HashSet<LEDStrip> ledStripsToChange = new HashSet<>();
            for (LEDStrip ledStrip : drill.ledStrips) {
                Performer p = ledStrip.getPerformer();
                double px = p.currentLocation.getX();
                double py = p.currentLocation.getY();
                if (selectionMethod == SelectionMethod.BOX) {
                    int bxmin = (int) (px + ledStrip.getLedConfig().gethOffset());
                    int bymin = (int) (py + ledStrip.getLedConfig().getvOffset());
                    int bxmax = (int) (px + ledStrip.getLedConfig().gethOffset() + ledStrip.getLedConfig().getWidth());
                    int bymax = (int) (py + ledStrip.getLedConfig().getvOffset() + ledStrip.getLedConfig().getHeight());
                    boolean intersecting;
                    if (axmax - axmin != 0 || aymax - aymin != 0) {
                        intersecting = AABB(axmin, aymin, axmax, aymax, bxmin, bymin, bxmax, bymax);
                    } else {
                        intersecting = (axmin >= bxmin && axmin <= bxmax && aymin >= bymin && aymin <= bymax);
                    }

                    if (intersecting) {
                        if (innerSelect) {
                            if (selectAllLEDs) {
                                for (Integer i : p.getLedStrips()) {
                                    LEDStrip l = drill.ledStrips.get(i);
                                    if (selectedLEDStrips.contains(l)) {
                                        if (isControlDown) {
                                            ledStripsToChange.add(l);
                                            if (!innerSelectedLEDStrips.contains(l)) {
                                                select = true;
                                            }
                                        } else {
                                            ledStripsToChange.add(l);
                                            select = true;
                                        }
                                    }
                                }
                            } else {
                                if (selectedLEDStrips.contains(ledStrip)) {
                                    if (isControlDown) {
                                        ledStripsToChange.add(ledStrip);
                                        if (!innerSelectedLEDStrips.contains(ledStrip)) {
                                            select = true;
                                        }
                                    } else {
                                        ledStripsToChange.add(ledStrip);
                                        select = true;
                                    }
                                }
                            }
                        } else {
                            if (selectAllLEDs) {
                                for (Integer i : p.getLedStrips()) {
                                    LEDStrip l = drill.ledStrips.get(i);
                                    if (isControlDown) {
                                        ledStripsToChange.add(l);
                                        if (!selectedLEDStrips.contains(l)) {
                                            select = true;
                                        }
                                    } else {
                                        ledStripsToChange.add(l);
                                        select = true;
                                    }
                                }
                            } else {
                                if (isControlDown) {
                                    ledStripsToChange.add(ledStrip);
                                    if (!selectedLEDStrips.contains(ledStrip)) {
                                        select = true;
                                    }
                                } else {
                                    ledStripsToChange.add(ledStrip);
                                    select = true;
                                }
                            }
                        }
                    }
                } else if (selectionMethod == SelectionMethod.LASSO) {
                    int leftCrossings = 0;
                    for (int i = 0; i < lassoPoints.size(); i++) {
                        if ((lassoPoints.get(i).y <= py && lassoPoints.get((i+1) % lassoPoints.size()).y >= py)
                                || (lassoPoints.get((i+1) % lassoPoints.size()).y <= py && lassoPoints.get(i).y >= py)) {
                            if (lassoPoints.get(i).x <= px || lassoPoints.get((i+1) % lassoPoints.size()).x <= px) {
                                leftCrossings++;
                            }
                        }
                    }
                    if (innerSelect) {
                        if (selectedLEDStrips.contains(ledStrip)) {
                            if (leftCrossings % 2 == 1) {
                                if (isControlDown) {
                                    ledStripsToChange.add(ledStrip);
                                    if (!innerSelectedLEDStrips.contains(ledStrip)) {
                                        select = true;
                                    }
                                } else {
                                    ledStripsToChange.add(ledStrip);
                                    select = true;
                                }
                            }
                        }
                    } else {
                        if (leftCrossings % 2 == 1) {
                            if (isControlDown) {
                                ledStripsToChange.add(ledStrip);
                                if (!selectedLEDStrips.contains(ledStrip)) {
                                    select = true;
                                }
                            } else {
                                ledStripsToChange.add(ledStrip);
                                select = true;
                            }
                        }
                    }
                }
            }
            lassoPoints.clear();
            if (select) {
                if (innerSelect) {
                    innerSelectedLEDStrips.addAll(ledStripsToChange);
                } else {
                    selectedLEDStrips.addAll(ledStripsToChange);
                    footballFieldListener.onPerformerSelect();
                }
            } else {
                if (innerSelect) {
                    for (LEDStrip ledStrip : ledStripsToChange) {
                        innerSelectedLEDStrips.remove(ledStrip);
                    }
                } else {
                    for (LEDStrip ledStrip : ledStripsToChange) {
                        selectedLEDStrips.remove(ledStrip);
                    }
                    footballFieldListener.onPerformerDeselect();
                }
            }
            this.repaintListener.onRepaintCall();
        }

        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectionMethod == SelectionMethod.BOX) {
                selectEndX = e.getX();
                selectEndY = e.getY();
            } else if (selectionMethod == SelectionMethod.LASSO) {
                if (!lassoPoints.isEmpty()) {
                    Point last = lassoPoints.get(lassoPoints.size() - 1);
                    Point curr = e.getPoint();
                    double diff = Math.sqrt(Math.pow(curr.x - last.x, 2) + Math.pow(curr.y - last.y, 2));
                    if (diff >= 3) {
                        lassoPoints.add(curr);
                    }
                } else {
                    lassoPoints.add(e.getPoint());
                }
            }

            this.repaintListener.onRepaintCall();
        }
        @Override
        public void mouseMoved(MouseEvent e) {}

        private static boolean AABB(
                int axmin, int aymin, int axmax, int aymax,
                int bxmin, int bymin, int bxmax, int bymax
        ) {
            return !(axmin >= bxmax || axmax <= bxmin || aymin >= bymax || aymax <= bymin);
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
        return this.selectedLEDStrips.size();
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
