import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;


class FootballFieldPanel extends JPanel {
    private final List<Point> dotCoordinates = new ArrayList<>();
    private final int fieldWidth = 720; // Width of the football field
    private final int fieldHeight = 360;

    public FootballFieldPanel() {
        setPreferredSize(new Dimension(500, 300)); // Set preferred size for the drawing area
    }

    public void addDot(int x, int y) {
        dotCoordinates.add(new Point(x, y));
        repaint(); // Repaint the panel to show the new dot
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the football field background
        g.setColor(new Color(92,255,103));
        g.fillRect(0, 0, fieldWidth, fieldHeight); // Position locked to the top left

        // Draw field lines
        g.setColor(Color.WHITE);
        // Outer borders
        g.drawRect(0, 0, fieldWidth, fieldHeight);
        // Midfield line
        g.drawLine(fieldWidth / 2, 0, fieldWidth / 2, fieldHeight);
        // Center circle
        g.drawOval((fieldWidth / 2) - (fieldHeight / 10), (fieldHeight / 2) - (fieldHeight / 10), fieldHeight / 5, fieldHeight / 5);
        // Goal areas
        g.drawRect(0, (fieldHeight / 2) - (fieldHeight / 4), fieldWidth / 10, fieldHeight / 2);
        g.drawRect(fieldWidth - (fieldWidth / 10), (fieldHeight / 2) - (fieldHeight / 4), fieldWidth / 10, fieldHeight / 2);

        // Draw red dots for each coordinate
        g.setColor(Color.RED);
        for (Point dot : dotCoordinates) {
            // Ensure dots are placed relative to the field's position and size
            int adjustedX = Math.min(dot.x, fieldWidth - 5); // Prevent dots from being outside the field
            int adjustedY = Math.min(dot.y, fieldHeight - 5); // Prevent dots from being outside the field
            g.fillOval(adjustedX - 5, adjustedY - 5, 10, 10); // Draw dot with a diameter of 10 pixels
        }
    }
}


public class MediaEditorGUI {

    private static FootballFieldPanel footballFieldPanel;
    public static void main(String[] args) {
        com.formdev.flatlaf.FlatLightLaf.setup();
        SwingUtilities.invokeLater(MediaEditorGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // Create the main window
        JFrame frame = new JFrame("Main View");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        // Create the top panel with buttons for File and Help
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton fileButton = new JButton("File");
        JButton helpButton = new JButton("Help");
        topPanel.add(fileButton);
        topPanel.add(helpButton);

        // Add action listeners to buttons
        fileButton.addActionListener(e -> showFileOptions(frame));
        helpButton.addActionListener(e -> showHelpOptions(frame));

        // Add top panel to the main frame
        frame.add(topPanel, BorderLayout.NORTH);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton effectButton = new JButton("Effect view");
        rightPanel.add(effectButton);
        rightPanel.setBackground(Color.LIGHT_GRAY);
        rightPanel.setPreferredSize(new Dimension(240, 400));

        frame.add(rightPanel, BorderLayout.EAST);

        footballFieldPanel = new FootballFieldPanel();
        frame.add(footballFieldPanel, BorderLayout.CENTER);

        // Display the window
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showFileOptions(Frame parent) {
        // file button
        JPanel panel = new JPanel(new GridLayout(1, 4)); // 1 row, 4 cols
        String[] labels = {"Import Pyware Object", "Open Emerick Object", "Save Emerick Project", "Export Emerick Packets File"};
        for (String label : labels) {
            JButton button = new JButton(label);
            panel.add(button);
            button.addActionListener((ActionEvent e) -> {
                JOptionPane.showMessageDialog(parent, "You clicked: " + label);
            });
        }
        displayOptionsPanel(parent, panel, "File Options");
    }

    private static void showHelpOptions(Frame parent) {
        // help button
        JPanel panel = new JPanel(new GridLayout(1, 2));
        String[] labels = {"View document(open Github Wiki Page)", "Submit an Issue(open Github Issues page)"};
        for (String label : labels) {
            JButton button = new JButton(label);
            panel.add(button);
            button.addActionListener((ActionEvent e) -> {
                JOptionPane.showMessageDialog(parent, "You clicked: " + label);
            });
        }
        displayOptionsPanel(parent, panel, "Help Options");
    }

    private static void displayOptionsPanel(Frame parent, JPanel panel, String title) {
        // Show the panel in a dialog
        JDialog dialog = new JDialog(parent, title, true);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public static void addDotToField(int x, int y) {
        footballFieldPanel.addDot(x, y);
    }
}