package org.emrick.project.effect;

import org.emrick.project.Performer;

import java.awt.*;
import java.time.Duration;
import java.util.*;

public class GridPatternHelper {

    private ArrayList<Performer> selectedPerformers;
    private Performer[][] selectedPerformers2D;
    private int width = 0;
    private int height = 0;

    // Hyper parameter
    private double locationErrorTerm = 0;

    // Helper classes
    private EffectManager effectManager;

    // Necessary parameters
    private long startTimeMSec = -1;
    private long durationMSec = -1;
    private Color color;

    public GridPatternHelper(ArrayList<Performer> selectedPerformers, EffectManager effectManager) {
        this.selectedPerformers = selectedPerformers;
        this.effectManager = effectManager;
        computeSelectedPerformers2D();
    }

    private void computeSelectedPerformers2D() {

        // Find the top left performer of grid (minimize x + y coordinate)
        Performer topLeftPerformer = selectedPerformers.stream().min(
                Comparator.comparing(v -> v.currentLocation.getX() + v.currentLocation.getY())).get();

        // Find the top right performer of grid (maximize x - y coordinate)
        Performer topRightPerformer = selectedPerformers.stream().max(
                Comparator.comparing(v -> v.currentLocation.getX() - v.currentLocation.getY())).get();

        // Find the greater and lesser Y coordinate between top left and right performers
        double greaterY = Math.max(topLeftPerformer.currentLocation.getY(), topRightPerformer.currentLocation.getY());
        double lesserY = Math.min(topLeftPerformer.currentLocation.getY(), topRightPerformer.currentLocation.getY());

        locationErrorTerm = (greaterY - lesserY);

        // Use greater and lesser Y to search through selected performers whose location falls within this Y range
        double upperBoundY = greaterY + locationErrorTerm;
        double lowerBoundY = lesserY - locationErrorTerm;

        // Those whose Y value falls in the range are considered to be in the top row
        ArrayList<Performer> topRowPerformers = new ArrayList<>();
        for (Performer performer : selectedPerformers) {
            double currentY = performer.currentLocation.getY();
            if (lowerBoundY <= currentY && currentY <= upperBoundY) {
                topRowPerformers.add(performer);
            }
        }

        // Get the width by using number of performers in the top row
        width = topRowPerformers.size();

        // Get the height by using the width
        height = selectedPerformers.size() / width;

        // Width corresponds to column, height to row
        selectedPerformers2D = new Performer[height][width];

        // assert(width * height == selectedPerformers.size());
        if (width * height != selectedPerformers.size()) {
            System.out.println("GridPatternHelper Computation Error: width * height != selectedPerformers");
        }

        // Sort the performers by their Y coordinate, least to greatest
        selectedPerformers.sort(new Comparator<Performer>() {
            @Override
            public int compare(Performer p1, Performer p2) {
                Double p1y = p1.currentLocation.getY();
                Double p2y = p2.currentLocation.getY();
                return p1y.compareTo(p2y);
            }
        });

        // TODO: Remove me, verification
//        for (Performer performer : selectedPerformers) {
//            System.out.println(performer.currentLocation.getY());
//        }
        System.out.println("GridPatternHelper: Width = " + width);
        System.out.println("GridPatternHelper: Height = " + height);

        // Fill in the 2D array
        ArrayList<Performer> remaining = new ArrayList<>(selectedPerformers);
        int row = 0;
        while (!remaining.isEmpty()) {

            // Find top 'w' performers with smallest Y value (smaller Y = towards top); array is sorted
            ArrayList<Performer> rowOfPerformers = new ArrayList<>();
            for (int i = 0; i < width; i++) {
                rowOfPerformers.add(remaining.get(i));
            }

            // Sort this row of performers by their X value
            rowOfPerformers.sort(new Comparator<Performer>() {
                @Override
                public int compare(Performer p1, Performer p2) {
                    Double p1x = p1.currentLocation.getX();
                    Double p2x = p2.currentLocation.getX();
                    return p1x.compareTo(p2x);
                }
            });

            // Insert this row of performers into the 2D array
            for (int col = 0; col < rowOfPerformers.size(); col++) {
                selectedPerformers2D[row][col] = rowOfPerformers.get(col);
            }

            // Remove performers in this row from list of remaining performers to evaluate
            for (Performer performer : rowOfPerformers) {
                remaining.remove(performer);
            }

            row += 1;
        }

        // System.out.println(Arrays.deepToString(selectedPerformers2D));
    }

    public void createPatternTopDownLineSweep() {

        // These parameters are needed to know how to assign effects
        if (startTimeMSec == -1 || durationMSec == -1 || color == null) {
            return;
        }

        // Find duration per row based on height
        long durationMSecPerRow = durationMSec / height;

        // For each row of the grid, assign the appropriate effect
        for (int row = 0; row < height; row++) {
            long startMSec = startTimeMSec + row * durationMSecPerRow;
            Effect effect = new Effect(startMSec,
                    color, color, Duration.ZERO, Duration.ofMillis(durationMSecPerRow), Duration.ZERO,
                    true, true, true, true);

            System.out.println("GridPatternHelper: startMSec = " + startMSec);

            // Assign this effect to each performer of the row
            for (int col = 0; col < width; col++) {
                Performer currPerformer = selectedPerformers2D[row][col];
                effectManager.addEffect(effect, currPerformer);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setStartTimeMSec(long startTimeMSec) {
        this.startTimeMSec = startTimeMSec;
    }

    public void setDurationMSec(long durationMSec) {
        this.durationMSec = durationMSec;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
