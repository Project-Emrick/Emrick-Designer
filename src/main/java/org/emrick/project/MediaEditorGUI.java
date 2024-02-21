package org.emrick.project;

import org.emrick.project.audio.AudioPlayer;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;
import com.formdev.flatlaf.FlatLightLaf;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.stream.Collectors;
import java.awt.geom.Point2D;



class FootballFieldPanel extends JPanel {
    public Drill drill;
    public HashMap<String,Performer> selectedPerformers;
    private double fieldWidth = 720; // Width of the football field
    private double fieldHeight = 360;
    private Point frontSideline50 = new Point(360,360);

    private Color colorChosen;
    private final int margin = 15;

    // Loading field decor.
    private BufferedImage surfaceImage;
    private BufferedImage floorCoverImage;
    private boolean ctrlHeld = false;
    private Set currentSet;

    public FootballFieldPanel() {
//        setPreferredSize(new Dimension(fieldWidth + 2*margin, fieldHeight + 2*margin)); // Set preferred size for the drawing area
        setMinimumSize(new Dimension(1042, 548));
        drill = new Drill();
        selectedPerformers = new HashMap<>();
        this.addMouseListener(new MouseInput());
        colorChosen = Color.RED;
    }

    public FootballFieldPanel(Color colorChosen) {
        this.colorChosen = colorChosen;
        setMinimumSize(new Dimension(1042, 548));
        drill = new Drill();
        selectedPerformers = new HashMap<>();
        this.addMouseListener(new MouseInput());
    }

    public void addSetToField(Set set) {
        currentSet = set;
        if (!set.equals("0")) {
            for (Performer p : drill.performers) {
                for (Coordinate c : p.getCoordinates()) {
                    if (c.set.equals(set)) {
                        p.currentLocation = dotToPoint(c.x, c.y);
                        break;
                    }
                }
            }
        } else {
            for (Performer p : drill.performers) {
                p.currentLocation = new Point2D.Double(-20,-20);
            }
        }
        repaint();
    }

    public Point2D dotToPoint(double x, double y) {
        double newY = frontSideline50.y - y/84 * fieldHeight;
        double newX = frontSideline50.x + x/160 * fieldWidth;
        return new Point2D.Double(newX,newY);
    }

    public void clearDots() {
        for (Performer p : drill.performers) {
            p.currentLocation = new Point2D.Double(-20,-20);
        }
        repaint();
    }

    public double getFieldWidth() {
        return fieldWidth;
    }

    public double getFieldHeight() {
        return fieldHeight;
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
        // Adjust dot drawing to account for the margin
//        g.setColor(Color.RED);
//        for (Point dot : dotCoordinates) {
//            double adjustedX = Math.min(dot.x, fieldWidth + margin - 5); // Adjust for margin
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
        for (Performer p : drill.performers) {
            //double adjustedX = Math.min(dot.x, fieldWidth); // Adjust for margin
            //double adjustedY = Math.min(dot.y, fieldHeight); // Adjust for margin
            Coordinate c = p.getCoordinateFromSet(currentSet.label);
            p.currentLocation = dotToPoint(c.x,c.y);
            double x = p.currentLocation.getX();
            double y = p.currentLocation.getY();
            g.setColor(colorChosen);
            g.fillRect((int)x-6,(int)y-6,6,12);
            g.setColor(colorChosen);
            g.fillRect((int)x,(int)y-6,6,12);
            if (selectedPerformers.get(p.getSymbol()+p.getLabel()) != null) {
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.BLACK);
            }
            g.drawRect((int)x-7,(int)y-7,14,14);
            g.drawRect((int)x-6,(int)y-6,12,12);
            g.drawLine((int)x,(int)y-5,(int)x,(int)y+6);
        }
    }
     public void setColorChosen(Color color) {
         this.colorChosen = color;
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

        this.fieldWidth = (image.getWidth() * ratio);
        this.fieldHeight = (image.getHeight() * ratio);

        // Center the image
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;
        frontSideline50 = new Point(x + (int)fieldWidth / 2,y + (int)fieldHeight);

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

    private class MouseInput implements MouseInputListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            int mx = e.getX();
            int my = e.getY();
            if (e.isControlDown()) {
                for (Performer p : drill.performers) {
                    Coordinate c = p.getCoordinateFromSet(currentSet.label);
                    p.currentLocation = dotToPoint(c.x,c.y);
                    int px = (int)p.currentLocation.getX();
                    int py = (int)p.currentLocation.getY();
                    if (mx <= px+7 && my <= py+7 && mx >= px-7 && my >= py-7) {
                        selectedPerformers.put(p.getSymbol()+p.getLabel(), p);
                        break;
                    }
                }
            } else {
                for (Performer p : drill.performers) {
                    Coordinate c = p.getCoordinateFromSet(currentSet.label);
                    p.currentLocation = dotToPoint(c.x,c.y);
                    double px = p.currentLocation.getX();
                    double py = p.currentLocation.getY();
                    if (mx <= px+7 && my <= py+7 && mx >= px-7 && my >= py-7) {
                        selectedPerformers = new HashMap<>();
                        selectedPerformers.put(p.getSymbol()+p.getLabel(), p);
                        break;
                    }
                    selectedPerformers = new HashMap<>();
                }
            }
            repaint();
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {}
    }
}


public class MediaEditorGUI implements ActionListener, ImportListener, ScrubBarListener {

    // String definitions
    public static final String FILE_MENU_NEW_PROJECT = "New Project";
    public static final String FILE_MENU_OPEN_PROJECT = "Open Project";
    public static final String FILE_MENU_SAVE = "Save";

    // UI Components of MediaEditorGUI
    private JFrame frame;
    private JPanel mainContentPanel;
    private FootballFieldPanel footballFieldPanel;
    private JPanel scrubBarPanel; // Refers directly to panel of ScrubBarGUI. Reduces UI refreshing issues.
    private ScrubBarGUI scrubBarGUI; // Refers to ScrubBarGUI instance, with functionality

    // Audio Components
    //  May want to abstract this away into some DrillPlayer class in the future
    private AudioPlayer audioPlayer;

    private Effect effect;
    private Color chosenColor;
    private JPanel colorDisplayPanel;

    // dots
    private List<Coordinate> dotCoordinates = new ArrayList<>();
    static JLabel sysMsg = new JLabel("Welcome to Emrick Designer!", SwingConstants.RIGHT);
    static Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });


    public static void main(String[] args) {
        // setup sysmsg

        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize LaF" );
        }

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
        Font f = new Font("FlatLaf.style", Font.PLAIN, 16);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.font", f);
        UIManager.put("CheckBoxMenuItem.font", f);
        UIManager.put("RadioButtonMenuItem.font", f);

        // Field
        footballFieldPanel = new FootballFieldPanel();
        footballFieldPanel.setBackground(Color.lightGray); // temp. Visual indicator for unfilled space
        JScrollPane fieldScrollPane = new JScrollPane(footballFieldPanel);
        fieldScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fieldScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Scrub Bar
        scrubBarGUI = new ScrubBarGUI(this, footballFieldPanel);
    }


    // Importing Listeners
    //  If you are not familiar with this, please check out how to use listener interfaces in Java

    @Override
    public void onImport() {
        scrubBarGUI.setReady(true);
    }

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
        audioPlayer = new AudioPlayer(audioFile);
    }

    @Override
    public void onDrillImport(String drill) {
        String text = DrillParser.extractText(drill);
        footballFieldPanel.drill = DrillParser.parseWholeDrill(text);
        footballFieldPanel.addSetToField(footballFieldPanel.drill.sets.get(0));

        // TODO: Q: Any way to get the Page Tabs w/ their respective counts?
        Map<String, Integer> pageTabCounts = new HashMap<>();
        int startCount = 0;
        for (Set s : footballFieldPanel.drill.sets) {
            startCount += s.duration;
            pageTabCounts.put(s.label, startCount);
        }

        scrubBarGUI.updatePageTabCounts(pageTabCounts);
        buildScrubBarPanel();
    }


    // ScrubBar Listeners

    @Override
    public void onPlay() {
        if (audioPlayer != null && scrubBarGUI.getAudioCheckbox().isSelected()) {
            audioPlayer.playAudio();
        }
    }

    @Override
    public void onPause() {
        if (audioPlayer != null) {
            audioPlayer.pauseAudio();
        }
    }


    /**
     * Loads the ScrubBarGUI Panel if it has not been created, or refreshes it if it already exists.
     */
    private void buildScrubBarPanel() {

        // Remove the existing scrubBarPanel
        if (scrubBarPanel != null) {
            mainContentPanel.remove(scrubBarPanel);
        }

        scrubBarPanel = scrubBarGUI.getScrubBarPanel();
        scrubBarPanel.setBorder(BorderFactory.createTitledBorder("Scrub Bar"));
        scrubBarPanel.setPreferredSize(new Dimension(650, 120));

        mainContentPanel.add(scrubBarPanel, BorderLayout.SOUTH);

        // IMPORTANT
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
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
        mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BorderLayout());

        footballFieldPanel.setBorder(BorderFactory.createTitledBorder("Main View"));
        mainContentPanel.add(footballFieldPanel, BorderLayout.CENTER);

        // Scrub Bar Panel
        buildScrubBarPanel();

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

        // Initialize the color display panel with a default color or make it transparent initially
        colorDisplayPanel = new JPanel();
        colorDisplayPanel.setBackground(Color.LIGHT_GRAY); // Default color
        colorDisplayPanel.setPreferredSize(new Dimension(50, 40)); // Adjust size as needed
        JLabel colorLabel = new JLabel("Selected Color");
        colorDisplayPanel.add(colorLabel, BorderLayout.WEST);
        // Add the color display panel to the Effect View panel
        colorDisplayPanel.setLayout(new BoxLayout(colorDisplayPanel, BoxLayout.Y_AXIS));

        // Create the "Apply" button
        JButton applyButton = new JButton("Apply");
        // Inside the ActionListener of the apply button
        applyButton.addActionListener(e -> {
            footballFieldPanel.setColorChosen(chosenColor);
            footballFieldPanel.repaint(); // This will cause the panel to redraw with the new color
        });

        colorDisplayPanel.add(applyButton);
        effectViewPanel.add(colorDisplayPanel, BorderLayout.SOUTH);

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
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
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
        JMenuItem displayCircleDrill = new JMenuItem("Load Demo Drill Object");
        fileMenu.add(displayCircleDrill);
        displayCircleDrill.addActionListener(e -> loadDemoDrillObj());

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
    }

    private void showPredefinedEffects(Frame parent) {
        // Example in showPredefinedEffects method
        Color selectedColor = JColorChooser.showDialog(parent, "Choose a Color", chosenColor);
        if (selectedColor != null) {
            chosenColor = selectedColor;
            colorDisplayPanel.setBackground(chosenColor); // Update the color display panel
            colorDisplayPanel.repaint(); // Repaint to reflect changes
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
            colorDisplayPanel.setBackground(selectedColor); // Update the color display panel
            colorDisplayPanel.repaint(); // Repaint to reflect changes
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

    public void clearDotsFromField() {
        footballFieldPanel.clearDots();
    }

    public double getFieldHeight() {
        return footballFieldPanel.getFieldHeight();
    }

    public double getFieldWidth() {
        return footballFieldPanel.getFieldWidth();
    }



    public void loadDemoDrillObj(){
        clearDotsFromField();
        String filePath = "./src/test/java/org/emrick/project/ExpectedPDFOutput.txt";
        try {
            String DrillString = Files.lines(Paths.get(filePath))
                    .collect(Collectors.joining(System.lineSeparator()));
            //System.out.println("Got drill string");
            //System.out.println(DrillString);
            DrillParser parse1 = new DrillParser();
            Drill drillby = parse1.parseWholeDrill(DrillString);
            footballFieldPanel.drill = drillby;
            footballFieldPanel.addSetToField(drillby.sets.get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ChangeColor(List<Coordinate> dots, List<String> selectIds,Color newColor){
        Effect effect = new Effect();
        effect.changeSelectedDotsColor(dots, newColor, footballFieldPanel);
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

            System.out.println("Should have loaded the field by now");
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