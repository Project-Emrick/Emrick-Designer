package org.emrick.project;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class FootballFieldPanel extends JPanel {
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
    private boolean ctrlHeld = false;
    private Set currentSet;
    private int currentCount = 0;
    private int currentSetStartCount = 0;
    private SerialTransmitter serialTransmitter;

    public FootballFieldPanel() {
//        setPreferredSize(new Dimension(fieldWidth + 2*margin, fieldHeight + 2*margin)); // Set preferred size for the drawing area
        setMinimumSize(new Dimension(1042, 548));
        drill = new Drill();
        selectedPerformers = new HashMap<>();
        this.addMouseListener(new MouseInput());
        colorChosen = Color.BLACK;
    }

    public FootballFieldPanel(Color colorChosen) {
        this.colorChosen = colorChosen;
        setMinimumSize(new Dimension(1042, 548));
        drill = new Drill();
        selectedPerformers = new HashMap<>();
        this.addMouseListener(new MouseInput());
    }

    public void addSetToField(Set set) {
        currentSet = set;
        if (!set.equals("0")) {
            serialTransmitter.writeSet(set.index);
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the surface image
        if (surfaceImage != null) {
            drawBetterImage(g, surfaceImage);
        }

        // Draw the floorCover image on top
        if (floorCoverImage != null) {
            drawBetterImage(g, floorCoverImage);
        }
//        for (Performer p : drill.performers) { // Replace 'performers' with your actual collection of performers.
//            g.setColor(p.getColor()); // This will use the color stored in the performer.
//            // Paint the performer
//        }

        // Draw performers with their colors
        for (Performer p : drill.performers) {
            g.setColor(p.getColor()); // This will use the color stored in the performer.

            Coordinate c1 = p.getCoordinateFromSet(currentSet.label);
            if (currentSet.index < drill.sets.size() - 1) {
                Coordinate c2 = p.getCoordinateFromSet(drill.sets.get(currentSet.index + 1).label);
                if (c1.x == c2.x && c1.y == c2.y || currentCount == currentSetStartCount) {
                    p.currentLocation = dotToPoint(c1.x, c1.y);
                } else {
                    int duration = drill.sets.get(currentSet.index + 1).duration;
                    p.currentLocation = dotToPoint((c2.x - c1.x) * (double) (currentCount - currentSetStartCount) / duration + c1.x, (c2.y - c1.y) * (double) (currentCount - currentSetStartCount) / duration + c1.y);
                }
            } else {
                p.currentLocation = dotToPoint(c1.x, c1.y);
            }
            double x = p.currentLocation.getX();
            double y = p.currentLocation.getY();

//            if (selectedPerformers.containsKey(p.getSymbol() + p.getLabel())) {
//                g.setColor(selectedPerformers.get(p.getSymbol() + p.getLabel()).getColor());
//            }
            g.setColor(c1.getColor());

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

    private class MouseInput implements MouseInputListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            int mx = e.getX();
            int my = e.getY();

            for (Performer p : drill.performers) {
                double px = p.currentLocation.getX();
                double py = p.currentLocation.getY();
                if (mx >= px - 7 && mx <= px + 7 && my >= py - 7 && my <= py + 7) {
                    if (e.isControlDown()) {

                        String key = p.getSymbol() + p.getLabel();
                        if (selectedPerformers.containsKey(key)) {
                            selectedPerformers.remove(key); // Deselect if already selected
                        } else {
                            selectedPerformers.put(key, p); // Select if not already selected
                        }
                    } else {
                        if (selectedPerformers.containsKey(p.getSymbol() + p.getLabel())) {
                            selectedPerformers.clear();
                        } else {
                            selectedPerformers.clear();
                            selectedPerformers.put(p.getSymbol() + p.getLabel(), p);
                        }
                    }
                    repaint();
                    break;
                }
            }
        }


        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {}
    }
    public void setCurrentSet(Set currentSet) {
        this.currentSet = currentSet;
    }
}
