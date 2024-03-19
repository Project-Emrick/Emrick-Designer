package org.emrick.project;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.emrick.project.audio.AudioPlayer;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import javax.swing.Timer;
import com.formdev.flatlaf.FlatLightLaf;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import org.emrick.project.serde.ColorAdapter;
import org.emrick.project.serde.PairAdapter;
import org.emrick.project.serde.Point2DAdapter;
import org.emrick.project.serde.ProjectFile;

public class MediaEditorGUI implements ImportListener, ScrubBarListener, SyncListener {

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

    private Color chosenColor;
    private JPanel colorDisplayPanel;

    // dots
    private JLabel sysMsg = new JLabel("Welcome to Emrick Designer!", SwingConstants.RIGHT);
    private Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });

    // Time keeping
    // TODO: save this
    private ArrayList<SyncTimeGUI.Pair> timeSync = null;
    private boolean useStartDelay; // If we are at the first count of the first set, useStartDelay = true
    private int startDelay; // in seconds. Drills might not start immediately, therefore use this.
    private Timer playbackTimer = null;

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
//                mediaEditorGUI.createAndShowGUI();
            }
        });
    }


    public MediaEditorGUI() {
        // serde setup
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Color.class, new ColorAdapter());
        builder.registerTypeAdapter(Point2D.class, new Point2DAdapter());
        builder.registerTypeAdapter(SyncTimeGUI.Pair.class, new PairAdapter());
        builder.serializeNulls();
        gson = builder.create();

        // Autosave and system message things
        clearSysMsg.setRepeats(false);
        clearSysMsg.start();

        // test autosave stuff
        Timer t = new Timer(60 * 1000, e -> {
            // FIXME: Temporarily commented auto-save feature -- like a snail is slowing eating away my disk space

//            System.out.println("autosaving...");
//            writeSysMsg("Autosaving...");

//            autosaveProject();
//            writeSysMsg("Autosaved.");
        });
        t.setRepeats(true);
        t.start();

        // playback timer
        playbackTimer = new Timer(0, e -> {
            if (scrubBarGUI == null || playbackTimer == null) {
                // TODO: throw an error, we shouldn't be able to be here!
                return;
            }

            // Start delay
            if (useStartDelay) {
                useStartDelay = false; // prevent infinite delay

                // System.out.println("Attempting to delay drill start.");
                playbackTimer.stop();
                Timer delayTimer = new Timer(startDelay * 1000, e2 -> {
                    playbackTimer.start();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
                return;
            }

            scrubBarGUI.nextCount();

            if (scrubBarGUI.isAtLastSet() && scrubBarGUI.isAtEndOfSet()) {
                playbackTimer.stop();
                // TODO: stop music
                scrubBarGUI.setIsPlayingPlay();
                return;
            }
            else if (scrubBarGUI.isAtEndOfSet()) {
                scrubBarGUI.nextSet();
            }

            setPlaybackTimerTime();

            // TODO: repaint everything relevant (field, timer, etc)
        });

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

        // Main frame
        frame = new JFrame("Emrick Designer");

        // Scrub Bar
        scrubBarGUI = new ScrubBarGUI(frame, this, this, footballFieldPanel);

        // Scrub bar cursor starts on first count of drill by default
        useStartDelay = true;

        createAndShowGUI();
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
        Map<String, Integer> pageTabCounts = new HashMap<>();
        int startCount = 0;
        for (Set s : footballFieldPanel.drill.sets) {
            startCount += s.duration;
            pageTabCounts.put(s.label, startCount);
        }

        scrubBarGUI.updatePageTabCounts(pageTabCounts);
        buildScrubBarPanel();
    }

    @Override
    public void onSync(ArrayList<SyncTimeGUI.Pair> times, int startDelay) {
        // we're treating the integers as duration. this may not be a great idea for the future.
        timeSync = times;
        // System.out.println(times);
        System.out.println("got times");

        this.startDelay = startDelay;
    }


    // ScrubBar Listeners

    @Override
    public boolean onPlay() {
        if (timeSync == null) {
            JOptionPane.showMessageDialog(frame, "Cannot play without syncing time!", "Playback Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (audioPlayer != null && scrubBarGUI.getAudioCheckbox().isSelected()) {
            // TODO: get audio to correct position
            audioPlayer.playAudio();
        }

        setPlaybackTimerTime();
        playbackTimer.start();

        return true;
    }

    @Override
    public boolean onPause() {
        if (timeSync == null) {
            JOptionPane.showMessageDialog(frame, "Cannot play without syncing time!", "Playback Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (audioPlayer != null) {
            audioPlayer.pauseAudio();
        }

        playbackTimer.stop();

        return true;
    }

    @Override
    public void onScrub() {
        useStartDelay = scrubBarGUI.isAtFirstSet() && scrubBarGUI.isAtStartOfSet();
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

            SelectFileGUI selectFileGUI = new SelectFileGUI(frame, this);
            selectFileGUI.show();

//            System.out.println("Should have loaded the field by now");
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

        // Run menu
        JMenu runMenu = new JMenu("Run");
        menuBar.add(runMenu);
        JMenuItem runShowItem = new JMenuItem("Run Show Linked to Viewport");
        runMenu.add(runShowItem);
        JMenuItem flowViewerItem = new JMenuItem("Run Show via Flow View");
        runMenu.add(flowViewerItem);
        JMenuItem stopShowItem = new JMenuItem("Stop show");
        stopShowItem.addActionListener(e -> {
            footballFieldPanel.setSerialTransmitter(null);
            runMenu.remove(stopShowItem);
            if (!runMenu.isMenuComponent(runShowItem)) {
                runMenu.add(runShowItem);
            }
            if (!runMenu.isMenuComponent(flowViewerItem)) {
                runMenu.add(flowViewerItem);
            }
        });
        flowViewerItem.addActionListener(e -> {
            SerialTransmitter st = new SerialTransmitter();
            String port = st.getSerialPort().getDescriptivePortName();
            int option = 1;
            while (option > 0) {
                if (option == 1) {
                    option = JOptionPane.showConfirmDialog(null, "Is (" + port + ") the correct port for the transmitter?", "Run Show", JOptionPane.YES_NO_OPTION);
                } else if (option == 2) {
                    option = JOptionPane.showConfirmDialog(null, "Port invalid: Make sure you have the right port and that it is not already in use then try again.", "Run show: ERROR", JOptionPane.OK_CANCEL_OPTION);
                    if (option == 2) {
                        option = -1;
                        return;
                    } else if (option == 0) {
                        option = 1;
                    }
                }
                if (option == 1) {
                    port = JOptionPane.showInputDialog("Enter COM port (example: COM7): ");
                    if (port != null) {
                        if (!st.setSerialPort(port)) {
                            option = 2;
                        } else {
                            port = st.getSerialPort().getDescriptivePortName();
                        }
                    } else {
                        option = -1;
                        return;
                    }
                } else if (option == 0) {
                    runMenu.remove(flowViewerItem);
                    runMenu.add(stopShowItem);
                }
            }
            JFrame flowFrame = new JFrame("Emrick Designer - Flow Viewer");
            flowFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            flowFrame.setSize(1200,800);
            flowFrame.setVisible(true);
            // TODO - When we add RF triggers, swap the list of sets to a list of triggers
            String[][] sets = new String[footballFieldPanel.drill.sets.size()][5];
            for (int i = 0; i < footballFieldPanel.drill.sets.size(); i++) {
                sets[i][0] = footballFieldPanel.drill.sets.get(i).label;
                sets[i][1] = Integer.toString(footballFieldPanel.drill.sets.get(i).duration);
                sets[i][2] = "";
                sets[i][3] = "";
                sets[i][4] = "";
            }
            String[] labels = new String[5];
            labels[0] = "Set";
            labels[1] = "Counts";
            labels[2] = "Title";
            labels[3] = "Cue";
            labels[4] = "Description";
            JTable table = new JTable(sets, labels);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            ListSelectionModel lsm = table.getSelectionModel();
            lsm.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        st.writeSet(e.getLastIndex());
                        // without a connected transmitter, above line will fail
                        // for debug, comment out above line and uncomment line below
                        //System.out.println(e.getLastIndex());
                    }
                }
            });
            table.setRowSelectionAllowed(true);
            table.setFillsViewportHeight(true);
            JScrollPane jsp = new JScrollPane(table);
            flowFrame.add(jsp);
        });
        runShowItem.addActionListener(e -> {
            SerialTransmitter st = new SerialTransmitter();
            String port = st.getSerialPort().getDescriptivePortName();
            int option = 1;
            while (option > 0) {
                if (option == 1) {
                    option = JOptionPane.showConfirmDialog(null, "Is (" + port + ") the correct port for the transmitter?", "Run Show", JOptionPane.YES_NO_OPTION);
                } else if (option == 2) {
                    option = JOptionPane.showConfirmDialog(null, "Port invalid: Make sure you have the right port and that it is not already in use then try again.", "Run show: ERROR", JOptionPane.OK_CANCEL_OPTION);
                    if (option == 2) {
                        option = -1;
                    } else if (option == 0) {
                        option = 1;
                    }
                }
                if (option == 1) {
                    port = JOptionPane.showInputDialog("Enter COM port (example: COM7): ");
                    if (port != null) {
                        if (!st.setSerialPort(port)) {
                            option = 2;
                        } else {
                            port = st.getSerialPort().getDescriptivePortName();
                        }
                    } else {
                        option = -1;
                    }
                } else if (option == 0) {
                    footballFieldPanel.setSerialTransmitter(st);
                    footballFieldPanel.addSetToField(footballFieldPanel.drill.sets.get(0));
                    runMenu.remove(runShowItem);
                    runMenu.add(stopShowItem);
                }
            }
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

        JMenuItem timeWeatherItem = new JMenuItem("Time & Weather Effects");
        timeWeatherItem.addActionListener(e -> showTimeWeatherDialog(frame));
        lightMenuPopup.add(timeWeatherItem);


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
    private void showTimeWeatherDialog(Frame parent) {
        JDialog dialog = new JDialog(parent, "Time & Weather Effects", true);
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner timeSpinner = new JSpinner(model);

        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date()); // will only show the current time

        JComboBox<String> weatherComboBox = new JComboBox<>(new String[]{"Clear", "Cloudy", "Rainy", "Snowy"});

        JButton confirmButton = new JButton("Apply");
        confirmButton.addActionListener(e -> {
            Date time = (Date) timeSpinner.getValue();
            String weather = (String) weatherComboBox.getSelectedItem();
            int transparency = calculateTransparency(time, weather);
            Drill drill = footballFieldPanel.drill;
            for (int i = 0; i < drill.coordinates.size(); i++) {
                Coordinate c = drill.coordinates.get(i);
                // Original color
                Color originalColor = c.getColor();
                Color colorWithNewTransparency = new Color(
                        originalColor.getRed(),
                        originalColor.getGreen(),
                        originalColor.getBlue(),
                        transparency
                );
                c.setColor(colorWithNewTransparency);
            }
            dialog.dispose();
        });

        dialog.setLayout(new GridLayout(0, 1));
        dialog.add(new JLabel("Time:"));
        dialog.add(timeSpinner);
        dialog.add(new JLabel("Select Weather Condition:"));
        dialog.add(weatherComboBox);
        dialog.add(confirmButton);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
    private int calculateTransparency(Date time, String weather) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int transparency;
        if (hour >= 6 && hour < 12) { // Morning
            transparency = 50;
        } else if (hour >= 12 && hour < 18) { // Afternoon
            transparency = 150;
        } else { // Evening
            transparency = 100;
        }
        switch (weather) {
            case "Clear":
                transparency -= 30;
                break;
            case "Cloudy":
                transparency += 20;
                break;
            case "Rainy":
                transparency += 40;
            case "Snowy":
                transparency += 150;
                break;
        }
        transparency = Math.min(Math.max(transparency, 0), 255);
        return transparency;
    }

    private void showPredefinedEffects(Frame parent) {
        Color selectedColor = JColorChooser.showDialog(parent, "Choose a Color", chosenColor);
        if (selectedColor != null) {
            chosenColor = selectedColor;
            colorDisplayPanel.setBackground(chosenColor);
            colorDisplayPanel.repaint();
        }
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

        ProjectFile pf = new ProjectFile(footballFieldPanel.drill, relArchive, relDrill, timeSync);
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

            timeSync = pf.timeSync;

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

    private void setPlaybackTimerTime() {
        float setSyncDuration = timeSync.get(scrubBarGUI.getCurrentSetIndex()).getValue();
        float setDuration = scrubBarGUI.getCurrSetDuration();
        playbackTimer.setDelay( Math.round(setSyncDuration / setDuration * 1000) );
    }
}
