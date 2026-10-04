package org.emrick.project.dev;

import org.emrick.project.LEDStrip;

import java.util.List;

/** Looks up boards in the open project's LED strip list (the same data the Device IDs CSV is built from). */
final class ProjectUnits {
    private ProjectUnits() {
    }

    static LEDStrip byId(List<LEDStrip> strips, int id) {
        for (LEDStrip strip : strips) {
            if (strip.getId() == id) {
                return strip;
            }
        }
        return null;
    }

    static LEDStrip byLabel(List<LEDStrip> strips, String label) {
        for (LEDStrip strip : strips) {
            if (strip.getLabel().equalsIgnoreCase(label.trim())) {
                return strip;
            }
        }
        return null;
    }

    static int maxId(List<LEDStrip> strips) {
        int max = -1;
        for (LEDStrip strip : strips) {
            max = Math.max(max, strip.getId());
        }
        return max;
    }

    static int ledCount(LEDStrip strip) {
        return strip.getLedConfig() == null ? 0 : strip.getLedConfig().getLEDCount();
    }

    /**
     * Compares what a unit reports against the open project.
     * @return a short human-readable verdict, or "" if no project is open
     */
    static String check(List<LEDStrip> strips, UnitInfo info) {
        if (strips.isEmpty() || info == null) {
            return "";
        }
        LEDStrip strip = byId(strips, info.id);
        if (strip == null) {
            return "Board ID " + info.id + " is not in the open project";
        }
        StringBuilder problems = new StringBuilder();
        if (info.label != null && !info.label.isEmpty() && !info.label.equalsIgnoreCase(strip.getLabel())) {
            problems.append("project says ID ").append(info.id).append(" is ").append(strip.getLabel())
                    .append(" but the unit is labeled ").append(info.label).append("; ");
        }
        if (ledCount(strip) != 0 && ledCount(strip) != info.leds) {
            problems.append("project has ").append(ledCount(strip)).append(" LEDs, unit has ").append(info.leds).append("; ");
        }
        if (problems.length() == 0) {
            return "Matches project (" + strip.getLabel() + ")";
        }
        return "Mismatch: " + problems.substring(0, problems.length() - 2);
    }
}
