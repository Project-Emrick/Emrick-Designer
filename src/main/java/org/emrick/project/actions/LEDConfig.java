package org.emrick.project.actions;

public class LEDConfig {
    private int ledCount;
    private int height;
    private int width;
    private int hOffset;
    private int vOffset;
    private String label;

    public LEDConfig() {
        ledCount = 50;
        height = 12;
        width = 6;
        hOffset = -6;
        vOffset = -6;
        label = "L";
    }

    public LEDConfig(int ledCount, int height, int width, int hOffset, int vOffset, String label) {
        this.ledCount = ledCount;
        this.height = height;
        this.width = width;
        this.hOffset = hOffset;
        this.vOffset = vOffset;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getLEDCount() {
        return ledCount;
    }

    public void setLEDCount(int ledCount) {
        this.ledCount = ledCount;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int gethOffset() {
        return hOffset;
    }

    public void sethOffset(int hOffset) {
        this.hOffset = hOffset;
    }

    public int getvOffset() {
        return vOffset;
    }

    public void setvOffset(int vOffset) {
        this.vOffset = vOffset;
    }
}
