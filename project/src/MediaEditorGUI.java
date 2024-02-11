import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MediaEditorGUI {

    static JLabel sysMsg = new JLabel("");
    static Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });

    public static void main(String[] args) {
        // setup sysmsg
        clearSysMsg.setRepeats(false);

        // test autosave stuff
        Timer t = new Timer(1 * 60 * 60 * 1000, e -> {
            System.out.println("autosaving...");
            clearSysMsg.stop();
            sysMsg.setText("Autosaving...");
            clearSysMsg.start();

            // TODO: actual saving here
        });
        t.setRepeats(true);
        t.start();

        // set up main view
        //  Swing is not thread-safe: execute on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(MediaEditorGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        //main window
        JFrame frame = new JFrame("Main View");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        // Top panel, File and Help buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton fileButton = new JButton("File");
        JButton helpButton = new JButton("Help");
        topPanel.add(fileButton);
        topPanel.add(helpButton);
        fileButton.addActionListener(e -> showFileOptions(frame));
        helpButton.addActionListener(e -> showHelpOptions(frame));
        frame.add(topPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BorderLayout());

        // Main View panel
        JPanel mainViewPanel = new JPanel();
        mainViewPanel.setBorder(BorderFactory.createTitledBorder("Main View"));
        mainViewPanel.setPreferredSize(new Dimension(650, 500));
        mainContentPanel.add(mainViewPanel, BorderLayout.CENTER);

        // Scrub Bar panel
        JPanel scrubBarPanel = new JPanel();
        scrubBarPanel.setBorder(BorderFactory.createTitledBorder("Scrub Bar"));
        scrubBarPanel.setPreferredSize(new Dimension(650, 100));
        mainContentPanel.add(scrubBarPanel, BorderLayout.SOUTH);

        frame.add(mainContentPanel, BorderLayout.CENTER);

        // Timeline panel
        JPanel timelinePanel = new JPanel();
        timelinePanel.setBorder(BorderFactory.createTitledBorder("Timeline"));
        timelinePanel.setPreferredSize(new Dimension(frame.getWidth(), 100));
        frame.add(timelinePanel, BorderLayout.SOUTH);

        // Effect View panel
        JPanel effectViewPanel = new JPanel();
        effectViewPanel.setLayout(new BorderLayout());
        JButton lightButton = new JButton("Light");
        lightButton.addActionListener(e -> showLightOptions(frame));
        effectViewPanel.add(lightButton, BorderLayout.NORTH);
        effectViewPanel.setPreferredSize(new Dimension(350, frame.getHeight()));
        effectViewPanel.setBorder(BorderFactory.createTitledBorder("Effect View"));
        frame.add(effectViewPanel, BorderLayout.EAST);


        //logan's. Direction needs to be changed
//        frame.add(sysMsg, BorderLayout.SOUTH);
        // Display the window
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    // Button
    //
    //
    //
    //

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

    private static void showLightOptions(Frame parent) {
        // Create a panel with a layout to hold the buttons
        JPanel lightOptionsPanel = new JPanel();
        lightOptionsPanel.setLayout(new BoxLayout(lightOptionsPanel, BoxLayout.Y_AXIS));

        // Create the "Predefined Light Effect" button and add it to the panel
        JButton predefinedButton = new JButton("Predefined Light Effect");
        predefinedButton.addActionListener(e -> showPredefinedEffects(lightOptionsPanel));
        lightOptionsPanel.add(predefinedButton);

        // Create the "Choose Light Effect by RGB Values" button and add it to the panel
        JButton rgbButton = new JButton("Choose Light Effect by RGB Values");
        lightOptionsPanel.add(rgbButton);

        // Display the light options panel in a dialog
        displayOptionsPanel(parent, lightOptionsPanel, "Light Options");

    }
    private static void showPredefinedEffects(JPanel lightOptionsPanel) {
        // Remove existing components if they were added before
        lightOptionsPanel.removeAll();

        // Create buttons labeled 1, 2, 3, 4, 5 without a loop and set their colors
        JButton button1 = createColoredButton("1", Color.BLUE, Color.WHITE);
        JButton button2 = createColoredButton("2", Color.RED, Color.WHITE);
        JButton button3 = createColoredButton("3", Color.GREEN, Color.WHITE);
        JButton button4 = createColoredButton("4", Color.ORANGE, Color.WHITE);
        JButton button5 = createColoredButton("5", Color.YELLOW, Color.BLACK);

        // Add new buttons to the panel
        lightOptionsPanel.add(button1);
        lightOptionsPanel.add(button2);
        lightOptionsPanel.add(button3);
        lightOptionsPanel.add(button4);
        lightOptionsPanel.add(button5);

        // Refresh the panel to show the new buttons
        lightOptionsPanel.revalidate();
        lightOptionsPanel.repaint();
    }

    private static JButton createColoredButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }


    private static void displayOptionsPanel(Frame parent, JPanel panel, String title) {
        // Show the panel in a dialog
        panel.setPreferredSize(new Dimension(300,200));
        JDialog dialog = new JDialog(parent, title, true);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // rectangle UI
    //
    //
    //
    //
    // first rectangle
    static class CustomPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.BLACK);

            Font boldFont = new Font("Default", Font.BOLD,15);
            g.setFont(boldFont);
            // Draw a rectangle under the File and Help buttons
            // g.drawRect(10, 10, this.getWidth() - 380, 400); // 1st

            // String Scrub_Bar = "Scrub Bar";
            // g.drawRect(10, 430, this.getWidth() - 380, 150); // scrub bar rectangele
            // g.drawString(Scrub_Bar, 15, 445);

            // String Timeline = "Timeline";
            // g.drawRect(10,600, this.getWidth() - 20, 120); // timeline rectangle
            // g.drawString(Timeline, 15, 615);

            // String effectview = "Effect View";
            // g.drawRect(650,10,this.getWidth() - 660, 575); //effect view rectangle
            // g.drawString(effectview, 660, 30);

        }
    }
}