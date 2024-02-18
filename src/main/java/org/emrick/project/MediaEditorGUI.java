package org.emrick.project;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;


class FootballFieldPanel extends JPanel {
    private final List<Point> dotCoordinates = new ArrayList<>();
    private final int fieldWidth = 720; // Width of the football field
    private final int fieldHeight = 360;
    private final int margin = 15;

    // Loading field decor.
    private BufferedImage surfaceImage;
    private BufferedImage floorCoverImage;

    public FootballFieldPanel() {
//        setPreferredSize(new Dimension(fieldWidth + 2*margin, fieldHeight + 2*margin)); // Set preferred size for the drawing area
        setMinimumSize(new Dimension(1042, 548));
    }

    public void addDot(int x, int y) {
        dotCoordinates.add(new Point(x + margin, y + margin));
        repaint(); // Repaint the panel to show the new dot
    }

    public void clearDots() {
        dotCoordinates.clear();
        repaint();
    }

//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//
//        // Draw the football field background
//        g.setColor(new Color(92,255,103));
//        g.fillRect(margin, margin, fieldWidth, fieldHeight); // Use margin for x and y start
//
//        // Adjust line and shape drawing to account for the margin
//        g.setColor(Color.WHITE);
//        g.drawRect(margin, margin, fieldWidth, fieldHeight);
//        g.drawLine(fieldWidth / 2 + margin, margin, fieldWidth / 2 + margin, fieldHeight + margin);
//        g.drawOval((fieldWidth / 2 - fieldHeight / 10) + margin, (fieldHeight / 2 - fieldHeight / 10) + margin, fieldHeight / 5, fieldHeight / 5);
//        g.drawRect(margin, (fieldHeight / 2 - fieldHeight / 4) + margin, fieldWidth / 10, fieldHeight / 2);
//        g.drawRect(fieldWidth - (fieldWidth / 10) + margin, (fieldHeight / 2 - fieldHeight / 4) + margin, fieldWidth / 10, fieldHeight / 2);
//
//        // Adjust dot drawing to account for the margin
//        g.setColor(Color.RED);
//        for (Point dot : dotCoordinates) {
//            int adjustedX = Math.min(dot.x, fieldWidth + margin - 5); // Adjust for margin
//            int adjustedY = Math.min(dot.y, fieldHeight + margin - 5); // Adjust for margin
//            g.fillOval(adjustedX - 5, adjustedY - 5, 10, 10);
//        }
//    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the surface image
        if (surfaceImage != null) {
//            g.drawImage(surfaceImage, 0, 0, this.getWidth(), this.getHeight(), this);
            drawBetterImage(g, surfaceImage);
        }

        // Draw the floorCover image on top
        if (floorCoverImage != null) {
            // Adjust the x, y, width, and height as needed
//            g.drawImage(floorCoverImage, 0, 0, this.getWidth(), this.getHeight(), this);
            drawBetterImage(g, floorCoverImage);
        }

        // (Carried Over) Adjust dot drawing to account for the margin
        g.setColor(Color.RED);
        for (Point dot : dotCoordinates) {
            int adjustedX = Math.min(dot.x, fieldWidth + margin - 5); // Adjust for margin
            int adjustedY = Math.min(dot.y, fieldHeight + margin - 5); // Adjust for margin
            g.fillOval(adjustedX - 5, adjustedY - 5, 10, 10);
        }
    }

    // Draw image while maintaining aspect ratio (don't let field stretch/compress)
    private void drawBetterImage(Graphics g, BufferedImage image) {
        assert image != null;

        // Calculate the best width and height to maintain aspect ratio
        double widthRatio = (double) getWidth() / image.getWidth();
        double heightRatio = (double) getHeight() / image.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int width = (int) (image.getWidth() * ratio);
        int height = (int) (image.getHeight() * ratio);

        // Center the image
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;

        g.drawImage(image, x, y, width, height, this);
    }

    public void setFloorCoverImage(Image floorCoverImage) {
        this.floorCoverImage = (BufferedImage) floorCoverImage;
    }

    public void setSurfaceImage(Image surfaceImage) {
        this.surfaceImage = (BufferedImage) surfaceImage;
    }

    public Image getFloorCoverImage() {
        return floorCoverImage;
    }

    public Image getSurfaceImage() {
        return surfaceImage;
    }
}


public class MediaEditorGUI implements ActionListener, ImportListener {

    // String definitions
    public static final String FILE_MENU_NEW_PROJECT = "New Project";
    public static final String FILE_MENU_OPEN_PROJECT = "Open Project";
    public static final String FILE_MENU_SAVE = "Save";

    // UI Components of MediaEditorGUI
    private JFrame frame;
    private FootballFieldPanel footballFieldPanel;
    private ScrubBarGUI scrubBarGUI;

    // Audio Components
    private AudioPlayer audioPlayer;

    private Effect effect;
    private Color chosenColor;

    static JLabel sysMsg = new JLabel("Welcome to Emrick Designer!", SwingConstants.RIGHT);
    static Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });


    // MAIN - LAUNCHER
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

        // Run this program on the Event Dispatch Thread (EDT)
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MediaEditorGUI mediaEditorGUI = new MediaEditorGUI();
                mediaEditorGUI.createAndShowGUI();
            }
        });
    }

    public MediaEditorGUI() {

        // Change Font Size for Menu and MenuIem
        Font f = new Font("sans-serif", Font.PLAIN, 18);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.font", f);
        UIManager.put("CheckBoxMenuItem.font", f);
        UIManager.put("RadioButtonMenuItem.font", f);

        // Field
        footballFieldPanel = new FootballFieldPanel();
        footballFieldPanel.setBackground(Color.red); // temp. Visual indicator for unfilled space
        JScrollPane fieldScrollPane = new JScrollPane(footballFieldPanel);
        fieldScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fieldScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Scrub Bar
        // Temporary - Begin
        Map<String, Integer> dummyData1 = new HashMap<>();
        dummyData1.put("1", 0); // Page tab 1 maps to count 0
        dummyData1.put("1A", 16); // Page tab 1A maps to count 16
        dummyData1.put("2", 32); // Page tab 2 maps to count 32
        dummyData1.put("2A", 48); // etc.
        dummyData1.put("3", 64);
        dummyData1.put("3A", 88);
        dummyData1.put("4", 96);
        dummyData1.put("4A", 112);
        dummyData1.put("4B", 128);
        // Temporary - End

        scrubBarGUI = new ScrubBarGUI(dummyData1);
    }


    // Importing - Callbacks to Update UI

    @Override
    public void onFloorCoverImport(Image image) {
        footballFieldPanel.setFloorCoverImage(image);
        footballFieldPanel.repaint();
    }

    @Override
    public void onSurfaceImport(Image image) {
        footballFieldPanel.setSurfaceImage(image);
        footballFieldPanel.repaint();
    }

    @Override
    public void onAudioImport(File audioFile) {

        // Play or pause audio through the AudioPlayer service class
//        audioPlayer = new AudioPlayer(audioFile, true);
    }


    private void createAndShowGUI() {
        //main window
        frame = new JFrame("Emrick Designer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);

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

        footballFieldPanel.setBorder(BorderFactory.createTitledBorder("Main View"));
//        footballFieldPanel.setPreferredSize(new Dimension(650, 500));
        mainContentPanel.add(footballFieldPanel, BorderLayout.CENTER);

        // Scrub Bar panel
        // JPanel scrubBarPanel = new JPanel();

        JPanel scrubBarPanel = scrubBarGUI.getScrubBarPanel();
        scrubBarPanel.setBorder(BorderFactory.createTitledBorder("Scrub Bar"));
        scrubBarPanel.setPreferredSize(new Dimension(650, 120));
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
        effectViewPanel.setPreferredSize(new Dimension(300, frame.getHeight()));
        effectViewPanel.setBorder(BorderFactory.createTitledBorder("Effect View"));
        frame.add(effectViewPanel, BorderLayout.EAST);


        /*
            Menus
         */

        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        // Import Pyware Project
        JMenuItem importItem = new JMenuItem(FILE_MENU_NEW_PROJECT);
        fileMenu.add(importItem);
        importItem.addActionListener(this);

        // TODO: make sfg not local, have it load the project after import finishes

        // Open Emrick Project
        // https://www.codejava.net/java-se/swing/add-file-filter-for-jfilechooser-dialog
        JMenuItem openItem = new JMenuItem(FILE_MENU_OPEN_PROJECT);
        fileMenu.add(openItem);
        openItem.addActionListener(e -> {
            System.out.println("Opening project...");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Open Project");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Emrick Project Files (emrick, json)", "emrick", "json"));
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                System.out.println("Opening file `"+fileChooser.getSelectedFile().getAbsolutePath()+"`.");
            }
        });

        fileMenu.addSeparator();

        // Save Emrick Project
        JMenuItem saveItem = new JMenuItem(FILE_MENU_SAVE);
        fileMenu.add(saveItem);
        saveItem.addActionListener(e -> {
            System.out.println("Saving project...");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Project");
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                System.out.println("Saving file `"+fileChooser.getSelectedFile().getAbsolutePath()+"`.");
            }
        });

        fileMenu.addSeparator();

        // Export Emerick Packets
        JMenuItem exportItem = new JMenuItem("Export Emerick Packets File");
        fileMenu.add(exportItem);
        exportItem.addActionListener(e -> {
            System.out.println("Exporting packets...");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Project");
            if (fileChooser.showSaveDialog(fileMenu) == JFileChooser.APPROVE_OPTION) {
                System.out.println("Exporting file `"+fileChooser.getSelectedFile().getAbsolutePath()+"`.");
            }
        });

        fileMenu.addSeparator();

        // Demos
        JMenuItem displayCircleDrill = new JMenuItem("Display Circle Drill");
        fileMenu.add(displayCircleDrill);
        displayCircleDrill.addActionListener(e -> addLotsaDots());
        JMenuItem displayStarDrill = new JMenuItem("Display Star Drill");
        fileMenu.add(displayStarDrill);
        displayStarDrill.addActionListener(e -> addStarDemo(mainContentPanel));

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        JMenuItem viewDocItem = new JMenuItem("View document (open Github Wiki Page)");
        helpMenu.add(viewDocItem);
        viewDocItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: View document"));

        JMenuItem submitIssueItem = new JMenuItem("Submit an Issue (open Github Issues page)");
        helpMenu.add(submitIssueItem);
        submitIssueItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: Submit an Issue"));


        // System message
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(sysMsg);


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
        frame.setTitle("Emrick Designer");

        // Need a list of Coordinate objects
        // Need a list of IDs as strings

        List<Coordinate> dots = new ArrayList<>();
        List<String> selectedIds = Arrays.asList("id1", "id2", "id3");

        ChangeColor(dots, selectedIds, chosenColor);
    }

    private void showPredefinedEffects(Frame parent) {
        // Open a JColorChooser dialog to let the user pick a color
        Color selectedColor = JColorChooser.showDialog(parent, "Choose a Color", chosenColor);
        if (selectedColor != null) {
            chosenColor = selectedColor; // Store the chosen color
            // For demonstration, let's print the selected RGB values
            System.out.println("The selected color is: " + chosenColor.toString());

        }
    }

    private void chooseRGB(Frame parent) {
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

    private int parseColorValue(String value) {
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

    public void addDotToField(int x, int y) {
        footballFieldPanel.addDot(x, y);
    }

    public void clearDotsFromField() {
        footballFieldPanel.clearDots();
    }

    public void addLotsaDots(){
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

    public void addStarDemo(JPanel mainContentPanel){
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

    public void ChangeColor(List<Coordinate> dots, List<String> selectIds,Color newColor){
        Effect effect = new Effect();
        effect.changeSelectedDotsColor(dots, selectIds, newColor);
        footballFieldPanel.repaint();
    }


    // Actions

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem) {
            menuActionPerformed(e);
        }
    }

    private void menuActionPerformed(ActionEvent e) {
        JMenuItem item = (JMenuItem) e.getSource();
        String text = item.getText();

        // New Project
        if (text.equals(FILE_MENU_NEW_PROJECT)) {
            System.out.println("New Project.");

            // Important: ImportListener allows import services (e.g., SelectFileGUI > ImportArchive) to call update
            //  methods belonging to the current class (MediaEditorGUI).

            SelectFileGUI selectFileGUI = new SelectFileGUI(this);
            selectFileGUI.show();
        }

        // Open Project
        else if (text.equals(FILE_MENU_OPEN_PROJECT)) {
            System.out.println("Open Project.");
        }

        // Save
        else if (text.equals(FILE_MENU_SAVE)) {
            System.out.println("Save.");

            // TODO: Saving
        }
    }
}