package org.emrick.project;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.emrick.project.audio.AudioPlayer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;
import com.formdev.flatlaf.FlatLightLaf;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import org.emrick.project.serde.ColorAdapter;
import org.emrick.project.serde.Point2DAdapter;
import org.emrick.project.serde.ProjectFile;

public class MediaEditorGUI implements ImportListener, ScrubBarListener {

    // String definitions
    public static final String FILE_MENU_NEW_PROJECT = "New Project";
    public static final String FILE_MENU_OPEN_PROJECT = "Open Project";
    public static final String FILE_MENU_SAVE = "Save Project";

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
    private JLabel sysMsg = new JLabel("Welcome to Emrick Designer!", SwingConstants.RIGHT);
    private Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });

    // JSON serde
    private Gson gson;

    // Project info
    private File archivePath = null;
    private File drillPath = null;
    private Path userHome = Paths.get(System.getProperty("user.home"), ".emrick");

    public static void main(String[] args) {
        // setup sysmsg

        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize LaF" );
        }

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
        // serde setup
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Color.class, new ColorAdapter());
        builder.registerTypeAdapter(Point2D.class, new Point2DAdapter());
        builder.serializeNulls();
        gson = builder.create();

        // Autosave and system message things
        clearSysMsg.setRepeats(false);
        clearSysMsg.start();

        // test autosave stuff
        Timer t = new Timer(60 * 1000, e -> {
            System.out.println("autosaving...");
            writeSysMsg("Autosaving...");

            autosaveProject();

            writeSysMsg("Autosaved.");
        });
        t.setRepeats(true);
        t.start();

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
    public void onFileSelect(File archivePath, File drillPath) {
        this.archivePath = archivePath;
        this.drillPath = drillPath;
    }

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
        rebuildPageTabCounts();
    }

    private void rebuildPageTabCounts() {
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
        applyButton.addActionListener(e -> {
            Color newColor = chosenColor;
            for (Performer performer : footballFieldPanel.selectedPerformers.values()) {
                performer.getCoordinateFromSet(footballFieldPanel.getCurrentSet().label).setColor(newColor);
            }
            footballFieldPanel.repaint();
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
        importItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        fileMenu.add(importItem);
//        importItem.addActionListener(this);
        importItem.addActionListener(e -> {
            System.out.println("New Project...");

            // Important: ImportListener allows import services (e.g., SelectFileGUI > ImportArchive) to call update
            //  methods belonging to the current class (MediaEditorGUI).

            SelectFileGUI selectFileGUI = new SelectFileGUI(this);
            selectFileGUI.show();

            System.out.println("Should have loaded the field by now");
        });

        // TODO: make sfg not local, have it load the project after import finishes

        // Open Emrick Project
        // https://www.codejava.net/java-se/swing/add-file-filter-for-jfilechooser-dialog
        JMenuItem openItem = new JMenuItem(FILE_MENU_OPEN_PROJECT);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        fileMenu.add(openItem);
        openItem.addActionListener(e -> {
            openProjectDialog();
        });

        fileMenu.addSeparator();

        // Save Emrick Project
        JMenuItem saveItem = new JMenuItem(FILE_MENU_SAVE);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        fileMenu.add(saveItem);
        saveItem.addActionListener(e -> {
            saveProjectDialog();
        });

        fileMenu.addSeparator();

        // Export Emerick Packets
        JMenuItem exportItem = new JMenuItem("Export Emerick Packets File");
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
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

        JMenuItem displayTestDrill = new JMenuItem("Load Test Drill Object");
        fileMenu.add(displayTestDrill);
        displayTestDrill.addActionListener(e -> loadTestDrillObj());

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        JMenuItem resetColorsItem = new JMenuItem("Reset all effects");
        editMenu.add(resetColorsItem);
        resetColorsItem.addActionListener(e -> {
            if (archivePath == null || drillPath == null) {
                System.out.println("no project loaded");
                return;
            }

            Drill drill = footballFieldPanel.drill;

            for (int i = 0; i < drill.coordinates.size(); i++) {
                Coordinate c = drill.coordinates.get(i);
                c.setColor(new Color(0, 0, 0));
            }

            for (int i = 0; i < drill.performers.size(); i++) {
                Performer p = drill.performers.get(i);
                p.setColor(new Color(0, 0,0));
                for (int j = 0; j < p.getCoordinates().size(); j++) {
                    Coordinate c = p.getCoordinates().get(j);
                    c.setColor(new Color(0, 0, 0));
                }
            }

            footballFieldPanel.drill = drill;
            footballFieldPanel.repaint();
        });

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

        // handle closing the window
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (archivePath != null && drillPath != null) {
                    int resp = JOptionPane.showConfirmDialog(frame, "Would you like to save before quitting?", "Save and Quit?", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (resp == JOptionPane.CANCEL_OPTION) {
                        System.out.println("User cancelled exit.");
                        return;
                    } else if (resp == JOptionPane.YES_OPTION) {
                        System.out.println("User saving and quitting.");
                        saveProjectDialog();
                    } else if (resp == JOptionPane.NO_OPTION) {
                        System.out.println("User not saving but quitting anyway.");
                    }
                }
                frame.dispose();
                super.windowClosing(e);
            }
        });

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

    public void loadTestDrillObj(){
        clearDotsFromField();
        String filePath = "./src/test/java/org/emrick/project/testDrillParsed.txt";
        try {
            String DrillString = Files.lines(Paths.get(filePath))
                    .collect(Collectors.joining(System.lineSeparator()));
            DrillParser parse1 = new DrillParser();
            Drill drilltest = parse1.parseWholeDrill(DrillString);
            footballFieldPanel.drill = drilltest;
            footballFieldPanel.addSetToField(drilltest.sets.get(0));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ChangeColor(List<Coordinate> dots, List<String> selectIds,Color newColor){
        Effect effect = new Effect();
        effect.changeSelectedDotsColor(dots, newColor, footballFieldPanel);
        footballFieldPanel.repaint();
    }

    private void openProjectDialog() {
        System.out.println("Opening project...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Project");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Emrick Project Files (emrick, json)", "emrick", "json"));

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Opening file `"+fileChooser.getSelectedFile().getAbsolutePath()+"`.");
            loadProject(fileChooser.getSelectedFile());
        }
    }

    private void saveProjectDialog() {
        if (archivePath == null || drillPath == null) {
            System.out.println("Nothing to save.");
            writeSysMsg("Nothing to save!");
            return;
        }

        System.out.println("Saving project...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Project");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Emrick Project Files (emrick, json)", "emrick", "json"));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Saving file `"+fileChooser.getSelectedFile().getAbsolutePath()+"`.");
            saveProject(fileChooser.getSelectedFile(), archivePath, drillPath);
        }
    }

    private void autosaveProject() {
        // we don't have a project open, nothing to save
        if (archivePath == null || drillPath == null) {
            return;
        }

        long time = System.currentTimeMillis() / 1000L;
        Path dir = Paths.get(userHome.toString(), String.valueOf(time));
        Path jsonDir = Paths.get(dir.toString(), "backup.json");
        Path archiveDir = Paths.get(dir.toString(), archivePath.getName());
        Path drillDir = Paths.get(dir.toString(), drillPath.getName());
        File backupDir = new File(dir.toUri());
        if (!backupDir.mkdirs()) {
            // TODO: handle error from the backup failing
            return;
        }

        try {
            Files.copy(archivePath.toPath(), archiveDir);
            Files.copy(drillPath.toPath(), drillDir);
        } catch (IOException e) {
            // TODO: handle error from the backup failing
            System.out.println(e.getMessage());
            return;
        }

        saveProject(jsonDir.toFile(), archiveDir.toFile(), drillDir.toFile());
        writeSysMsg("Autosaved project to `" + jsonDir + "`.");
    }

    public void saveProject(File path, File archivePath, File drillPath) {
        String relArchive = path.getParentFile().toURI().relativize(archivePath.toURI()).getPath();
        String relDrill = path.getParentFile().toURI().relativize(drillPath.toURI()).getPath();

        ProjectFile pf = new ProjectFile(footballFieldPanel.drill, relArchive, relDrill);
        String g = gson.toJson(pf);

        System.out.println("saving to `" + path + "`");
//        System.out.println(g);

        try {
            FileWriter w = new FileWriter(path);
            w.write(g);
            w.close();
        } catch (IOException e) {
            writeSysMsg("Failed to save to `" + path + "`.");
            throw new RuntimeException(e);
        }

        writeSysMsg("Saved project to `" + path + "`.");
    }

    public void loadProject(File path) {
        try {
            // TODO: pdf loading is redundant with project file. fix? - LHD
            FileReader r = new FileReader(path);
            ProjectFile pf = gson.fromJson(r, ProjectFile.class);
            ImportArchive ia = new ImportArchive(this);
            Path fullArchive = Paths.get(path.getParentFile().getPath(), pf.archivePath);
            Path fullDrill = Paths.get(path.getParentFile().getPath(), pf.drillPath);

            archivePath = fullArchive.toFile();
            drillPath = fullDrill.toFile();

            ia.fullImport(fullArchive.toString(), fullDrill.toString());
            footballFieldPanel.drill = pf.drill;
            footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(0));
//            rebuildPageTabCounts();
//            scrubBarGUI.setReady(true);
            footballFieldPanel.repaint();
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            writeSysMsg("Failed to open to `" + path + "`.");
            throw new RuntimeException(e);
        }
    }

    private void writeSysMsg(String msg) {
        clearSysMsg.stop();
        sysMsg.setText(msg);
        clearSysMsg.start();
    }
}
