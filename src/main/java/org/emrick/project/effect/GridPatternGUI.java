package org.emrick.project.effect;

import org.emrick.project.Performer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class GridPatternGUI implements ActionListener {

    // Dialog
    private final JDialog dialog;

    // Labels
    JLabel patternLabel = new JLabel("Pattern:  ");
    JLabel motionLabel = new JLabel("Motion:  ");
    JLabel colorLabel = new JLabel("Color:  ");
    JLabel durationLabel = new JLabel("Duration (s):  ");

    // Pattern parameter
    JComboBox<String> patternsComboBox;
    JComboBox<String> motionsComboBox;
    JButton colorButton;
    JTextField durationField;

    // Action buttons
    JButton cancelButton;
    JButton confirmButton;

    // Color
    Color selectedColor = new Color(0,0,0);

    // Grid pattern helper object -- Help perform calculations. Let this class be GUI alone.
    GridPatternHelper gridPatternHelper;
    // Effect manager helper object
    EffectManager effectManager;
    // Necessary parameters
    long startTimeMSec;

    public GridPatternGUI(JFrame parent, ArrayList<Performer> selectedPerformers, EffectManager effectManager,
                          long startTimeMSec) {
        this.effectManager = effectManager;
        this.startTimeMSec = startTimeMSec;

        // Initialize dialog
        dialog = new JDialog(parent, true);
        dialog.setTitle("Create Grid Pattern");
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setSize(400, 400);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);

        // Use GridPatternHelper to determine the dimensions of the selected grid
        gridPatternHelper = new GridPatternHelper(selectedPerformers, effectManager);
        gridPatternHelper.setStartTimeMSec(startTimeMSec);
        int width = gridPatternHelper.getWidth();
        int height = gridPatternHelper.getHeight();

        // Create a JLabel for performer selection prompt
        JLabel selectionLabel = new JLabel("<html><h3>Create a pattern for a " + width + " by " + height +
                " grid of performers</h3></html>");

        // Create a JComboBox dropdown menu with pattern options
        String[] patterns = {"Line Sweep"};
        patternsComboBox = new JComboBox<>(patterns);
        patternsComboBox.setFocusable(false);

        // Create a JComboBox dropdown menu for motion parameter
        String[] motions = {"Top-Bottom"};
        motionsComboBox = new JComboBox<>(motions);
        motionsComboBox.setFocusable(false);

        // Create a JButton for selecting color
        colorButton = new JButton();
        colorButton.setPreferredSize(new Dimension(20, 20));
        colorButton.setFocusable(false);
        colorButton.addActionListener(this);
        colorButton.setBackground(selectedColor);

        // Create a JTextField for entering duration
        durationField = new JTextField(10);

        // Create JPanel for tweaking parameters
        JPanel parametersPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();

        //////////////// 0th Row ////////////////
        gc.weightx = 1;
        gc.weighty = 0.5;

        gc.gridx = 0;
        gc.gridy = 0;

        // Let this next element consume 2 columns and 1 row
        gc.gridwidth = 2;
        gc.gridheight = 1;

        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        parametersPanel.add(selectionLabel, gc);

        gc.gridwidth = 1;
        gc.gridheight = 1;

        //////////////// 1st Row ////////////////
        gc.gridy = 1;

        // Row for selecting the motion
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.LINE_END;
        parametersPanel.add(patternLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        parametersPanel.add(patternsComboBox, gc);

        //////////////// 2nd Row ////////////////
        gc.gridy = 2;

        // Row for selecting the motion
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.LINE_END;
        parametersPanel.add(motionLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        parametersPanel.add(motionsComboBox, gc);

        //////////////// 3rd Row ////////////////
        gc.gridy = 3;

        // Row for selecting the motion
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.LINE_END;
        parametersPanel.add(colorLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        parametersPanel.add(colorButton, gc);

        //////////////// 4th Row ////////////////
        gc.gridy = 4;

        // Row for selecting the motion
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.LINE_END;
        parametersPanel.add(durationLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        parametersPanel.add(durationField, gc);

        //////////////// 5th Row //////////////// To push rest of contents upwards -- Dummy row
        gc.gridy = 5;
        gc.weighty = 0.75;

        parametersPanel.add(new JPanel(), gc);

        // Create cancel and confirm buttons
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(this);

        // Create a JPanel for holding buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        buttonPanel.add(Box.createHorizontalGlue()); // Push buttons to the right
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Space buttons apart
        buttonPanel.add(confirmButton);

        dialog.add(parametersPanel);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source.equals(colorButton)) {

            // Allow user to choose a color
            selectedColor = JColorChooser.showDialog(dialog, "Choose Color", selectedColor);

            // Update the button with the selected color
            if (selectedColor != null) {
                colorButton.setBackground(selectedColor);
            }
        }
        else if (source.equals(cancelButton)) {
            dialog.dispose();
        }
        else if (source.equals(confirmButton)) {

            // Check that fields are entered correctly
            long durationMSec = 0;
            try {
                durationMSec = (long) (Float.parseFloat(durationField.getText()) * 1000);
            } catch (NumberFormatException nfe) {
                System.out.println("GridPatternGUI: NumberFormatException - " + nfe.getMessage());
                JOptionPane.showMessageDialog(dialog, "Please provide a proper duration in seconds.",
                        "Parameter Error", JOptionPane.ERROR_MESSAGE);
            }

            // Notify GridPatternHelper of selected parameters from GUI
            gridPatternHelper.setDurationMSec(durationMSec);
            gridPatternHelper.setColor(selectedColor);

            // Attempt to apply the effects for the pattern
            // Note: It doesn't matter that the effect can't be applied to one or more performers. Job is done for all
            //  performers where the effect can be applied. (I.e., not all or nothing).
            gridPatternHelper.createPatternTopDownLineSweep();

            dialog.dispose();
        }
    }

    public static void main(String[] args) {
        JFrame dummyParent = new JFrame();

        ArrayList<Performer> performers = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                Performer p = new Performer();
                p.currentLocation.setLocation(x, y);
//                p.setSymbol(Integer.toString(x));
//                p.setLabel(y);
                p.setLabel(x);
                performers.add(p);
            }
        }
        // Old testing
//        new GridPatternGUI(dummyParent, performers);
    }

}
