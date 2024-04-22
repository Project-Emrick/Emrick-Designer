package org.emrick.project;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

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
    private int currentCount = 0;
    private int currentSetStartCount = 0;
    private SerialTransmitter serialTransmitter;

    // Effects utility
    private final FootballFieldListener footballFieldListener;
    private EffectManager effectManager;
    private int effectTransparency = 255;

    public FootballFieldPanel(FootballFieldListener footballFieldListener) {
//        setPreferredSize(new Dimension(fieldWidth + 2*margin, fieldHeight + 2*margin)); // Set preferred size for the drawing area
        setMinimumSize(new Dimension(1042, 548));
        drill = new Drill();
        selectedPerformers = new HashMap<>();
        this.addMouseMotionListener(new MouseInput(this));
        this.addMouseListener(new MouseInput(this));
        colorChosen = Color.BLACK;
        this.footballFieldListener = footballFieldListener;
    }

    public FootballFieldPanel(Color colorChosen, FootballFieldListener footballFieldListener) {
        this(footballFieldListener);
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

    public void addSetToField(Set set) {
        currentSet = set;
        if (!set.equals("0")) {
            if (serialTransmitter != null) {
                serialTransmitter.writeSet(set.index);
            }
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

    public SerialTransmitter getSerialTransmitter() {
        return serialTransmitter;
    }

    public void setSerialTransmitter(SerialTransmitter serialTransmitter) {
        this.serialTransmitter = serialTransmitter;
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

    public void clearDots() {
        for (Performer p : drill.performers) {
            p.currentLocation = new Point2D.Double(-20,-20);
        }
        repaint();
    }

    public double getFieldWidth() {
        return fieldWidth;
    }

    public double getFieldHeight() {
        return fieldHeight;
    }

//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//
//        // Draw the football field background
//        g.setColor(new Color(92,255,103));
//        g.fillRect(margin, margin, fieldWidth, fieldHeight); // Use margin for x and y start
//
//        // Adjust line and shape drawing to account for the margin
//        g.setColor(Color.WHITE);
//        g.drawRect(margin, margin, fieldWidth, fieldHeight);
//        g.drawLine(fieldWidth / 2 + margin, margin, fieldWidth / 2 + margin, fieldHeight + margin);
//        g.drawOval((fieldWidth / 2 - fieldHeight / 10) + margin, (fieldHeight / 2 - fieldHeight / 10) + margin, fieldHeight / 5, fieldHeight / 5);
//        g.drawRect(margin, (fieldHeight / 2 - fieldHeight / 4) + margin, fieldWidth / 10, fieldHeight / 2);
//        g.drawRect(fieldWidth - (fieldWidth / 10) + margin, (fieldHeight / 2 - fieldHeight / 4) + margin, fieldWidth / 10, fieldHeight / 2);
//
    // Adjust dot drawing to account for the margin
//        g.setColor(Color.RED);
//        for (Point dot : dotCoordinates) {
//            double adjustedX = Math.min(dot.x, fieldWidth + margin - 5); // Adjust for margin
//            int adjustedY = Math.min(dot.y, fieldHeight + margin - 5); // Adjust for margin
//            g.fillOval(adjustedX - 5, adjustedY - 5, 10, 10);
//        }
//    }

    protected Color calculateColor(Effect e) {
        long currMS = effectManager.timeManager.getCount2MSec().get(currentCount);
        if (e.DO_DELAY) {
            if (e.startTimeMSec + e.delay.toMillis() > currMS) {
                if (e.INSTANT_COLOR) {
                    return e.startColor;
                } else {
                    return Color.black;
                }
            }
        }
        if (e.TIME_GRADIENT) {
            if (e.startTimeMSec + e.delay.toMillis() + e.duration.toMillis() > currMS) {
                long startGradient = e.startTimeMSec + e.delay.toMillis();
                float shiftProgress = (float) (currMS - startGradient) / (float) e.duration.toMillis();
                float[] hsvs = new float[3];
                Color.RGBtoHSB(e.startColor.getRed(), e.startColor.getGreen(), e.startColor.getBlue(), hsvs);
                hsvs[0] *= 360;
                float startHue = hsvs[0];
                float[] hsve = new float[3];
                Color.RGBtoHSB(e.endColor.getRed(), e.endColor.getGreen(), e.endColor.getBlue(), hsve);
                hsve[0] *= 360;
                float endHue = hsve[0];
                boolean clockwise = true;
                if (endHue > startHue) {
                    if (endHue - startHue > 180) {
                        clockwise = false;
                    }
                } else {
                    if (startHue - endHue < 180) {
                        clockwise = false;
                    }
                }
                float h,s,v;
                if (clockwise) {
                    if (hsve[0] >= hsvs[0]) {
                        h = ((hsve[0] - hsvs[0]) * shiftProgress + hsvs[0]) % 360;
                    } else {
                        h = ((hsve[0] + 360 - hsvs[0]) * shiftProgress + hsvs[0]) % 360;
                    }
                    s = (hsve[1] - hsvs[1]) * shiftProgress + hsvs[1];
                    v = (hsve[2] - hsvs[2]) * shiftProgress + hsvs[2];
                } else {
                    if (hsve[0] <= hsvs[0]) {
                        h = ((hsvs[0] - (hsve[0] - hsvs[0]) * shiftProgress)) % 360;
                    } else {
                        h = (hsvs[0] + 360 - (hsvs[0] + 360 - hsve[0]) * shiftProgress) % 360;
                    }
                    s = (hsve[1] - hsvs[1]) * shiftProgress + hsvs[1];
                    v = (hsve[2] - hsvs[2]) * shiftProgress + hsvs[2];
                }
//                System.out.println(h + ", " + s + ", " + v + ", " + shiftProgress);
                h /= 360;
                return new Color(Color.HSBtoRGB(h,s,v));
            }
        }
        if (e.SET_TIMEOUT) {
            if (e.startTimeMSec + e.delay.toMillis() + e.duration.toMillis() + e.timeout.toMillis() > currMS) {
                return e.endColor;
            }
        }
        return Color.black;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the surface image
        if (surfaceImage != null && showSurfaceImage) {
            drawBetterImage(g, surfaceImage);
        }

        // Draw the floorCover image on top
        if (floorCoverImage != null && showFloorCoverImage) {
            drawBetterImage(g, floorCoverImage);
        }

        if (!showFloorCoverImage && !showSurfaceImage) {
            drawBetterImage(g, dummyImage); // For accurate plotting, need some image reference
        }

        // Draw performers with their colors
        for (Performer p : drill.performers) {
            Coordinate c1 = p.getCoordinateFromSet(currentSet.label);
            if (currentSet.index < drill.sets.size() - 1) {
                Coordinate c2 = p.getCoordinateFromSet(drill.sets.get(currentSet.index + 1).label);
                if (c1.x == c2.x && c1.y == c2.y || currentCount == currentSetStartCount) {
                    p.currentLocation = dotToPoint(c1.x, c1.y);
                } else {
                    int duration = drill.sets.get(currentSet.index + 1).duration;
                    p.currentLocation = dotToPoint((c2.x - c1.x) * (double) (currentCount - currentSetStartCount) /
                            duration + c1.x, (c2.y - c1.y) * (double) (currentCount - currentSetStartCount) / duration + c1.y);
                }
            } else {
                p.currentLocation = dotToPoint(c1.x, c1.y);
            }
            double x = p.currentLocation.getX();
            double y = p.currentLocation.getY();

//            if (selectedPerformers.containsKey(p.getSymbol() + p.getLabel())) {
//                g.setColor(selectedPerformers.get(p.getSymbol() + p.getLabel()).getColor());
//            }

            g.setColor(c1.getColor()); // Default is coordinate color. Remove this?
            if (effectManager != null) {
                Effect currentEffect = effectManager.getEffect(p);

                // No effect is present at the current count
                if (currentEffect == null) {
                    g.setColor(new Color(0,0,0, effectTransparency));
                } else {
                    Color effectColor = calculateColor(currentEffect); // TODO eventually: Calculate phase of color shift from effect
                    //System.out.println(effectColor);
                    Color displayColor = new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), effectTransparency);
                    g.setColor(displayColor);
                }
            }

            g.fillRect((int)x-6,(int)y-6,6,12);
            g.fillRect((int)x,(int)y-6,6,12);
            if (selectedPerformers.get(p.getSymbol()+p.getLabel()) != null) {
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.BLACK);
            }
            g.drawRect((int)x-7,(int)y-7,14,14);
            g.drawRect((int)x-6,(int)y-6,12,12);
            g.drawLine((int)x,(int)y-5,(int)x,(int)y+6);
        }

        if (selecting) {
            int w = Math.abs(selectStartX - selectEndX);
            int h = Math.abs(selectStartY - selectEndY);
            int x = Math.min(selectStartX, selectEndX);
            int y = Math.min(selectEndY, selectStartY);
            g.setColor(new Color(0, 100, 100, 100));
            g.fillRect(x, y, w, h);
        }
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

            for (Performer p : drill.performers) {
                double px = p.currentLocation.getX();
                double py = p.currentLocation.getY();

                int bxmin = (int) (px - 7);
                int bymin = (int) (py - 7);
                int bxmax = (int) (px + 7);
                int bymax = (int) (py + 7);

                boolean intersecting = AABB(axmin, aymin, axmax, aymax, bxmin, bymin, bxmax, bymax);

                if (intersecting) {
                    String key = p.getSymbol() + p.getLabel();
                    if (isControlDown) {
                        if (selectedPerformers.containsKey(key)) {
                            selectedPerformers.remove(key); // Deselect if already selected
                            footballFieldListener.onPerformerDeselect();
                        }
                        else {
                            selectedPerformers.put(key, p); // Select if not already selected
                            footballFieldListener.onPerformerSelect();
                        }
                    } else {
                        selectedPerformers.put(key, p);
                        footballFieldListener.onPerformerSelect();
                    }
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
}
