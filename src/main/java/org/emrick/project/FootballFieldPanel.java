package org.emrick.project;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

class FootballFieldPanel extends JPanel {
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

    public FootballFieldPanel() {
//        setPreferredSize(new Dimension(fieldWidth + 2*margin, fieldHeight + 2*margin)); // Set preferred size for the drawing area
        setMinimumSize(new Dimension(1042, 548));
        drill = new Drill();
        selectedPerformers = new HashMap<>();
        this.addMouseListener(new MouseInput());
        colorChosen = Color.RED;
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
//            g.drawImage(surfaceImage, 0, 0, this.getWidth(), this.getHeight(), this);
            drawBetterImage(g, surfaceImage);
        }

        // Draw the floorCover image on top
        if (floorCoverImage != null) {
            // Adjust the x, y, width, and height as needed
//            g.drawImage(floorCoverImage, 0, 0, this.getWidth(), this.getHeight(), this);
            drawBetterImage(g, floorCoverImage);
        }

        // (Carried Over) Adjust dot drawing to account for the margin
        for (Performer p : drill.performers) {
            //double adjustedX = Math.min(dot.x, fieldWidth); // Adjust for margin
            //double adjustedY = Math.min(dot.y, fieldHeight); // Adjust for margin
            Coordinate c = p.getCoordinateFromSet(currentSet.label);
            p.currentLocation = dotToPoint(c.x,c.y);
            double x = p.currentLocation.getX();
            double y = p.currentLocation.getY();
            g.setColor(colorChosen);
            g.fillRect((int)x-6,(int)y-6,6,12);
            g.setColor(colorChosen);
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
            if (e.isControlDown()) {
                for (Performer p : drill.performers) {
                    Coordinate c = p.getCoordinateFromSet(currentSet.label);
                    p.currentLocation = dotToPoint(c.x,c.y);
                    int px = (int)p.currentLocation.getX();
                    int py = (int)p.currentLocation.getY();
                    if (mx <= px+7 && my <= py+7 && mx >= px-7 && my >= py-7) {
                        selectedPerformers.put(p.getSymbol()+p.getLabel(), p);
                        break;
                    }
                }
            } else {
                for (Performer p : drill.performers) {
                    Coordinate c = p.getCoordinateFromSet(currentSet.label);
                    p.currentLocation = dotToPoint(c.x,c.y);
                    double px = p.currentLocation.getX();
                    double py = p.currentLocation.getY();
                    if (mx <= px+7 && my <= py+7 && mx >= px-7 && my >= py-7) {
                        selectedPerformers = new HashMap<>();
                        selectedPerformers.put(p.getSymbol()+p.getLabel(), p);
                        break;
                    }
                    selectedPerformers = new HashMap<>();
                }
            }
            repaint();
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
}
