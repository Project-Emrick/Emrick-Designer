package org.emrick.project;

/**
 * Listen to important events whose source comes from FootballFieldPanel. For example, when a performer is selected.
 */
public interface FootballFieldListener extends RFSignalListener {
    void onPerformerSelect();
    void onPerformerDeselect();
    void onResizeBackground();
    void onFinishRepaint();
    double getFrameRate();
}
