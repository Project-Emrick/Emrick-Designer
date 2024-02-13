import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.util.ArrayList;
import java.util.List;


class FootballFieldPanel extends JPanel {
    private final List<Point> dotCoordinates = new ArrayList<>();
    private final int fieldWidth = 720; // Width of the football field
    private final int fieldHeight = 360;
    private final int margin = 15;

    public FootballFieldPanel() {
        setPreferredSize(new Dimension(fieldWidth + 2*margin, fieldHeight + 2*margin)); // Set preferred size for the drawing area
    }

    public void addDot(int x, int y) {
        dotCoordinates.add(new Point(x + margin, y + margin));
        repaint(); // Repaint the panel to show the new dot
    }

    public void clearDots() {
        dotCoordinates.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the football field background
        g.setColor(new Color(92,255,103));
        g.fillRect(margin, margin, fieldWidth, fieldHeight); // Use margin for x and y start

        // Adjust line and shape drawing to account for the margin
        g.setColor(Color.WHITE);
        g.drawRect(margin, margin, fieldWidth, fieldHeight);
        g.drawLine(fieldWidth / 2 + margin, margin, fieldWidth / 2 + margin, fieldHeight + margin);
        g.drawOval((fieldWidth / 2 - fieldHeight / 10) + margin, (fieldHeight / 2 - fieldHeight / 10) + margin, fieldHeight / 5, fieldHeight / 5);
        g.drawRect(margin, (fieldHeight / 2 - fieldHeight / 4) + margin, fieldWidth / 10, fieldHeight / 2);
        g.drawRect(fieldWidth - (fieldWidth / 10) + margin, (fieldHeight / 2 - fieldHeight / 4) + margin, fieldWidth / 10, fieldHeight / 2);

        // Adjust dot drawing to account for the margin
        g.setColor(Color.RED);
        for (Point dot : dotCoordinates) {
            int adjustedX = Math.min(dot.x, fieldWidth + margin - 5); // Adjust for margin
            int adjustedY = Math.min(dot.y, fieldHeight + margin - 5); // Adjust for margin
            g.fillOval(adjustedX - 5, adjustedY - 5, 10, 10);
        }
    }
}


public class MediaEditorGUI {
    private static FootballFieldPanel footballFieldPanel;    
    
    static Color chosenColor;
    static JLabel sysMsg = new JLabel("Welcome to Emrick Designer!");
    static Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });

    public static void main(String[] args) {
        // setup sysmsg
        clearSysMsg.setRepeats(false);
        clearSysMsg.start();

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
        frame.setSize(1100, 800);


        JMenuBar menuBar = new JMenuBar();


        /*
            Panels
         */
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


        frame.add(topPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BorderLayout());

        // Main View panel
        // JPanel mainViewPanel = new JPanel();
        // mainViewPanel.setBorder(BorderFactory.createTitledBorder("Main View"));
        // mainViewPanel.setPreferredSize(new Dimension(650, 500));
        // mainContentPanel.add(mainViewPanel, BorderLayout.CENTER);

        footballFieldPanel = new FootballFieldPanel();
        footballFieldPanel.setBorder(BorderFactory.createTitledBorder("Main View"));
        footballFieldPanel.setPreferredSize(new Dimension(650, 500));
        mainContentPanel.add(footballFieldPanel, BorderLayout.CENTER);

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
        effectViewPanel.setPreferredSize(new Dimension(350, frame.getHeight()));
        effectViewPanel.setBorder(BorderFactory.createTitledBorder("Effect View"));
        frame.add(effectViewPanel, BorderLayout.EAST);


        /*
            Menus
         */
        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem importItem = new JMenuItem("Import Pyware Object");
        fileMenu.add(importItem);
        importItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: Import Pyware Object"));

        JMenuItem openItem = new JMenuItem("Open Emerick Object");
        fileMenu.add(openItem);
        openItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: Open Emerick Object"));

        JMenuItem displayCircleDrill = new JMenuItem("Display Circle Drill");
        fileMenu.add(displayCircleDrill);
        displayCircleDrill.addActionListener(e -> addLotsaDots());

        JMenuItem displayStarDrill = new JMenuItem("Display Star Drill");
        fileMenu.add(displayStarDrill);
        displayStarDrill.addActionListener(e -> addStarDemo(mainContentPanel));

        JMenuItem saveItem = new JMenuItem("Save Emerick Project");
        fileMenu.add(saveItem);
        saveItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: Save Emerick Project"));

        JMenuItem exportItem = new JMenuItem("Export Emerick Packets File");
        fileMenu.add(exportItem);
        exportItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: Export Emerick Packets File"));

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        JMenuItem viewDocItem = new JMenuItem("View document (open Github Wiki Page)");
        helpMenu.add(viewDocItem);
        viewDocItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: View document"));

        JMenuItem submitIssueItem = new JMenuItem("Submit an Issue (open Github Issues page)");
        helpMenu.add(submitIssueItem);
        submitIssueItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: Submit an Issue"));


        //Light menu. and adjust its menu location
        JPopupMenu lightMenuPopup = new JPopupMenu();
        JMenuItem predefinedLightItem = new JMenuItem("Predefined Light Effect");
        predefinedLightItem.addActionListener(e -> showPredefinedEffects(frame));
        lightMenuPopup.add(predefinedLightItem);

        JMenuItem chooseRGBItem = new JMenuItem("Choose Light Effect by RGB Values");
        lightMenuPopup.add(chooseRGBItem);

        chooseRGBItem.addActionListener(e -> chooseRGB(frame));

        // Button that triggers the popup menu
        JButton lightButton = new JButton("Light Options");
        lightButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int x = 0;
                int y = lightButton.getHeight();
                lightMenuPopup.show(lightButton, x, y);
            }
        });
        effectViewPanel.add(lightButton, BorderLayout.NORTH);


        // Display the window
        frame.setJMenuBar(menuBar);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }



    private static JButton createColoredButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }



//    private static void showPredefinedEffects(Frame parent) {
//        JDialog dialog = new JDialog(parent, "Predefined Effects", true);
//        dialog.setLayout(new FlowLayout());
//
//        JButton button1 = createColoredButton("Blue", Color.BLUE, Color.WHITE);
//        dialog.add(button1);
//
//        JButton button2 = createColoredButton("Red", Color.RED, Color.WHITE);
//        dialog.add(button2);
//
//        JButton button3 = createColoredButton("Green", Color.GREEN, Color.WHITE);
//        dialog.add(button3);
//
//        JButton button4 = createColoredButton("Orange", Color.ORANGE, Color.WHITE);
//        dialog.add(button4);
//
//        JButton button5 = createColoredButton("Yellow", Color.YELLOW, Color.BLACK);
//        dialog.add(button5);
//
//        dialog.pack();
//        dialog.setLocationRelativeTo(parent);
//        dialog.setVisible(true);
//    }
    private static void showPredefinedEffects(Frame parent) {
        // Open a JColorChooser dialog to let the user pick a color
        Color selectedColor = JColorChooser.showDialog(parent, "Choose a Color", chosenColor);
        if (selectedColor != null) {
            chosenColor = selectedColor; // Store the chosen color
            // For demonstration, let's print the selected RGB values
            System.out.println("The selected color is: " + chosenColor.toString());

        }
    }
    private static void chooseRGB(Frame parent) {
        JTextField fieldR = new JTextField(5);
        JTextField fieldG = new JTextField(5);
        JTextField fieldB = new JTextField(5);

        JPanel panel = new JPanel();
        panel.add(new JLabel("Red:"));
        panel.add(fieldR);
        panel.add(Box.createHorizontalStrut(15));
        panel.add(new JLabel("Green:"));
        panel.add(fieldG);
        panel.add(Box.createHorizontalStrut(15));
        panel.add(new JLabel("Blue:"));
        panel.add(fieldB);

        int result = JOptionPane.showConfirmDialog(parent, panel,
                "Enter RGB values", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int r = parseColorValue(fieldR.getText());
            int g = parseColorValue(fieldG.getText());
            int b = parseColorValue(fieldB.getText());

            if (r == -1 || g == -1 || b == -1) {
                JOptionPane.showMessageDialog(parent,
                        "Invalid input. Please enter values between 0 and 255.");
                return;
            }

            // Now you have r, g, b values, you can use them to set the color
            Color selectedColor = new Color(r, g, b);

        }
    }

    private static int parseColorValue(String value) {
        try {
            int intValue = Integer.parseInt(value.trim());
            if (intValue >= 0 && intValue <= 255) {
                return intValue;
            }
        } catch (NumberFormatException e) {
            // The input was not an integer
        }
        return -1; // Return -1 if the input was invalid
    }


    public static void addDotToField(int x, int y) {
        footballFieldPanel.addDot(x, y);
    }

    public static void clearDotsFromField() {
        footballFieldPanel.clearDots();
    }

    public static void addLotsaDots(){
        clearDotsFromField();
        addDotToField(370, 90);  // Top center
        addDotToField(420, 115); // Top-right
        addDotToField(450, 165); // Right upper-middle
        addDotToField(450, 215); // Right lower-middle
        addDotToField(420, 265); // Bottom-right
        addDotToField(370, 290); // Bottom center
        addDotToField(320, 265); // Bottom-left
        addDotToField(290, 215); // Left lower-middle
        addDotToField(290, 165); // Left upper-middle
        addDotToField(320, 115); // Top-left
    }

    public static void addStarDemo(JPanel mainContentPanel){
        clearDotsFromField();
        addDotToField(360, 180);
        addDotToField(380, 180);
        addDotToField(400, 180);

        addDotToField(400, 180);
        addDotToField(410, 170);
        addDotToField(420, 160);
        addDotToField(430, 150);

        addDotToField(400, 110);
        addDotToField(410, 120);
        addDotToField(420, 130);
        addDotToField(430, 140);

        addDotToField(340, 110);
        addDotToField(360, 110);
        addDotToField(380, 110);
        addDotToField(400, 110);

        addDotToField(360, 120);
        addDotToField(360, 140);
        addDotToField(360, 160);
        addDotToField(360, 200);
        addDotToField(360, 220);
        addDotToField(360, 240);
        addDotToField(360, 260);
    }
}