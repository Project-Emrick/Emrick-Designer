package org.emrick.project;

import com.formdev.flatlaf.*;
import com.google.gson.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.emrick.project.audio.*;
import org.emrick.project.effect.*;
import org.emrick.project.serde.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import java.awt.Font;
import java.awt.Image;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;


public class MediaEditorGUI extends Component implements ImportListener, ScrubBarListener, SyncListener,
        FootballFieldListener, EffectListener, SelectListener, UserAuthListener, RFTriggerListener {

    // String definitions
    public static final String FILE_MENU_NEW_PROJECT = "New Project";
    public static final String FILE_MENU_OPEN_PROJECT = "Open Project";
    public static final String FILE_MENU_SAVE = "Save Project";

    // UI Components of MediaEditorGUI
    private final JFrame frame;
    private JPanel mainContentPanel;
    private final JPanel footballField;
    private final FootballFieldPanel footballFieldPanel;
    private final FootballFieldBackground footballFieldBackground;
    // dots
    private final JLabel sysMsg = new JLabel("Welcome to Emrick Designer!", SwingConstants.RIGHT);
    private final Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });
    // JSON serde
    private final Gson gson;
    private final Path userHome = Paths.get(System.getProperty("user.home"), ".emrick");
    private final String[] tutorialMessages = {
            "<html>Welcome to Media Editor!<br>Click 'File' to open or create new project.<br></html>",
            "Modify Each player over Main Panel",
            "<html>Use the Scrub Bar to manipulate your project. <br> You can change to the drill you wanted or " +
            "change the speed there <br></html> ",
            "<html>On the right side,<br> there are many effects that can apply to each performers.<br></html>",
            "Use the 'Help' menu for detailed documentation."
    };
    public int currentTutorialIndex = 0;
    private JPanel scrubBarPanel; // Refers directly to panel of ScrubBarGUI. Reduces UI refreshing issues.
    private ScrubBarGUI scrubBarGUI; // Refers to ScrubBarGUI instance, with functionality
    private JPanel effectViewPanel;
    private JPanel timelinePanel;
    private TimelineGUI timelineGUI;

    // Audio Components
    //  May want to abstract this away into some DrillPlayer class in the future
    private AudioPlayer audioPlayer;
    private boolean canSeekAudio = true;

    // Effect
    private EffectManager effectManager;
    private EffectGUI effectGUI;
    private Effect currentEffect;
    private Effect copiedEffect;
    private int selectedEffectType = 0;
    public final int DEFAULT_FUNCTION = 0x1;
    public final int TIME_GRADIENT = 0x2;
    public final int SET_TIMEOUT = 0x4;
    public final int DO_DELAY = 0x8;
    public final int INSTANT_COLOR = 0x10;
    public final int PROGRAMMING_MODE = 0x20;

    // RF Trigger
    private RFTriggerGUI rfTriggerGUI;
    private HashMap<Integer, RFTrigger> count2RFTrigger;

    // Time keeping
    // TODO: save this
    private TimeManager timeManager;
    private ArrayList<SyncTimeGUI.Pair> timeSync = null;
    private boolean useStartDelay; // If we are at the first count of the first set, useStartDelay = true
    private float startDelay; // Drills might not start immediately, therefore use this. Unit: seconds.
    private float playbackSpeed = 1;
    // The selected playback speed. For example "0.5", "1.0", "1.5". Use as a multiplier
    private Timer playbackTimer = null;
    private long frameStartTime;
    private long playbackStartMS;
    private int timeAdjustment = 0;
    // Project info
    private File archivePath = null;
    private File drillPath = null;
    private File csvFile;
    private Border originalBorder;  // To store the original border of the highlighted component

    public MediaEditorGUI() {
        // serde setup
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Color.class, new ColorAdapter());
        builder.registerTypeAdapter(Point2D.class, new Point2DAdapter());
        builder.registerTypeAdapter(SyncTimeGUI.Pair.class, new PairAdapter());
        builder.registerTypeAdapter(Duration.class, new DurationAdapter());
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
        playbackStartMS = 0;
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
                int startDelayMs = (int) (startDelay * 1000);
                Timer delayTimer = new Timer(startDelayMs, e2 -> {
                    playbackTimer.start();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
                return;
            }

            canSeekAudio = false;
            if (scrubBarGUI.isUseFps()) {
                frameStartTime = System.currentTimeMillis();
                if (scrubBarGUI.nextStep(playbackSpeed)) {
                    // Reached the end
                    playbackTimer.stop();
                    scrubBarGUI.setIsPlayingPlay();
                }
                setPlaybackTimerTimeByFps();
            } else {
                scrubBarGUI.nextCount();

                if (scrubBarGUI.isAtLastSet() && scrubBarGUI.isAtEndOfSet()) {
                    // Reached the end
                    playbackTimer.stop();
                    audioPlayer.pauseAudio();
                    scrubBarGUI.setIsPlayingPlay();
                    canSeekAudio = true;
                    return;
                } else if (scrubBarGUI.isAtEndOfSet()) {
                    scrubBarGUI.nextSet();
                }
                setPlaybackTimerTimeByCounts();
            }
            canSeekAudio = true;
        });

        // Change Font Size for Menu and MenuIem
        Font f = new Font("FlatLaf.style", Font.PLAIN, 14);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.font", f);
        UIManager.put("CheckBoxMenuItem.font", f);
        UIManager.put("RadioButtonMenuItem.font", f);

        // Field
        footballFieldPanel = new FootballFieldPanel(this);
        footballFieldPanel.setOpaque(false);
        //footballFieldPanel.setBackground(Color.lightGray); // temp. Visual indicator for unfilled space
        JScrollPane fieldScrollPane = new JScrollPane(footballFieldPanel);
        fieldScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fieldScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        footballFieldBackground = new FootballFieldBackground(this);
        footballField = new JPanel();

        // Main frame
        frame = new JFrame("Emrick Designer");

        // Scrub Bar
        scrubBarGUI = new ScrubBarGUI(frame, this, this, footballFieldPanel);

        // Scrub bar cursor starts on first count of drill by default
        useStartDelay = true;

        createAndShowGUI();
    }

    private void setPlaybackTimerTimeByFps() {
        scrubBarGUI.setPlaybackTime();
        playbackTimer.setDelay((int) (1 / scrubBarGUI.getFps() * 1000.0 / playbackSpeed));
    }

    private void setPlaybackTimerTimeByCounts() {
        float setSyncDuration = timeSync.get(scrubBarGUI.getCurrentSetIndex()).getValue();
        float setDuration = scrubBarGUI.getCurrSetDuration();
        playbackTimer.setDelay(Math.round(setSyncDuration / setDuration * 1000 / playbackSpeed));
    }

    public void createAndShowGUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);

        JMenuBar menuBar = new JMenuBar();

        ////////////////////////// Panels //////////////////////////

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        frame.add(topPanel, BorderLayout.NORTH);

        mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BorderLayout());

        // footballFieldPanel.setBorder(BorderFactory.createTitledBorder("Main View"));
        footballField.setLayout(new OverlayLayout(footballField));
        footballField.add(footballFieldPanel, BorderLayout.CENTER);
        footballField.add(footballFieldBackground, BorderLayout.CENTER);
        mainContentPanel.add(footballField, BorderLayout.CENTER);
//        mainContentPanel.add(footballFieldBackground, BorderLayout.CENTER);
//        mainContentPanel.add(footballFieldPanel, BorderLayout.CENTER);

        // Scrub Bar Panel
        buildScrubBarPanel();

        frame.add(mainContentPanel, BorderLayout.CENTER);

        // Timeline panel
        timelinePanel = new JPanel(new BorderLayout());
        timelinePanel.setBorder(BorderFactory.createTitledBorder("Timeline"));
        timelinePanel.setPreferredSize(new Dimension(frame.getWidth(), 120));
        frame.add(timelinePanel, BorderLayout.SOUTH);

        // Effect View panel
        effectGUI = new EffectGUI(EffectGUI.noProjectSyncMsg);
        effectViewPanel = new JPanel();
        effectViewPanel.setLayout(new BorderLayout());
        effectViewPanel.setPreferredSize(new Dimension(300, frame.getHeight()));
        effectViewPanel.setBorder(BorderFactory.createTitledBorder("Effect View"));
        effectViewPanel.add(effectGUI.getEffectPanel());
        frame.add(effectViewPanel, BorderLayout.EAST);

        ////////////////////////// Menu //////////////////////////

        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        // Import Pyware Project
        JMenuItem importItem = new JMenuItem(FILE_MENU_NEW_PROJECT);
        importItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        fileMenu.add(importItem);
        importItem.addActionListener(e -> {
            System.out.println("New Project...");
            new SelectFileGUI(frame, this);
        });
// TODO: select stuff
        // TODO: make sfg not local, have it load the project after import finishes// TODO: select stuff
// TODO: select stuff
        // Open Emrick Project// TODO: select stuff
        // https://www.codejava.net/java-se/swing/add-file-filter-for-jfilechooser-dialog// TODO: select stuff
        JMenuItem openItem = new JMenuItem(FILE_MENU_OPEN_PROJECT);// TODO: select stuff
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));// TODO: select stuff
        fileMenu.add(openItem);// TODO: select stuff
        openItem.addActionListener(e -> {// TODO: select stuff
            openProjectDialog();// TODO: select stuff
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

        // Export Emrick Packets
        JMenuItem exportItem = new JMenuItem("Export Emrick Packets File");
        exportItem.setEnabled(csvFile != null);
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
        fileMenu.add(exportItem);
        exportItem.addActionListener(e -> {
            System.out.println("Exporting packets...");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Project");
            if (fileChooser.showSaveDialog(fileMenu) == JFileChooser.APPROVE_OPTION) {
                System.out.println("Exporting file `" + fileChooser.getSelectedFile().getAbsolutePath() + "`.");
                exportPackets(new File(fileChooser.getSelectedFile().getAbsolutePath()));
            }
        });

        fileMenu.addSeparator();

        // Export CSV file
        JMenuItem exportCsvItem = new JMenuItem("Export Device IDs CSV");
        fileMenu.add(exportCsvItem);
        exportCsvItem.addActionListener(e -> {
            ArrayList<Performer> performers = footballFieldPanel.drill.performers;

            JFileChooser fileChooser = SelectFileGUI.getFileChooser("Device ID Comma Separated Values (*.csv)", ".csv");

            int retVal = fileChooser.showSaveDialog(null);

            System.out.println("retVal = " + retVal);

            if (retVal == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                exportCsvFileForPerformerDeviceIDs(selectedFile);
            }

        });

        // Import CSV file
        JMenuItem importCsvItem = new JMenuItem("Import Device IDs CSV");
        fileMenu.add(importCsvItem);
        importCsvItem.addActionListener(e -> {
            JFileChooser fileChooser = SelectFileGUI.getFileChooser("Device ID Comma Separated Values (*.csv)", ".csv");

            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println("CSV     | Selected file: " + selectedFile.getAbsoluteFile());
                csvFile = selectedFile;
                exportItem.setEnabled(true);
                parseCsvFileForPerformerDeviceIDs(csvFile);
            }
        });

        fileMenu.addSeparator();

        // Demos
        JMenuItem displayCircleDrill = new JMenuItem("Load Demo Drill Object");
        displayCircleDrill.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                                                                 Toolkit.getDefaultToolkit()
                                                                        .getMenuShortcutKeyMaskEx()));
        fileMenu.add(displayCircleDrill);
        displayCircleDrill.addActionListener(e -> loadDemoDrillObj());

        JMenuItem displayTestDrill = new JMenuItem("Load Test Drill Object");
        displayTestDrill.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                                                               Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        fileMenu.add(displayTestDrill);
        displayTestDrill.addActionListener(e -> loadTestDrillObj());

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        JMenuItem undoColorsItem = new JMenuItem("Undo");
        undoColorsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                                                             Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoColorsItem.addActionListener(e -> {
            //undoColorChange();
            effectManager.undo();
            footballFieldPanel.repaint();
        });
        editMenu.add(undoColorsItem);

        JMenuItem redoColorsItem = new JMenuItem("Redo");
        redoColorsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                                                             Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        redoColorsItem.addActionListener(e -> {
            //redoColorChange();
            effectManager.redo();
            footballFieldPanel.repaint();
        });
        editMenu.add(redoColorsItem);

        editMenu.addSeparator();

        JMenuItem removeEffectsForAll = new JMenuItem("Reset All Performers");
        editMenu.add(removeEffectsForAll);
        removeEffectsForAll.addActionListener(e -> {
            if (this.effectManager == null) return;
            this.effectManager.removeAllEffectsFromAllPerformers();

            // TODO: Below is deprecated. Schedule for removal.
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
                p.setColor(new Color(0, 0, 0));
                for (int j = 0; j < p.getCoordinates().size(); j++) {
                    Coordinate c = p.getCoordinates().get(j);
                    c.setColor(new Color(0, 0, 0));
                }
            }

            footballFieldPanel.drill = drill;
            footballFieldPanel.repaint();
        });

        // Remove effects for selected
        JMenuItem removeEffectsForSelected = new JMenuItem("Reset Selected Performers");
        removeEffectsForSelected.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                                                                       Toolkit.getDefaultToolkit()
                                                                              .getMenuShortcutKeyMaskEx()));
        editMenu.add(removeEffectsForSelected);
        removeEffectsForSelected.addActionListener(e -> {
            if (this.effectManager == null) return;
            this.effectManager.removeAllEffectsFromSelectedPerformers();
            this.footballFieldPanel.repaint();
        });

        editMenu.addSeparator();

        // Copy current effect
        JMenuItem copyCurrentEffect = new JMenuItem("Copy Effect");
        copyCurrentEffect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                                Toolkit.getDefaultToolkit()
                                                                       .getMenuShortcutKeyMaskEx()));
        copyCurrentEffect.addActionListener(e -> {
            if (this.effectManager == null) return;
            if (this.currentEffect == null) {
                JOptionPane.showMessageDialog(frame,
                                              "No effect to copy.",
                                              "Copy Effect: Warning",
                                              JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(frame,
                                          "Effect copied.",
                                          "Copy Effect: Success",
                                          JOptionPane.INFORMATION_MESSAGE);
            this.copiedEffect = this.currentEffect;
        });
        editMenu.add(copyCurrentEffect);

        // Paste copied effect
        JMenuItem pasteCopiedEffect = new JMenuItem("Paste Effect");
        pasteCopiedEffect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                                                                Toolkit.getDefaultToolkit()
                                                                       .getMenuShortcutKeyMaskEx()));
        pasteCopiedEffect.addActionListener(e -> {
            if (this.effectManager == null) return;
            boolean success = this.effectManager.addEffectToSelectedPerformers(this.copiedEffect);
            if (success) updateEffectViewPanel(selectedEffectType);
            this.footballFieldPanel.repaint();
        });
        editMenu.add(pasteCopiedEffect);

        editMenu.addSeparator();

        JMenuItem selectByCrit = new JMenuItem("Select by Criteria");
        selectByCrit.addActionListener(e -> {
            if (archivePath == null || drillPath == null) {
                System.out.println("no project loaded");
                return;
            }

            HashSet<Integer> labels = new HashSet<>();
            HashSet<String> symbols = new HashSet<>();
            for (Performer p : footballFieldPanel.drill.performers) {
                labels.add(p.getLabel());
                symbols.add(p.getSymbol());
            }
            System.out.println("selecting by criteria...");
            FilterSelect filterSelect = new FilterSelect(frame, this, labels, symbols);
            filterSelect.show();
        });
        editMenu.add(selectByCrit);

        // View menu
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        JCheckBoxMenuItem toggleFloorCoverImage = new JCheckBoxMenuItem("Show Floor Cover Image");
        toggleFloorCoverImage.setState(true);
        toggleFloorCoverImage.addActionListener(e -> {
            footballFieldPanel.setShowFloorCoverImage(!footballFieldPanel.getShowFloorCoverImage());
            footballFieldPanel.repaint();
        });
        viewMenu.add(toggleFloorCoverImage);
        JCheckBoxMenuItem toggleSurfaceImage = new JCheckBoxMenuItem("Show Surface Image");
        toggleSurfaceImage.setState(true);
        toggleSurfaceImage.addActionListener(e -> {
            footballFieldPanel.setShowSurfaceImage(!footballFieldPanel.getShowSurfaceImage());
            footballFieldPanel.repaint();
        });
        viewMenu.add(toggleSurfaceImage);

        // Run menu
        JMenu runMenu = new JMenu("Run");
        menuBar.add(runMenu);
        JMenuItem runShowItem = new JMenuItem("Run Show Linked to Viewport");
        runMenu.add(runShowItem);
        JMenuItem flowViewerItem = new JMenuItem("Run Show via Flow View");
        runMenu.add(flowViewerItem);
        JMenuItem programItem = new JMenuItem("Enter Programming Mode");
        runMenu.add(programItem);
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
        programItem.addActionListener(e -> {
            SerialTransmitter st = new SerialTransmitter();
            String port = st.getSerialPort().getDescriptivePortName();
            int option = 1;
            while (option > 0) {
                if (option == 1) {
                    option = JOptionPane.showConfirmDialog(null,
                            "Is (" + port + ") the correct port for the transmitter?",
                            "Run Show",
                            JOptionPane.YES_NO_OPTION);
                } else if (option == 2) {
                    option = JOptionPane.showConfirmDialog(null,
                            "Port invalid: Make sure you have the right port and that "
                                    + "it is not already in use then try again.",
                            "Run show: ERROR",
                            JOptionPane.OK_CANCEL_OPTION);
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
                }
            }
            st.writeToSerialPort("p");
        });
        flowViewerItem.addActionListener(e -> {
            SerialTransmitter st = new SerialTransmitter();
            String port = st.getSerialPort().getDescriptivePortName();
            int option = 1;
            while (option > 0) {
                if (option == 1) {
                    option = JOptionPane.showConfirmDialog(null,
                            "Is (" + port + ") the correct port for the transmitter?",
                            "Run Show",
                            JOptionPane.YES_NO_OPTION);
                } else if (option == 2) {
                    option = JOptionPane.showConfirmDialog(null,
                            "Port invalid: Make sure you have the right port and that "
                                    + "it is not already in use then try again.",
                            "Run show: ERROR",
                            JOptionPane.OK_CANCEL_OPTION);
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
            flowFrame.setSize(1200, 800);
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
                int last = -1;

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        if (e.getFirstIndex() == last) {
                            st.writeSet(e.getLastIndex());
                            last = e.getLastIndex();
                        } else {
                            st.writeSet(e.getFirstIndex());
                            last = e.getFirstIndex();
                        }
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
                    option = JOptionPane.showConfirmDialog(null,
                            "Is (" + port + ") the correct port for the transmitter?",
                            "Run Show",
                            JOptionPane.YES_NO_OPTION);
                } else if (option == 2) {
                    option = JOptionPane.showConfirmDialog(null,
                            "Port invalid: Make sure you have the right port and that "
                                    + "it is not already in use then try again.",
                            "Run show: ERROR",
                            JOptionPane.OK_CANCEL_OPTION);
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
        JMenuItem viewDocItem = new JMenuItem("View Document (Github Wiki)");
        helpMenu.add(viewDocItem);
        viewDocItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: View document"));

        JMenuItem submitIssueItem = new JMenuItem("Submit Issue (Github Issues)");
        helpMenu.add(submitIssueItem);
        submitIssueItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "You clicked: Submit an Issue"));


        /*
            Tutorial
         */
        JMenuItem tutorialButton = new JMenuItem("Tutorial");
        helpMenu.add(tutorialButton);
        tutorialButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                currentTutorialIndex = 0;
                if (currentTutorialIndex < tutorialMessages.length) {

                    // OPen or Create project
                    if (currentTutorialIndex == 0) {
                        System.out.println("first one\n");
                        fileMenu.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        Timer timer = new Timer(1000, new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                fileMenu.setBorder(BorderFactory.createLineBorder(Color.BLACK, 0));
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
//                        displayNonModalTip(tutorialMessages[currentTutorialIndex]);
                    }
                    displayNonModalTip(tutorialMessages[currentTutorialIndex]);

                    // 1
//                    if (currentTutorialIndex == 1) {
//                        System.out.println("second\n");
//                        displayNonModalTip(tutorialMessages[currentTutorialIndex]);
//
//                    }
//                    System.out.println(currentTutorialIndex);
                }

            }
//            private void displayNonModalTip(String message) {
//                JWindow tipWindow = new JWindow(frame);
//                JPanel contentPane = new JPanel(new BorderLayout());
//                contentPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
//                contentPane.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER);
//
//                JPanel buttonPanel = new JPanel();
//                JButton nextButton = new JButton("Next");
//                JButton closeButton = new JButton("Close");
//
//                buttonPanel.add(closeButton);
//                buttonPanel.add(nextButton);
//                contentPane.add(buttonPanel, BorderLayout.SOUTH);
//                nextButton.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        tipWindow.dispose();
//                    }
//                });
//                closeButton.addActionListener(e -> tipWindow.dispose());
//
//                tipWindow.setContentPane(contentPane);
//                tipWindow.setSize(400, 100);
//                tipWindow.setLocation(frame.getLocationOnScreen().x + (frame.getWidth() - tipWindow.getWidth()) / 2,
//                        frame.getLocationOnScreen().y + (frame.getHeight() - tipWindow.getHeight()) / 2);
//                tipWindow.setVisible(true);
//
//            }
        });

        JMenuItem loginItem = new JMenu("Account");
        //loginItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        menuBar.add(loginItem);

        JMenuItem signIn = new JMenuItem("Sign In");
        signIn.addActionListener(e -> {
            System.out.println("Signing in...");
            new UserAuthGUI(frame, this); // This assumes UserAuthGUI sets itself visible
        });
        loginItem.add(signIn);

        // System message
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(sysMsg);

        //Light menu. and adjust its menu location
        JButton effectOptions = getEffectOptionsButton();
        effectViewPanel.add(effectOptions, BorderLayout.NORTH);

        // handle closing the window
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (archivePath != null && drillPath != null) {
                    int resp = JOptionPane.showConfirmDialog(frame,
                                                             "Would you like to save before quitting?",
                                                             "Save and Quit?",
                                                             JOptionPane.YES_NO_CANCEL_OPTION);
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

    private JButton getEffectOptionsButton() {
        JPopupMenu lightMenuPopup = new JPopupMenu();

        JMenuItem timeWeatherItem = new JMenuItem("Time & Weather Effects");
        timeWeatherItem.addActionListener(e -> showTimeWeatherDialog(frame));
        lightMenuPopup.add(timeWeatherItem);

        lightMenuPopup.addSeparator();

        JMenuItem createGridPattern = new JMenuItem("Create Grid Pattern");
        createGridPattern.addActionListener(e -> showGridPatternDialog(frame));
        lightMenuPopup.add(createGridPattern);

        lightMenuPopup.addSeparator();

        JMenuItem lightDescription = new JMenuItem("Create Light Description");
        lightDescription.addActionListener(e-> showLightDescription(frame));
        lightMenuPopup.add(lightDescription);

        JMenuItem effectDescription = new JMenuItem("Create Effect Group Descriptions");
        effectDescription.addActionListener(e-> showEffectGroupDescriptions());
        lightMenuPopup.add(effectDescription);

        lightMenuPopup.addSeparator();

        JMenuItem fadePattern = new JMenuItem("Create Fade Pattern");
        fadePattern.addActionListener(e -> {
            selectedEffectType = effectGUI.GENERATED_FADE;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(fadePattern);

        JMenuItem staticColorPattern = new JMenuItem("Create Static Color Pattern");
        staticColorPattern.addActionListener(e -> {
            selectedEffectType = effectGUI.STATIC_COLOR;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(staticColorPattern);

        JMenuItem wavePattern = new JMenuItem("Create Wave Pattern");
        wavePattern.addActionListener(e -> {
            selectedEffectType = effectGUI.WAVE;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(wavePattern);

        // Button that triggers the popup menu
        JButton lightButton = new JButton("Effect Options");
        lightButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int x = 0;
                int y = lightButton.getHeight();
                lightMenuPopup.show(lightButton, x, y);
            }
        });
        return lightButton;
    }

    public void loadDemoDrillObj() {
        clearDotsFromField();
        String filePath = "./src/test/java/org/emrick/project/ExpectedPDFOutput.txt";
        try {
            String DrillString = Files.lines(Paths.get(filePath)).collect(Collectors.joining(System.lineSeparator()));
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

    public void loadTestDrillObj() {
        clearDotsFromField();
        String filePath = "./src/test/java/org/emrick/project/testDrillParsed.txt";
        try {
            String DrillString = Files.lines(Paths.get(filePath)).collect(Collectors.joining(System.lineSeparator()));
            DrillParser parse1 = new DrillParser();
            Drill drilltest = parse1.parseWholeDrill(DrillString);
            footballFieldPanel.drill = drilltest;
            footballFieldPanel.addSetToField(drilltest.sets.get(0));

        } catch (IOException e) {
            e.printStackTrace();
        }
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

            if (pf.timeSync != null && pf.startDelay != null) {
                timeSync = pf.timeSync;
                onSync(timeSync, pf.startDelay);
                scrubBarGUI.setTimeSync(timeSync);
                startDelay = pf.startDelay;
                count2RFTrigger = pf.count2RFTrigger;
                setupEffectView();
            }

        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            writeSysMsg("Failed to open to `" + path + "`.");
            throw new RuntimeException(e);
        }
    }

    public void clearDotsFromField() {
        footballFieldPanel.clearDots();
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

    @Override
    public void onMultiSelect(HashSet<Integer> labels, HashSet<String> symbols) {
        // TODO: select stuff
//        footballFieldPanel.selectedPerformers
        for (Performer p : footballFieldPanel.drill.performers) {
            if (labels.contains(p.getLabel()) || symbols.contains(p.getSymbol())) {
                String key = p.getSymbol() + p.getLabel();
                footballFieldPanel.selectedPerformers.put(key, p);
            }
        }
        footballFieldPanel.repaint();
    }

    private void exportCsvFileForPerformerDeviceIDs(File selectedFile) {
        try (FileWriter fileWriter = new FileWriter(selectedFile)) {
            for (Performer performer : footballFieldPanel.drill.performers) {
                fileWriter.write((Integer.parseInt(performer.getDeviceId()) * 2) + "");
                fileWriter.write(",");
                fileWriter.write(performer.getIdentifier() + "L");
                fileWriter.write(System.lineSeparator());
                fileWriter.write((Integer.parseInt(performer.getDeviceId()) * 2 + 1) + "");
                fileWriter.write(",");
                fileWriter.write(performer.getIdentifier() + "R");
                fileWriter.write(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseCsvFileForPerformerDeviceIDs(File inputFile) {
        try (var fileReader = new FileReader(inputFile); var bufferReader = new BufferedReader(fileReader)) {
            String temp = "";
            //read file
            int linesCount = 0;
            while ((temp = bufferReader.readLine()) != null) {
                if (!temp.contains(",")) {
                    continue;
                }
                String[] tmpContent = temp.split(",");
                for (String s : tmpContent) {
                    if (!s.trim().isEmpty()) {
                        if (tmpContent[1].contains("L")) {
                            footballFieldPanel.drill.performers.stream()
                                    .filter(performer -> performer.getIdentifier()
                                            .equals(tmpContent[1].substring(0,tmpContent[1].length()-1)))
                                    .findFirst()
                                    .ifPresent(performer -> performer.setDeviceId(tmpContent[0]));
                        }
                    }
                }
                linesCount = linesCount + 1;
            }

            if (linesCount != footballFieldPanel.drill.performers.size()) {
                exportCsvFileForPerformerDeviceIDs(inputFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ////////////////////////// Sync Listeners //////////////////////////

    private void openProjectDialog() {
        System.out.println("Opening project...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Project");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Emrick Project Files (emrick, json)",
                                                                       "emrick",
                                                                       "json"));

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Opening file `" + fileChooser.getSelectedFile().getAbsolutePath() + "`.");
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
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Emrick Project Files (emrick, json)",
                                                                       "emrick",
                                                                       "json"));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Saving file `" + fileChooser.getSelectedFile().getAbsolutePath() + "`.");
            saveProject(fileChooser.getSelectedFile(), archivePath, drillPath);
        }
    }

    ////////////////////////// Scrub Bar Listeners //////////////////////////

    private void showLightDescription(Frame parentFrame){
        JDialog dialog = new JDialog(parentFrame, "Light Description", true);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(Color.WHITE);

        JTextArea textArea = new JTextArea(10, 20); // Adjust the size as needed
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(221, 221, 221)), // Outer border color
                BorderFactory.createEmptyBorder(5, 5, 5, 5))); // Inner padding

        textArea.setDocument(new LimitedDocument(5000));
        JLabel charCountLabel = new JLabel("5000 characters remaining");
        updateCharCountLabel(charCountLabel, textArea.getText().length(), 5000);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateCharCountLabel(charCountLabel, textArea.getText().length(), 5000);
            }

            public void removeUpdate(DocumentEvent e) {
                updateCharCountLabel(charCountLabel, textArea.getText().length(), 5000);
            }

            public void changedUpdate(DocumentEvent e) {
                updateCharCountLabel(charCountLabel, textArea.getText().length(), 5000);
            }
        });

        JButton exportButton = new JButton("Export to PDF");
        exportButton.setFocusPainted(false);
        exportButton.setBackground(new Color(32, 136, 203)); // Button background color
        exportButton.setForeground(Color.WHITE); // Button text color

        contentPanel.add(scrollPane, BorderLayout.CENTER); // Add scroll pane to the center
        contentPanel.add(charCountLabel, BorderLayout.NORTH); // Add character count label at the top
        contentPanel.add(exportButton, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);

        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(charCountLabel, BorderLayout.NORTH);
        dialog.add(exportButton, BorderLayout.PAGE_END);

        dialog.setSize(350, 250);
        dialog.setResizable(false);

        dialog.setLocationRelativeTo(parentFrame);

        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportToPDF(textArea.getText());
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    private void showEffectGroupDescriptions() {
        if (this.effectManager == null) {
            return;
        }

        System.out.println("this.footballFieldPanel.selectedPerformers.size() = " + this.footballFieldPanel.selectedPerformers.size());
        if (this.footballFieldPanel.selectedPerformers.size() > 1) {
            this.effectViewPanel.remove(this.effectGUI.getEffectPanel());
            this.effectViewPanel.revalidate();
            this.effectViewPanel.repaint();

            this.currentEffect = null;

            String placeholderText = EffectGUI.noPerformerMsg;
            Map<Performer, Collection<Effect>> selectedEffects = new LinkedHashMap<>();

            for (Performer performer : this.footballFieldPanel.selectedPerformers.values()) {
                if (performer.getEffects() == null || performer.getEffects().isEmpty()) {
                    placeholderText = EffectGUI.noEffectGroupMsg;
                } else {
                    selectedEffects.put(performer, performer.getEffects());
                }
            }

            this.effectGUI = new EffectGUI(placeholderText);
            if (placeholderText.equals(EffectGUI.noEffectGroupMsg)) {
                effectGUI.setSelectedEffects(new LinkedHashMap<>());
            } else {
                effectGUI.setSelectedEffects(selectedEffects);
            }

            this.effectViewPanel.add(this.effectGUI.getEffectPanel());
            this.effectViewPanel.revalidate();
            this.effectViewPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(null,
                    "Please select multiple performers to use the effect group feature",
                    "Effect Group: Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showGridPatternDialog(JFrame frame) {

        // Show 'Create Grid Pattern' window -- Calculations performed internally.
        ArrayList<Performer> selectedPerformers = new ArrayList<>(footballFieldPanel.selectedPerformers.values());
        if (selectedPerformers.size() < 4) {
            JOptionPane.showMessageDialog(null,
                    "Please select multiple performers arranged in a grid to use the grid pattern feature",
                    "Grid Pattern: Error", JOptionPane.ERROR_MESSAGE);
        }

        // Get the current time by using current count
        long startTimeMSec = timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());

        new GridPatternGUI(frame, selectedPerformers, effectManager, startTimeMSec);
    }

    ////////////////////////// Effect Listeners //////////////////////////

    private void updateCharCountLabel(JLabel label, int currentLength, int maxChars) {
        label.setText((maxChars - currentLength) + " characters remaining");
    }

    private void exportToPDF(String textContent) {
        if (textContent.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The text area cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream("LightDescription.pdf"));
            document.open();
            document.add(new Paragraph(textContent));
            document.close();
            JOptionPane.showMessageDialog(this,
                                          "PDF exported successfully!",
                                          "Success",
                                          JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                                          "Could not create PDF: " + e.getMessage(),
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTimeWeatherDialog(Frame parent) {
        JDialog dialog = new JDialog(parent, "Time & Weather", true);
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
            footballFieldPanel.setEffectTransparency(transparency); // Added

            // TODO: Deprecated, scheduled for removal: manage colors via Coordinate class
            Drill drill = footballFieldPanel.drill;
            for (int i = 0; i < drill.coordinates.size(); i++) {
                Coordinate c = drill.coordinates.get(i);
                Color originalColor = c.getColor();
                Color colorWithNewTransparency = new Color(originalColor.getRed(),
                                                           originalColor.getGreen(),
                                                           originalColor.getBlue(),
                                                           transparency);
                c.setColor(colorWithNewTransparency);
            }
            footballFieldPanel.repaint();
            dialog.dispose();
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            footballFieldPanel.setEffectTransparency(255);
            footballFieldPanel.repaint();
            dialog.dispose();
        });

        JPanel timeWeatherPanel = new JPanel(new GridLayout(0, 1, 0, 1));
        timeWeatherPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        timeWeatherPanel.add(new JLabel("Select Time:"));
        timeWeatherPanel.add(timeSpinner);
        timeWeatherPanel.add(new JLabel("Select Weather Condition:"));
        timeWeatherPanel.add(weatherComboBox);
        timeWeatherPanel.add(new JPanel());
        timeWeatherPanel.add(resetButton);
        timeWeatherPanel.add(confirmButton);

        dialog.add(timeWeatherPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    ////////////////////////// Football Field Listeners //////////////////////////

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
                break;
            case "Snowy":
                transparency += 150;
                break;
        }
        transparency = Math.min(transparency, 255);
        return transparency;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        // Run this program on the Event Dispatch Thread (EDT)
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() { new MediaEditorGUI(); }
        });
    }

    ////////////////////////// Importing Listeners //////////////////////////

    @Override
    public void onImport() {
        scrubBarGUI.setReady(true);
    }

    @Override
    public void onFileSelect(File archivePath, File drillPath, File csvFile) {
        this.archivePath = archivePath;
        this.drillPath = drillPath;
        this.csvFile = csvFile;
    }

    @Override
    public void onFloorCoverImport(Image image) {
        footballFieldBackground.setFloorCoverImage((BufferedImage) image);
        footballFieldPanel.setFloorCoverImage(image);
        footballFieldBackground.repaint();
        footballFieldPanel.repaint();
    }

    @Override
    public void onSurfaceImport(Image image) {
        footballFieldBackground.setSurfaceImage((BufferedImage) image);
        footballFieldPanel.setSurfaceImage(image);
        footballFieldBackground.repaint();
        footballFieldPanel.repaint();
    }

    @Override
    public void onAudioImport(File audioFile) {
        // Playing or pausing audio is done through the AudioPlayer service class
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

        // At the point of import process, the project is ready to sync
        //scrubBarGUI.getSyncButton().doClick();
    }

    ////////////////////////// Sync Listeners //////////////////////////

    @Override
    public void onSync(ArrayList<SyncTimeGUI.Pair> times, float startDelay) {
        System.out.println("MediaEditorGUI: Got Synced Times");

        this.timeSync = times;
        this.startDelay = startDelay;

        scrubBarGUI.setTimeSync(timeSync);
        count2RFTrigger = new HashMap<>();

        setupEffectView();
    }

    private void setupEffectView() {

        // Recalculate set to count map (pageTab2Count) to initialize timeManager
        Map<String, Integer> pageTab2Count = new HashMap<>();
        int startCount = 0;
        for (Set s : footballFieldPanel.drill.sets) {
            startCount += s.duration;
            pageTab2Count.put(s.label, startCount);
        }
        // Initialize important state member variables
        this.timeManager = new TimeManager(pageTab2Count, this.timeSync, this.startDelay);
        this.effectManager = new EffectManager(this.footballFieldPanel, this.timeManager, this.count2RFTrigger);
        this.footballFieldPanel.setEffectManager(this.effectManager);

        updateEffectViewPanel(selectedEffectType);
        updateRFTriggerButton();
    }

    ////////////////////////// Scrub Bar Listeners //////////////////////////

    @Override
    public boolean onPlay() {
        if (timeSync == null) {
            JOptionPane.showMessageDialog(frame, "Cannot play without syncing time!",
                    "Playback Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (audioPlayer != null && scrubBarGUI.getAudioCheckbox().isSelected()) {
            playAudioFromCorrectPosition();
        }
        if (scrubBarGUI.isUseFps()) {
            setPlaybackTimerTimeByFps();
            footballFieldPanel.setUseFps(true);
            playbackStartMS = System.currentTimeMillis() - timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
            System.out.println("Start time: " + timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount()));
        } else {
            setPlaybackTimerTimeByCounts();
            footballFieldPanel.setUseFps(false);
        }
        playbackTimer.start();
        return true;
    }

    @Override
    public boolean onPause() {
        if (timeSync == null) {
            JOptionPane.showMessageDialog(frame, "Cannot play without syncing time!",
                    "Playback Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (audioPlayer != null) {
            audioPlayer.pauseAudio();
        }
        playbackTimer.stop();
        return true;
    }

    @Override
    public long onScrub() {
        // If time cursor is at start of first set, arm the start-delay
        useStartDelay = scrubBarGUI.isAtFirstSet() && scrubBarGUI.isAtStartOfSet();
        if (this.footballFieldPanel.getNumSelectedPerformers() > 0) {
            updateEffectViewPanel(selectedEffectType);
        }
        // If triggers are ready to be used, refresh on scroll
        if (count2RFTrigger != null) {
            updateRFTriggerButton();
        }
        if (scrubBarGUI.isPlaying() && canSeekAudio) {
            System.out.println("Called onScrub() -> Seeking audio...");
            playAudioFromCorrectPosition();
        }
        if (timeManager != null) {
            return timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
        }
        return 0;
    }

    @Override
    public void onTimeChange(long time) {
        footballFieldPanel.currentMS = time;
    }

    private void updateRFTriggerButton() {
        // Create a create/delete button depending on whether there is RF trigger at current count
        if (rfTriggerGUI != null) {
            effectViewPanel.remove(rfTriggerGUI.getCreateDeleteBtn());
            effectViewPanel.revalidate();
            effectViewPanel.repaint();
        }
        int currentCount = footballFieldPanel.getCurrentCount();
        RFTrigger currentRFTrigger = count2RFTrigger.get(currentCount);
        rfTriggerGUI = new RFTriggerGUI(
                currentCount, timeManager.getCount2MSec().get(currentCount), currentRFTrigger, this);

        effectViewPanel.add(rfTriggerGUI.getCreateDeleteBtn(), BorderLayout.SOUTH);
        effectViewPanel.revalidate();
        effectViewPanel.repaint();
    }

    private void playAudioFromCorrectPosition() {
        // Get audio to correct position before playing
        if (!scrubBarGUI.getAudioCheckbox().isSelected()) {
            audioPlayer.pauseAudio();
            return;
        }
        long timestampMillis = timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
        if (useStartDelay) {
            timestampMillis -= (long) (startDelay * 1000);
        }
        audioPlayer.pauseAudio();
        audioPlayer.playAudio(timestampMillis);
    }

    @Override
    public void onSpeedChange(float playbackSpeed) {
        System.out.println("MediaEditorGUI: playbackSpeed = " + playbackSpeed);
        // If playback speed is not normal, don't play the audio (simple solution)
        if (playbackSpeed != 1) {
            scrubBarGUI.getAudioCheckbox().setSelected(false);
            scrubBarGUI.getAudioCheckbox().setEnabled(false);
            if (audioPlayer != null) audioPlayer.pauseAudio();
        } else {
            scrubBarGUI.getAudioCheckbox().setEnabled(true);
        }
        this.playbackSpeed = playbackSpeed;
    }

    ////////////////////////// Effect Listeners //////////////////////////

    @Override
    public void onUserLoggedIn(String username) {
        frame.setTitle("Emrick Designer - Welcome "+username);
    }

    private void showEffectBeforeFirstTriggerError() {
        JOptionPane.showMessageDialog(null,
                "Could not create effect. Ensure that an RF Trigger is placed before or at the same time as the first effect.",
                "Create Effect: Error", JOptionPane.ERROR_MESSAGE);
    }
    @Override
    public void onCreateEffect(Effect effect) {
        if (effect.getStartTimeMSec() < ((RFTrigger) count2RFTrigger.values().toArray()[0]).getTimestampMillis()) {
            showEffectBeforeFirstTriggerError();
            return;
        }
        boolean successful = this.effectManager.addEffectToSelectedPerformers(effect);
        this.footballFieldPanel.repaint();
        if (successful) {
            updateEffectViewPanel(selectedEffectType);
            updateTimelinePanel();
        }
    }

    @Override
    public void onUpdateEffect(Effect oldEffect, Effect newEffect) {
        this.effectManager.replaceEffectForSelectedPerformers(oldEffect, newEffect);
        this.footballFieldPanel.repaint();
        updateEffectViewPanel(selectedEffectType);
        updateTimelinePanel();
    }

    @Override
    public void onDeleteEffect(Effect effect) {
        this.effectManager.removeEffectFromSelectedPerformers(effect);
        this.footballFieldPanel.repaint();
        updateEffectViewPanel(selectedEffectType);
        updateTimelinePanel();
    }

    ////////////////////////// Football Field Listeners //////////////////////////

    @Override
    public void onPerformerSelect() {
        updateEffectViewPanel(selectedEffectType);
        updateTimelinePanel();
    }

    @Override
    public void onPerformerDeselect() {
        updateEffectViewPanel(selectedEffectType);
        updateTimelinePanel();
    }

    ////////////////////////// RF Trigger Listeners //////////////////////////

    @Override
    public void onCreateRFTrigger(RFTrigger rfTrigger) {
        if (!effectManager.isValid(rfTrigger)) {
            return;
        }
        count2RFTrigger.put(footballFieldPanel.getCurrentCount(), rfTrigger);
        updateRFTriggerButton();
        updateTimelinePanel();
        JOptionPane.showMessageDialog(null,
                "RF trigger created successfully at count " + footballFieldPanel.getCurrentCount(),
                "RF Trigger Create: Success", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onDeleteRFTrigger(int count) {
        count2RFTrigger.remove(count);
        updateRFTriggerButton();
        JOptionPane.showMessageDialog(null,
                "RF trigger at count " + count + " deleted successfully", "RF Trigger Delete: Success",
                JOptionPane.INFORMATION_MESSAGE);
        updateTimelinePanel();
    }

    ////////////////////////// Effect Listeners //////////////////////////

    @Override
    public void onResizeBackground() {
        footballFieldPanel.setFieldHeight(footballFieldBackground.getFieldHeight());
        footballFieldPanel.setFieldWidth(footballFieldBackground.getFieldWidth());
        footballFieldPanel.setFrontSideline50(footballFieldBackground.getFrontSideline50());
        footballFieldPanel.repaint();
    }

    @Override
    public void onFinishRepaint() {
        if (scrubBarGUI.isPlaying() && scrubBarGUI.isUseFps()) {

            try {
                double currTime;
                if (footballFieldPanel.getCurrentSet().index > 0) {
                    currTime = scrubBarGUI.getTime() * 1000 + timeManager.getSet2MSec().get(footballFieldPanel.getCurrentSet().index).getValue();
                } else {
                    currTime = scrubBarGUI.getTime() * 1000;
                }
                double timeDiff = System.currentTimeMillis() - playbackStartMS - currTime;
                playbackTimer.setDelay(playbackTimer.getDelay() - (int) timeDiff);
            }
            catch (IllegalArgumentException iae) {
                if (frameStartTime != 0) {
                    playbackTimer.setDelay(0);
                }
            }
        }
    }

    private void updateEffectViewPanel(int effectType) {

        // No point in updating effect view if can't use effects
        if (effectManager == null) return;

        // Remove existing effect data
        effectViewPanel.remove(effectGUI.getEffectPanel());
        effectViewPanel.revalidate();
        effectViewPanel.repaint();

        // Effects
        // TODO: Eventually, for multiple selected performers, also check if not all selected performers share the same effect
        if (footballFieldPanel.selectedPerformers.size() < 1) {
            // Eventually should be able to work with multiple performers at a time
            currentEffect = null;
            effectGUI = new EffectGUI(EffectGUI.noPerformerMsg);
            effectViewPanel.add(effectGUI.getEffectPanel(), BorderLayout.CENTER);

            return;
        }

        // FIXME: Here, we know there's only one performer selected. The EffectGUI is for that single performer, for now
        long currentMSec = timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
        currentEffect = effectManager.getEffectsFromSelectedPerformers(currentMSec);
        if (currentEffect == null) {
            // Eventually should be able to work with multiple performers at a time
            currentEffect = null;
            effectGUI = new EffectGUI(EffectGUI.noCommonEffectMsg);
            effectViewPanel.add(effectGUI.getEffectPanel(), BorderLayout.CENTER);

            return;
        }
        effectGUI = new EffectGUI(currentEffect, currentMSec, this, selectedEffectType);

        // Add updated data for effect view
        effectViewPanel.add(effectGUI.getEffectPanel(), BorderLayout.CENTER);
        effectViewPanel.revalidate();
        effectViewPanel.repaint();
    }

    private void updateTimelinePanel() {

        // No point in updating timeline if project has not been synced
        if (timeManager == null) return;

        // Remove existing timeline data if it exists
        if (timelineGUI != null) {
            timelinePanel.remove(timelineGUI.getTimelineScrollPane());
            timelinePanel.revalidate();
            timelinePanel.repaint();
        }

        // Get effects of selected performers, if applicable, else will be null
        HashSet<Effect> effectsSet = new HashSet<>();
        for (Map.Entry<String, Performer> selected : footballFieldPanel.selectedPerformers.entrySet()) {
            effectsSet.addAll(selected.getValue().getEffects());
        }
        ArrayList<Effect> effectsList = new ArrayList<>(effectsSet);
        timelineGUI = new TimelineGUI(effectsList, count2RFTrigger);

        timelinePanel.add(timelineGUI.getTimelineScrollPane());
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }

    // Don't delete, just unused for now because I don't want my disk space being eaten up
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
            System.out.println("MediaEditorGUI autosaveProject(): " + e.getMessage());
            return;
        }

        saveProject(jsonDir.toFile(), archiveDir.toFile(), drillDir.toFile());
        writeSysMsg("Autosaved project to `" + jsonDir + "`.");
    }

    public void saveProject(File path, File archivePath, File drillPath) {
        String relArchive = path.getParentFile().toURI().relativize(archivePath.toURI()).getPath();
        String relDrill = path.getParentFile().toURI().relativize(drillPath.toURI()).getPath();

        ProjectFile pf = new ProjectFile(footballFieldPanel.drill, relArchive, relDrill, timeSync, startDelay, count2RFTrigger);
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

    private long timeBeforeEffect(int index, Effect e, ArrayList<Effect> effects, Long[] timesMS) {
        if (index == 0) {
            return e.getStartTimeMSec() - timesMS[0];
        }
        long lastTrigger = 0;
        for (int i = 0; i < timesMS.length; i++) {
            if (timesMS[i] <= e.getStartTimeMSec()) {
                lastTrigger = timesMS[i];
            } else {
                break;
            }
        }
        if (effects.get(index-1).getStartTimeMSec() > lastTrigger) {
            return e.getStartTimeMSec() - effects.get(index-1).getEndTimeMSec();
        } else {
            return e.getStartTimeMSec() - lastTrigger;
        }
    }

    private long timeAfterEffect(int index, Effect e, ArrayList<Effect> effects, Long[] timesMS) {
        if (index == effects.size()-1) {
            return Long.MAX_VALUE;
        }
        long nextTrigger = 0;
        for (int i = timesMS.length-1; i >= 0; i--) {
            if (timesMS[i] > e.getStartTimeMSec()) {
                nextTrigger = timesMS[i];
            } else {
                break;
            }
        }
        if (effects.get(index+1).getStartTimeMSec() < nextTrigger) {
            if (effects.get(index+1).getStartTimeMSec() > e.getEndTimeMSec()+1) {
                return effects.get(index+1).getStartTimeMSec() - e.getEndTimeMSec();
            } else {
                return Long.MAX_VALUE;
            }
        } else {
            return Long.MAX_VALUE;
        }
    }

    private int getEffectTriggerIndex(Effect e, Long[] timesMS) {
        int r = 0;
        for (int i = 0; i < timesMS.length; i++) {
            if (e.getStartTimeMSec() >= timesMS[i]) {
                r = i;
            } else {
                break;
            }
        }
        return r;
    }

    public void exportPackets(File path) {
        int s = count2RFTrigger.size();
        RFTrigger[] rfTriggerArray = new RFTrigger[s];
        Iterator<RFTrigger> rfIterator = count2RFTrigger.values().iterator();
        int i = 0;
        while (rfIterator.hasNext()) {
            rfTriggerArray[i] = rfIterator.next();
            i++;
        }
        ArrayList<Long> timeMS = new ArrayList<>();
        timeMS.add(rfTriggerArray[0].getTimestampMillis());
        for (i = 1; i < rfTriggerArray.length; i++) {
             for (int j = 0; j < timeMS.size(); j++) {
                 if (rfTriggerArray[i].getTimestampMillis() < timeMS.get(j)) {
                     timeMS.add(j, rfTriggerArray[i].getTimestampMillis());
                     break;
                 } else {
                     if (j == timeMS.size()-1) {
                         timeMS.add(rfTriggerArray[i].getTimestampMillis());
                         break;
                     }
                 }
             }
        }
        Long[] timesMS = new Long[timeMS.size()];
        timesMS = timeMS.toArray(timesMS);
        try {
            BufferedWriter bfw = new BufferedWriter(new FileWriter(path));
            String out = "{\"Devices\": [\n";
            for (int k = 0; k < footballFieldPanel.drill.performers.size(); k++) {
                Performer p = footballFieldPanel.drill.performers.get(k);
                out += "\t{\"Device_id\": " + p.getDeviceId() + ", \n";
                out += "\t\"Pkt_count\": " + p.getEffects().size() + ",\n";
                out += "\t\"Packets\": [\n";
                for (i = 0; i < p.getEffects().size(); i++) {
                    Effect e = p.getEffects().get(i);
                    out += "\t\t{\"Size\": 0,";
                    out += "\"Strip_id\": " + p.getDeviceId() + ", ";
                    out += "\"Set_id\": " + getEffectTriggerIndex(e, timesMS) + ", ";
                    Color startColor = e.getStartColor();
                    out += "\"Start_color\": {" + startColor.getRed() + ", " + startColor.getBlue() + ", " + startColor.getGreen() + "}, ";
                    Color endColor = e.getEndColor();
                    out += "\"End_color\": {" + endColor.getRed() + ", " + endColor.getBlue() + ", " + endColor.getGreen() + "}, ";
                    int flags = 0;
                    if (timeBeforeEffect(i, e, p.getEffects(), timesMS) != 0) {
                        flags += DO_DELAY;
                    }
                    if (timeAfterEffect(i, e, p.getEffects(), timesMS) == Long.MAX_VALUE) {
                        flags += SET_TIMEOUT;
                    }
                    flags += TIME_GRADIENT;
                    if ((flags & DO_DELAY) > 0) {
                        out += "\"Delay\": " + timeBeforeEffect(i, e, p.getEffects(), timesMS) + ",";
                    } else {
                        out += "\"Delay\": 0, ";
                    }
                    out += "\"Duration\": " + (e.getEndTimeMSec() - e.getStartTimeMSec()) + ", ";
                    out += "\"Function\": 0, ";
                    out += "\"Timeout\": 0},\n";
                    if (i < p.getEffects().size() - 1) {
                        bfw.write(out);
                        bfw.flush();
                        out = "";
                    }
                }
                if (k < footballFieldPanel.drill.performers.size() - 1) {
                    out = out.substring(0, out.length() - 2) + "]},\n";
                    bfw.write(out);
                    bfw.flush();
                    out = "";
                }
            }
            out = out.substring(0, out.length() - 2) + "]}\n\t]\n}\n";
            bfw.write(out);
            bfw.flush();
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void writeSysMsg(String msg) {
        clearSysMsg.stop();
        sysMsg.setText(msg);
        clearSysMsg.start();
    }

    private void displayNonModalTip(String message) {
        JWindow tipWindow = new JWindow(frame);
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        contentPane.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton nextButton = new JButton("Next");
        JButton closeButton = new JButton("Close");

        buttonPanel.add(closeButton);
        buttonPanel.add(nextButton);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tipWindow.dispose();
                currentTutorialIndex++;
                if (currentTutorialIndex < tutorialMessages.length) {
                    // Effect Panel
                    if (currentTutorialIndex == 3) {
                        effectViewPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        Timer timer = new Timer(1000, new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                effectViewPanel.setBorder(BorderFactory.createTitledBorder("Effect View"));
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                        displayNonModalTip(tutorialMessages[currentTutorialIndex]);
                    }
                    // Scrub Bar
                    else if (currentTutorialIndex == 2) {
                        scrubBarPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        Timer timer = new Timer(1000, new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if (scrubBarPanel != null) {
                                    mainContentPanel.remove(scrubBarPanel);
                                }
                                scrubBarPanel = scrubBarGUI.getScrubBarPanel();
                                scrubBarPanel.setBorder(BorderFactory.createTitledBorder("Scrub Bar"));
                                scrubBarPanel.setPreferredSize(new Dimension(650, 120));

                                mainContentPanel.add(scrubBarPanel, BorderLayout.SOUTH);

                                mainContentPanel.revalidate();
                                mainContentPanel.repaint();
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                        displayNonModalTip(tutorialMessages[currentTutorialIndex]);
                    }
                    // main Content
                    else if (currentTutorialIndex == 1) {
                        mainContentPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        Timer timer = new Timer(1000, new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                mainContentPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 0));
                            }
                        });
                        timer.setRepeats(false);
                        timer.start();
                        displayNonModalTip(tutorialMessages[currentTutorialIndex]);
                    }
                    //
                    else {
                        displayNonModalTip(tutorialMessages[currentTutorialIndex]);
                    }
                }
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Restore the original border
                tipWindow.dispose();
            }
        });
        tipWindow.setContentPane(contentPane);
        tipWindow.setSize(400, 100);
        tipWindow.setLocation(frame.getLocationOnScreen().x + (frame.getWidth() - tipWindow.getWidth()) / 2,
                              frame.getLocationOnScreen().y + (frame.getHeight() - tipWindow.getHeight()) / 2);
        tipWindow.setVisible(true);
    }

    /*
        tutorial
     */

    public class LimitedDocument extends PlainDocument {

        private final int limit;

        public LimitedDocument(int limit) {
            this.limit = limit;
        }

        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
            if (str == null) return;

            if ((getLength() + str.length()) <= limit) {
                super.insertString(offset, str, attr);
            }
        }
    }

}