package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.actions.EffectLEDStripMap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class GridEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private int height;
    private int width;
    private GridShape[] shapes;
    private Duration duration;
    private int id;

    public GridEffect(long startTime, long endTime, int height, int width, GridShape[] shapes, Duration duration, int id) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.height = height;
        this.width = width;
        this.shapes = shapes;
        this.duration = duration;
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
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

    public GridShape[] getShapes() {
        return shapes;
    }

    public void setShapes(GridShape[] shapes) {
        this.shapes = shapes;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public EffectList getEffectType() {
        return EffectList.GRID;
    }

    @Override
    public Effect generateEffectObj() {
        Effect e = new Effect(startTime);
        e.setEndTimeMSec(endTime);
        e.setHeight(height);
        e.setWidth(width);
        e.setShapes(shapes);
        e.setDuration(duration);
        e.setId(id);
        e.setEffectType(EffectList.GRID);
        return e;
    }

    @Override
    public ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips) {
        LEDStrip[][] grid = buildGrid(ledStrips, width, height);

        return null;
    }

    public static LEDStrip[][] buildGrid(ArrayList<LEDStrip> ledStrips, int width, int height) {
        LEDStrip[][] grid = new LEDStrip[height][width];
        ledStrips.sort(Comparator.comparingInt(o -> (int) o.getPerformer().currentLocation.getY()));
        int next = 0;
        HashSet<Integer> uniqueXPositions = new HashSet<>();
        for (int i = 0; i < height; i++) {
            ArrayList<LEDStrip> row = new ArrayList<>();
            for (int j = 0; j < width; j++) {
                if (next < ledStrips.size()) {
                    uniqueXPositions.add((int) ledStrips.get(next).getPerformer().currentLocation.getX());
                    if (j > 0) {
                        if (ledStrips.get(next).getPerformer().currentLocation.getY() == ledStrips.get(next - 1).getPerformer().currentLocation.getY()) {
                            row.add(ledStrips.get(next));
                            next++;
                        } else {
                            break;
                        }
                    } else {
                        row.add(ledStrips.get(next));
                        next++;
                    }
                } else {
                    break;
                }
            }
            row.sort(Comparator.comparingInt(o -> (int) o.getPerformer().currentLocation.getX()));
            grid[i] = row.toArray(grid[i]);
        }
        ArrayList<Integer> positions = new ArrayList<>(uniqueXPositions);
        positions.sort(Comparator.comparingInt(o -> (int) o));
        int[] xPosOccurrences = new int[uniqueXPositions.size()];
        for (int i = 0; i < height; i++) {
            int[] tmpOccurrences = new int[uniqueXPositions.size()];
            for (int j = 0; j < width; j++) {
                if (grid[i][j] != null) {
                    tmpOccurrences[positions.indexOf((int) grid[i][j].getPerformer().currentLocation.getX())]++;
                }
            }
            for (int j = 0; j < tmpOccurrences.length; j++) {
                if (tmpOccurrences[j] > xPosOccurrences[j]) {
                    xPosOccurrences[j] = tmpOccurrences[j];
                }
            }
        }
        int[] colX = new int[width];
        int offset = 0;
        for (int i = 0; i < uniqueXPositions.size(); i++) {
            for (int j = 0; j < xPosOccurrences[i]; j++) {
                colX[offset] = positions.get(i);
                offset++;
            }
        }


        for (int i = width - 1; i >= 0; i--) {
            for (int j = 0; j < height; j++) {
                if (grid[j][i] != null) {
                    if ((int) grid[j][i].getPerformer().currentLocation.getX() != colX[i]) {
                        int k = width - 1;
                        while (k > j && (int) grid[j][i].getPerformer().currentLocation.getX() != colX[k]
                                || grid[j][k] != null) {
                            k--;
                        }
                        if ((int) grid[j][i].getPerformer().currentLocation.getX() == colX[k]) {
                            grid[j][k] = grid[j][i];
                            grid[j][i] = null;
                        }
                    }
                }
            }
        }
        return grid;
    }
}
