import java.awt.*;
import java.util.List;

public class Effect {

    /**
     * Changes the color of selected dots based on their IDs.
     *
     * @param dots A list of Coordinate objects representing dots.
     * @param selectedIds A list of IDs for the dots whose color should be changed.
     * @param newColor The new color to apply to the selected dots.
     */
    public void changeSelectedDotsColor(List<Coordinate> dots, List<String> selectedIds, Color newColor) {
        for (Coordinate dot : dots) {
            if (selectedIds.contains(dot.getId())) {
                dot.setColor(newColor); // Use the setColor method
            }
        }
        //  not sure which way can repaint successfully
        //        SwingUtilities.invokeLater(() -> footballFieldPanel.repaint());

    }
}
