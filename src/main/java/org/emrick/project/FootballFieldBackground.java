package org.emrick.project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

public class FootballFieldBackground extends JPanel {
    private FootballFieldListener footballFieldListener;
    // Loading field decor.
    private BufferedImage surfaceImage;
    private BufferedImage floorCoverImage;
    private final BufferedImage dummyImage = new BufferedImage(2635, 1155, BufferedImage.TYPE_INT_ARGB);
    private BufferedImage fullImage;
    private double ratio;
    private boolean showSurfaceImage = true;
    private boolean showFloorCoverImage = true;
    private double fieldWidth;
    private double fieldHeight;
    private Point frontSideline50;
    private boolean heightBound;
    public boolean justResized = false;
    public FootballFieldBackground(FootballFieldListener footballFieldListener) {
        this.footballFieldListener = footballFieldListener;
        fieldHeight = 0;
        fieldWidth = 0;
        ratio = 0;
        heightBound = false;
        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                justResized = true;
                repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
        Graphics g = dummyImage.getGraphics();
        g.setColor(new Color(0xBFBFBF));
        g.fillRect(0, 0, dummyImage.getWidth(), dummyImage.getHeight());
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (justResized) {
            double widthRatio = (double) getWidth() / dummyImage.getWidth();
            double heightRatio = (double) getHeight() / dummyImage.getHeight();
            ratio = Math.min(widthRatio, heightRatio);
            if (ratio == heightRatio) {
                heightBound = true;
            } else {
                heightBound = false;
            }
            BufferedImage scaledImage = new BufferedImage((int) (dummyImage.getWidth() * ratio), (int) (dummyImage.getHeight() * ratio), BufferedImage.TYPE_INT_RGB);
            Graphics g1 = scaledImage.getGraphics();
            if (!showSurfaceImage || (floorCoverImage == null && surfaceImage == null)) {
                drawBetterImage(g1, dummyImage); // For accurate plotting, need some image reference
            }
            if (surfaceImage != null && showSurfaceImage) {
                drawBetterImage(g1, surfaceImage);
            }

            // Draw the floorCover image on top
            if (floorCoverImage != null && showFloorCoverImage) {
                drawBetterImage(g1, floorCoverImage);
            }

            fullImage = scaledImage;
        }
        int x = (getWidth() - (int) fieldWidth) / 2;
        int y = (getHeight() - (int) fieldHeight) / 2;
        g.drawImage(fullImage, x, y, (int) fieldWidth, (int) fieldHeight, this);
        if (justResized) {
            justResized = false;
            footballFieldListener.onResizeBackground();
        }
    }

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

        g.drawImage(image, 0, 0, width, height, this);
    }

    public boolean isShowSurfaceImage() {
        return showSurfaceImage;
    }

    public void setShowSurfaceImage(boolean showSurfaceImage) {
        this.showSurfaceImage = showSurfaceImage;
    }

    public boolean isShowFloorCoverImage() {
        return showFloorCoverImage;
    }

    public void setShowFloorCoverImage(boolean showFloorCoverImage) {
        this.showFloorCoverImage = showFloorCoverImage;
    }

    public BufferedImage getSurfaceImage() {
        return surfaceImage;
    }

    public void setSurfaceImage(BufferedImage surfaceImage) {
        this.surfaceImage = surfaceImage;
    }

    public BufferedImage getFloorCoverImage() {
        return floorCoverImage;
    }

    public void setFloorCoverImage(BufferedImage floorCoverImage) {
        this.floorCoverImage = floorCoverImage;
    }

    public BufferedImage getDummyImage() {
        return dummyImage;
    }

    public double getFieldWidth() {
        return fieldWidth;
    }

    public void setFieldWidth(double fieldWidth) {
        this.fieldWidth = fieldWidth;
    }

    public double getFieldHeight() {
        return fieldHeight;
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
