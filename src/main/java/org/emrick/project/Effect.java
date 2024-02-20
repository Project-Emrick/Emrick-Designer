package org.emrick.project;

import java.awt.*;
import java.util.List;

public class Effect {

    /**
     * Changes the color of selected dots based on their IDs.
     *
     * @param dots A list of Coordinate objects representing dots.
     * @param newColor The new color to apply to the selected dots.
     */
    public void changeSelectedDotsColor(List<Coordinate> dots, Color newColor) {
        for (Coordinate dot : dots) {
            dot.setColor(newColor); // Use the setColor method

        }
        //  not sure which way can repaint successfully
        //        SwingUtilities.invokeLater(() -> footballFieldPanel.repaint());

    }
}
