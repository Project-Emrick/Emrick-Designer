package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.actions.EffectLEDStripMap;

import java.awt.*;
import java.time.Duration;
import java.util.*;

public class GridEffect implements GeneratedEffect {
    private long startTime;
    private long endTime;
    private int height;
    private int width;
    private GridShape[] shapes;
    private Duration duration;
    private int id;


    // TODO: add option for double width movement

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
        e.setGeneratedEffect(this);
        return e;
    }

    @Override
    public ArrayList<EffectLEDStripMap> generateEffects(ArrayList<LEDStrip> ledStrips) {
        // remove duplicates
        HashSet<LEDStrip> stripSet = new HashSet<>(ledStrips);
        ledStrips.clear();
        ledStrips.addAll(stripSet);

        LEDStrip[][] grid = buildGrid(ledStrips, width, height);
        ArrayList<EffectLEDStripMap> effects = new ArrayList<>();
        for (int i = 0; i < shapes.length; i++) {
            GridShape shape = shapes[i];
            int iterations = Math.max(Math.abs(shape.getMovement().x), Math.abs(shape.getMovement().y)) + 1;
            long prevDuration = 0;
            for (int j = 0; j < iterations; j++) {
                long time = duration.toMillis() / iterations;
                ArrayList<LEDStrip> strips = new ArrayList<>();
                Point curPos = new Point();
                curPos.x = (int) Math.round((double) shape.getStartPos().x + (double) shape.getMovement().x / ((double)iterations - 1.0) * (double) j * (double) shape.getSpeed());
                curPos.y = (int) Math.round((double) shape.getStartPos().y + (double) shape.getMovement().y / ((double)iterations - 1.0) * (double) j);
                for (int k = curPos.y; k < curPos.y + shape.getShape().length; k++) {
                    for (int l = curPos.x; l < curPos.x + shape.getShape()[k - curPos.y].length; l++) {
                        if (shape.getShape()[k - curPos.y][l - curPos.x]) {
                            if (k < height && l < width && k >= 0 && l >= 0
                                    && grid[k][l] != null) {
                                strips.add(grid[k][l]);
                            }
                        }
                    }
                }
                for (LEDStrip strip : strips) {
                    for (int k = 0; k < effects.size(); k++) {
                        EffectLEDStripMap effect = effects.get(k);
                        if (effect.getLedStrip().equals(strip)) {
                            // calculate overlap
                            long start = effect.getEffect().getStartTimeMSec();
                            long end = effect.getEffect().getEndTimeMSec();
                            long overlap = 0;
                            long overlapStart;
                            if (start >= startTime + prevDuration) {
                                overlap = (startTime + prevDuration + time) - start;
                                overlapStart = start;
                            } else {
                                overlap = end - (startTime + prevDuration);
                                overlapStart = startTime + prevDuration;
                            }
                            if (overlap > 0) {
                                //System.out.println(effect.getEffect().getStartColor() + ", " + effect.getLedStrip());
                                if (effect.getEffect().getStartColor().equals(Color.BLACK)) {
                                    Effect e = new Effect(overlapStart);
                                    e.setEffectType(EffectList.GRID);
                                    e.setStartColor(Color.BLACK);
                                    e.setDelay(Duration.ofMillis(overlap));
                                    e.setDO_DELAY(true);
                                    e.setUSE_DURATION(false);
                                    e.setId(id);
                                    e.setGeneratedEffect(this);
                                    effects.add(new EffectLEDStripMap(e, strip));
                                    effects.remove(effect);
                                } else {
                                    // throw error
                                }
                            }
                        }
                    }


                    Effect e = new Effect(startTime + prevDuration);
                    e.setEffectType(EffectList.GRID);
                    e.setStartColor(shape.getColor());
                    e.setDelay(Duration.ofMillis(time));
                    e.setDO_DELAY(true);
                    e.setUSE_DURATION(false);
                    e.setId(id);
                    e.setGeneratedEffect(this);
                    effects.add(new EffectLEDStripMap(e, strip));
                }
                prevDuration += time;
            }
        }

        // fill in the space between patterns

        for (LEDStrip ledStrip : ledStrips) {
            ArrayList<EffectLEDStripMap> maps = new ArrayList<>();
            for (int i = 0 ; i < effects.size(); i++) {
                if (effects.get(i).getLedStrip().equals(ledStrip)) {
                    maps.add(effects.get(i));
                }
            }
            maps.sort(Comparator.comparingLong((o) -> o.getEffect().getStartTimeMSec()));
            long lastEnd = startTime;
            for (int i = 0; i < maps.size(); i++) {
                if (maps.get(i).getEffect().getStartTimeMSec() > lastEnd + 1) {
                    Effect e = new Effect(lastEnd);
                    e.setEffectType(EffectList.GRID);
                    e.setStartColor(Color.BLACK);
                    e.setDelay(Duration.ofMillis(maps.get(i).getEffect().getStartTimeMSec() - lastEnd));
                    e.setDO_DELAY(true);
                    e.setUSE_DURATION(false);
                    e.setId(id);
                    e.setGeneratedEffect(this);
                    effects.add(new EffectLEDStripMap(e, ledStrip));
                }
                lastEnd = maps.get(i).getEffect().getEndTimeMSec();
            }
            if (lastEnd < endTime - 1) {
                Effect e = new Effect(lastEnd);
                e.setEffectType(EffectList.GRID);
                e.setStartColor(Color.BLACK);
                e.setDelay(Duration.ofMillis(endTime - lastEnd));
                e.setDO_DELAY(true);
                e.setUSE_DURATION(false);
                e.setId(id);
                e.setGeneratedEffect(this);
                effects.add(new EffectLEDStripMap(e, ledStrip));
            }
        }
        return effects;
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
