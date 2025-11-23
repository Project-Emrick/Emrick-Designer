package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.jthemedetecor.OsThemeDetector;
import com.sun.net.httpserver.HttpServer;
import org.emrick.project.actions.LEDConfig;
import org.emrick.project.audio.AudioPlayer;
import org.emrick.project.effect.*;
import org.emrick.project.serde.*;

import javax.imageio.ImageIO;
import java.awt.font.TextAttribute;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

/**
 * Main class of Emrick Designer.
 * Contains all GUI elements and logic for light show design and Emrick board interaction
 */
public class MediaEditorGUI extends Component implements ImportListener, ScrubBarListener, SyncListener,
        FootballFieldListener, EffectListener, SelectListener, UserAuthListener, RFTriggerListener, RFSignalListener, RequestCompleteListener,
        LEDConfigListener, ReplaceFilesListener, TimelineListener {

    // String definitions
    public static final String FILE_MENU_CONCATENATE = "Concatenate";
    public static final String FILE_MENU_NEW_PROJECT = "New Project";
    public static final String FILE_MENU_OPEN_PROJECT = "Open Project";
    public static final String FILE_MENU_SAVE = "Save Project";
    public static final String FILE_MENU_SAVE_AS = "Save Project As";

    // UI Components of MediaEditorGUI
    private final JFrame frame;
    private WindowListener windowListener;
    private JPanel mainContentPanel;
    private JPanel footballField;
    private FootballFieldPanel footballFieldPanel;
    private FootballFieldBackground footballFieldBackground;
    private LEDConfigurationGUI ledConfigurationGUI;
    private TimelineGUI timelineGUI;
    private EffectGUI effectGUI;
    private SelectionGroupGUI groupsGUI;
    private RFTriggerGUI rfTriggerGUI;
    private FlowViewGUI flowViewGUI;
    private LEDStripViewGUI ledStripViewGUI;
    private ScrubBarGUI scrubBarGUI; // Refers to ScrubBarGUI instance, with functionality
    private JPanel scrubBarPanel; // Refers directly to panel of ScrubBarGUI. Reduces UI refreshing issues.
    private JPanel effectViewPanel;
    private boolean showAllEffects = true; // when true, timeline shows all effects; checkbox toggles behavior
    private JPanel timelinePanel;

    private JSplitPane hSplitPane;
    private JSplitPane vSplitPane;

    // dots
    private final JLabel sysMsg = new JLabel("Welcome to Emrick Designer!", SwingConstants.RIGHT);
    private final Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });
    // JSON serde
    private final Gson gson;
    private final Path userHome = Paths.get(System.getProperty("user.home"), ".emrick");

    // Audio Components
    //  May want to abstract this away into some DrillPlayer class in the future
    public ArrayList<AudioPlayer> audioPlayers;
    public AudioPlayer currentAudioPlayer;
    private boolean canSeekAudio = true;


    // Effect
    private EffectManager effectManager;
    private Effect currentEffect;
    private Effect copiedEffect;
    private EffectList selectedEffectType = EffectList.STATIC_COLOR;
    public final int DEFAULT_FUNCTION = 0x1;
    public final int USE_DURATION = 0x2;
    public final int SET_TIMEOUT = 0x4;
    public final int DO_DELAY = 0x8;
    public final int INSTANT_COLOR = 0x10;
    public final int PROGRAMMING_MODE = 0x20;
    public final int USE_COLORS = 0x40;
    public final int DIRECTION = 0x80;
    public final int LIGHT_BOARD = 0x100;
    public final int CONTINUOUS = 0x200;
    public final int VERIFY = 0x400;
    public final int CHECK_LR = 0x800;

    // RF Trigger
    private HashMap<Integer, RFTrigger> count2RFTrigger;


    // Time keeping
    private TimeManager timeManager;
    private ArrayList<SyncTimeGUI.Pair> timeSync = null;
    private boolean useStartDelay; // If we are at the first count of the first set, useStartDelay = true
    private float startDelay; // Drills might not start immediately, therefore use this. Unit: seconds.
    private float playbackSpeed = 1; // The selected playback speed. For example "0.5", "1.0", "1.5". Use as a multiplier
    private java.util.Timer playbackTimer = null;
    public int currentMovement;

    // Web Server
    private HttpServer server;
    private HttpServer rssiServer;
    private String ssid;
    private String password;
    private int port;
    private int currentID;
    private static int MAX_CONNECTIONS = 50;
    private long lastRun = System.currentTimeMillis();
    private int token;
    private Color verificationColor;
    private Timer noRequestTimer;
    private HashSet<Integer> requestIDs;
    private JMenuItem runWebServer;
    private JMenuItem runLightBoardWebServer;
    private JMenuItem stopWebServer;
    private JMenuItem runRSSILogger;
    private JMenuItem stopRSSILogger;
    private ProgrammingTracker programmingTracker;
    private JProgressBar programmingProgressBar;
    private boolean lightBoardMode;
    JLabel programmingProgressLabel = new JLabel();

    // Flow viewer
    private JMenuItem runShowItem;
    private JMenuItem flowViewerItem;
    private JMenuItem lightBoardFlowViewerItem;
    private JMenuItem stopShowItem;
    private boolean isLightBoardMode;

    private JCheckBoxMenuItem showIndividualView;

    // Project info
    private ArrayList<File> archivePaths = null;
    private File emrickPath = null;
    private File csvFile;
    
    private HardwareStatusIndicator hardwareStatusIndicator;
    JFrame webServerFrame;
    
    

    
    /**
     * Main method of Emrick Designer.
     *
     * @param args - Only used when opening the application via an associated file type rather than an executable
     */
    public static void main(String[] args) {
        // Process file argument
        final String file = args.length != 0 ? args[0] : "";
        
        // Set up theme detection and initial look and feel
        setupLookAndFeel();
        
        // Run this program on the Event Dispatch Thread (EDT)
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() { new MediaEditorGUI(file); }
        });
    }

    /**
     * Constructor for MediaEditorGUI
     *
     * @param file - Used when starting the application via opening a file via an associated type.
     *             Otherwise, this can be left as an empty string.
     */
    public MediaEditorGUI(String file) {
        // serde setup
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Color.class, new ColorAdapter());
        builder.registerTypeAdapter(Point2D.class, new Point2DAdapter());
        builder.registerTypeAdapter(SyncTimeGUI.Pair.class, new PairAdapter());
        builder.registerTypeAdapter(Duration.class, new DurationAdapter());
        builder.registerTypeAdapter(GeneratedEffect.class, new GeneratedEffectAdapter());
        builder.registerTypeAdapter(JButton.class, new JButtonAdapter());
        builder.serializeNulls();
        gson = builder.create();

        // Autosave and system message things
        clearSysMsg.setRepeats(false);
        clearSysMsg.start();

        // Change Font Size for Menu and MenuIem
        Font f = new Font("FlatLaf.style", Font.PLAIN, 14);
        UIManager.put("Menu.font", f);
        UIManager.put("MenuItem.font", f);
        UIManager.put("CheckBoxMenuItem.font", f);
        UIManager.put("RadioButtonMenuItem.font", f);

        // Main frame
        frame = new JFrame("Emrick Designer");
        Image icon = Toolkit.getDefaultToolkit().getImage(PathConverter.pathConverter("res/images/icon.png", true));
        frame.setIconImage(icon);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        frame.setSize((int) (screenSize.width * 0.75), (int) (screenSize.height * 0.75));

        windowListener = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (server != null) {
                    stopServer();
                    webServerFrame.dispose();
                }
                if (playbackTimer != null) {
                    playbackTimer.cancel();
                    playbackTimer.purge();
                    playbackTimer = null;
                    if (audioPlayers.get(currentMovement - 1) != null) {
                        audioPlayers.get(currentMovement - 1).pauseAudio();
                    }
                }
                
                // Stop hardware scanning
                if (hardwareStatusIndicator != null) {
                    hardwareStatusIndicator.stopScanning();
                }
                
                if (archivePaths != null) {
                    if (effectManager != null && !effectManager.getUndoStack().isEmpty()) {
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
                }
                File showDataDir = new File(PathConverter.pathConverter("show_data/", false));
                showDataDir.mkdirs();
                File[] cleanFiles = showDataDir.listFiles();
                for (File f : cleanFiles) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
                frame.dispose();
                super.windowClosing(e);
                System.exit(0);
            }
        };
        frame.addWindowListener(windowListener);

        currentID = MAX_CONNECTIONS;

        // Make sure user dir exists
        File userDir = new File(PathConverter.pathConverter("", false));
        userDir.mkdirs();

        // Delete leftover files from show_data/
        File showDataDir = new File(PathConverter.pathConverter("show_data/", false));
        if (showDataDir.exists()) {
            showDataDir.mkdirs();
            if (showDataDir.isDirectory()) {
                if (showDataDir.listFiles().length > 0) {
                    ArrayList<File> files = new ArrayList<>(Arrays.stream(showDataDir.listFiles()).toList());
                    int i = 0;
                    File file1;
                    while (i < files.size()) {
                        file1 = files.get(i);
                        if (file1.isDirectory() && file1.listFiles().length > 0) {
                            File[] files1 = file1.listFiles();
                            for (File f1 : files1) {
                                files.add(i, f1);
                            }
                        } else {
                            file1.delete();
                            i++;
                        }
                    }
                }
            }
        }

        // Delete leftover files from packet export
        File tmpDir = new File(PathConverter.pathConverter("tmp/", false));
        if (tmpDir.exists()) {
            tmpDir.mkdirs();
            deleteDirectory(tmpDir);
        }
        File tmppkt = new File(PathConverter.pathConverter("tempPkt.pkt", false));
        if (tmppkt.exists()) {
            tmppkt.delete();
        }

        noRequestTimer = new Timer(10000, e -> {
            onRequestComplete(-1);
        });

        if (!file.equals("")) {
            if (file.endsWith(".emrick")) {
                createAndShowGUI();
                loadProject(new File(file));
            } else {
                createAndShowGUI();
                runWebServer.setEnabled(false);
                runLightBoardWebServer.setEnabled(false);
                stopWebServer.setEnabled(true);
                runServer(file, false);
            }
        } else {
            createAndShowGUI();
        }


    }

    /**
     * Calculates and returns the frame length in milliseconds
     *
     * @return - Time in milliseconds the timer should wait between frames
     */
    private long getPlaybackTimerTimeByCounts() {
        float setSyncDuration = timeSync.get(scrubBarGUI.getCurrentSetIndex()).getValue();
        float setDuration = scrubBarGUI.getCurrSetDuration();
        return Math.round(setSyncDuration / setDuration * 1000 / playbackSpeed);
    }

    /**
     * Sets up the look and feel of the application based on the user's OS theme.
     */
    private static void setupLookAndFeel() {
        FlatLaf.registerCustomDefaultsSource("org.emrick.project.ui");
        FlatDarkLaf.setup();
        FlatLightLaf.setup();
        OsThemeDetector.getDetector().registerListener(isDark -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (isDark) {
                        UIManager.setLookAndFeel(new FlatDarkLaf());
                    } else {
                        UIManager.setLookAndFeel(new FlatLightLaf());
                    }
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        // Force a theme detection update - using proper method instead of notify()
        SwingUtilities.invokeLater(() -> {
            // This will trigger the theme listener we registered above
            boolean isDarkMode = OsThemeDetector.getDetector().isDark();
            try {
                UIManager.setLookAndFeel(isDarkMode ? new FlatDarkLaf() : new FlatLightLaf());
            } catch (UnsupportedLookAndFeelException ex) {
                System.err.println("Failed to update look and feel: " + ex.getMessage());
            }
        });
    }   

    /**
     * Builds all major GUI elements and adds them to the main frame.
     * This method should be called on startup and on project loading when another project is already loaded.
     */
    private void createAndShowGUI() {
        RFTrigger.rfTriggerListener = this;
        Effect.effectListener = this;

        if (archivePaths != null) {
            frame.remove(mainContentPanel);
            frame.remove(hSplitPane);
            frame.remove(vSplitPane);
            frame.revalidate();
            frame.repaint();
        }

        // playback timer

        ////////////////////////// Panels //////////////////////////

        JMenuBar menuBar = new JMenuBar();

        // Field
        footballFieldPanel = new FootballFieldPanel(this, null);
        footballFieldPanel.setOpaque(false);

        JScrollPane fieldScrollPane = new JScrollPane(footballFieldPanel);
        fieldScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fieldScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        footballFieldBackground = new FootballFieldBackground(this);

        try {
            BufferedImage surface = ImageIO.read(new File(PathConverter.pathConverter("res/images/field/Surface.png", true)));
            BufferedImage cover = ImageIO.read(new File(PathConverter.pathConverter("res/images/field/Cover.png", true)));
            footballFieldBackground.setSurfaceImage(surface);
            footballFieldBackground.setFloorCoverImage(cover);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        footballField = new JPanel();

        flowViewGUI = new FlowViewGUI(new HashMap<>(), this, footballFieldPanel.drill.sets);

        // Scrub Bar
        scrubBarGUI = new ScrubBarGUI(frame, this, this, footballFieldPanel, audioPlayers);

        // Scrub bar cursor starts on first count of drill by default
        useStartDelay = true;

        mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BorderLayout());

        footballField.setLayout(new OverlayLayout(footballField));
        footballField.add(footballFieldPanel, BorderLayout.CENTER);
        footballField.add(footballFieldBackground, BorderLayout.CENTER);
        mainContentPanel.add(footballField, BorderLayout.CENTER);

        // Scrub Bar Panel
        buildScrubBarPanel();


        // Timeline panel
        timelinePanel = new JPanel(new BorderLayout());
        timelinePanel.setBorder(BorderFactory.createTitledBorder("Timeline"));

        // Effect View panel
        effectGUI = new EffectGUI(EffectGUI.noProjectSyncMsg);
        groupsGUI = new SelectionGroupGUI(this);
        effectViewPanel = new JPanel();
        effectViewPanel.setLayout(new BorderLayout());
        effectViewPanel.setBorder(BorderFactory.createTitledBorder("Effect View"));
        effectViewPanel.add(effectGUI.getEffectPanel());

        hSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainContentPanel, effectViewPanel);
        hSplitPane.setOneTouchExpandable(true);
        hSplitPane.setDividerLocation(0.8);
        hSplitPane.setDividerSize(10);
        hSplitPane.setResizeWeight(0.8);
        vSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hSplitPane, timelinePanel);
        vSplitPane.setOneTouchExpandable(true);
        vSplitPane.setDividerLocation(0.6);
        vSplitPane.setDividerSize(10);
        vSplitPane.setResizeWeight(0.6);

        vSplitPane.setPreferredSize(frame.getSize());
        frame.add(vSplitPane);

        ////////////////////////// Menu //////////////////////////

        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        // Import Pyware Project
        JMenuItem importItem = new JMenuItem(FILE_MENU_NEW_PROJECT);
        importItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        fileMenu.add(importItem);
        importItem.addActionListener(e -> {
            writeSysMsg("New Project...");
            new SelectFileGUI(frame, this);
        });

        JMenuItem openItem = new JMenuItem(FILE_MENU_OPEN_PROJECT);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        fileMenu.add(openItem);
        openItem.addActionListener(e -> {
            openProjectDialog();
        });

        fileMenu.addSeparator();

        // Save Emrick Project
        JMenuItem saveItem = new JMenuItem(FILE_MENU_SAVE);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        fileMenu.add(saveItem);
        saveItem.addActionListener(e -> {
            saveProjectDialog();
        });
        // Save As Emrick Project
        JMenuItem saveAsItem = new JMenuItem(FILE_MENU_SAVE_AS);
        fileMenu.add(saveAsItem);
        saveAsItem.addActionListener(e -> {
            saveAsProjectDialog();
        });

        fileMenu.addSeparator();

        //Concatenate Projects (the current project will be appended to) and a copy of the old will be made
        JMenuItem concatenateItem = new JMenuItem(FILE_MENU_CONCATENATE);
        fileMenu.add(concatenateItem);
        concatenateItem.addActionListener(e -> {
            concatenateDialog();
        });

        fileMenu.addSeparator();

        // Modify Drill/Audio
        JMenuItem modifyProject = new JMenuItem("Modify Drill/Audio");
        modifyProject.addActionListener(e -> {
            ReplaceProjectFilesGUI replaceProjectFilesGUI = new ReplaceProjectFilesGUI(frame, this);
            replaceProjectFilesGUI.setVisible(true);
        });
        fileMenu.add(modifyProject);
        fileMenu.addSeparator();

        // Export Emrick Packets
        JMenuItem exportItem = new JMenuItem("Export Emrick Packets File");
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        fileMenu.add(exportItem);
        exportItem.addActionListener(e -> {
            writeSysMsg("Exporting packets...");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Project");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Emrick Project Packets (*.pkt)",
                    "pkt"));
            if (fileChooser.showSaveDialog(fileMenu) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".pkt")) {
                    path += ".pkt";
                }
                writeSysMsg("Exporting file `" + path + "`.");
                exportPackets(new File(path));
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
                if (!selectedFile.getAbsolutePath().endsWith(".csv")) {
                    File tmp = selectedFile;
                    selectedFile = new File(tmp.getAbsolutePath() + ".csv");
                }
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
                parseCsvFileForPerformerDeviceIDs(csvFile);
                footballFieldPanel.repaint();
            }
        });

        fileMenu.addSeparator();

        // Edit Configuration
        JMenuItem editConfigItem = new JMenuItem("Edit LED Configuration");
        fileMenu.add(editConfigItem);
        editConfigItem.addActionListener(e -> {
            ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);
            if (footballField.isShowing()) {
                mainContentPanel.remove(footballField);
            } else if (flowViewGUI.isShowing()) {
                removeFlowViewer();
            } else if (ledStripViewGUI.isShowing()) {
                showIndividualView.setState(false);
                mainContentPanel.remove(ledStripViewGUI);
            }
            mainContentPanel.add(ledConfigurationGUI);
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
        });

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);

        JMenuItem undoColorsItem = new JMenuItem("Undo");
        undoColorsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoColorsItem.addActionListener(e -> {
            //undoColorChange();
            if (ledConfigurationGUI.isShowing()) {
                ledConfigurationGUI.undo();
            } else {
                effectManager.undo();
                footballFieldPanel.repaint();
                updateTimelinePanel();
                updateEffectViewPanel(selectedEffectType, null);
            }
        });
        editMenu.add(undoColorsItem);

        JMenuItem redoColorsItem = new JMenuItem("Redo");
        redoColorsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        redoColorsItem.addActionListener(e -> {
            if (ledConfigurationGUI.isShowing()) {
                ledConfigurationGUI.redo();
            } else {
                effectManager.redo();
                footballFieldPanel.repaint();
                updateTimelinePanel();
                updateEffectViewPanel(selectedEffectType, null);
            }
        });
        editMenu.add(redoColorsItem);

        editMenu.addSeparator();

        JMenuItem removeEffectsForAll = new JMenuItem("Reset All Performers");
        editMenu.add(removeEffectsForAll);
        removeEffectsForAll.addActionListener(e -> {
            if (this.effectManager == null) return;
            this.effectManager.removeAllEffectsFromAllLEDStrips();

            footballFieldPanel.repaint();
            updateTimelinePanel();
            updateEffectViewPanel(selectedEffectType, null);
        });

        // Remove effects for selected
        JMenuItem removeEffectsForSelected = new JMenuItem("Reset Selected Performers");
        removeEffectsForSelected.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                Toolkit.getDefaultToolkit()
                        .getMenuShortcutKeyMaskEx()));
        editMenu.add(removeEffectsForSelected);
        removeEffectsForSelected.addActionListener(e -> {
            if (this.effectManager == null) return;
            this.effectManager.removeAllEffectsFromSelectedLEDStrips();
            this.footballFieldPanel.repaint();
            updateTimelinePanel();
            updateEffectViewPanel(selectedEffectType, null);
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
            boolean success = this.effectManager.addEffectToSelectedLEDStrips(this.copiedEffect);
            if (success) updateEffectViewPanel(this.copiedEffect.getEffectType(), this.copiedEffect);
            this.footballFieldPanel.repaint();
        });
        editMenu.add(pasteCopiedEffect);

        // Select Menu
        JMenu selectMenu = new JMenu("Select");
        menuBar.add(selectMenu);

        JMenuItem selectByLabel = new JMenuItem("Select By Label");
        selectByLabel.addActionListener(e -> {
            if (archivePaths == null) {
                System.out.println("no project loaded");
                return;
            }

            HashSet<Integer> labels = new HashSet<>();
            HashSet<String> symbols = new HashSet<>();
            for (Performer p : footballFieldPanel.drill.performers) {
                labels.add(p.getLabel());
                symbols.add(p.getSymbol());
            }
            FilterSelect filterSelect = new FilterSelect(frame, this, labels, symbols);
            filterSelect.show();
        });
        selectMenu.add(selectByLabel);

        JMenuItem boxSelect = new JMenuItem("Box Selection");
        JMenuItem lassoSelect = new JMenuItem("Lasso Selection");
        boxSelect.addActionListener(e -> {
            boxSelect.setEnabled(false);
            lassoSelect.setEnabled(true);
            footballFieldPanel.selectionMethod = FootballFieldPanel.SelectionMethod.BOX;
        });
        boxSelect.setEnabled(false);
        selectMenu.add(boxSelect);

        lassoSelect.addActionListener(e -> {
            lassoSelect.setEnabled(false);
            boxSelect.setEnabled(true);
            footballFieldPanel.selectionMethod = FootballFieldPanel.SelectionMethod.LASSO;
        });
        selectMenu.add(lassoSelect);
        selectMenu.addSeparator();

        JMenuItem groups = new JMenuItem("Show Saved Groups");
        JMenuItem hideGroups = new JMenuItem("Hide Saved Groups");
        groups.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        hideGroups.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        groups.addActionListener(e -> {
            selectedEffectType = EffectList.SHOW_GROUPS;
            updateEffectViewPanel(selectedEffectType, null);
            hideGroups.setEnabled(true);
            groups.setEnabled(false);
        });
        selectMenu.add(groups);
        hideGroups.addActionListener(e -> {
            selectedEffectType = EffectList.HIDE_GROUPS;
            updateEffectViewPanel(selectedEffectType, null);
            groups.setEnabled(true);
            hideGroups.setEnabled(false);
        });
        hideGroups.setEnabled(false);
        selectMenu.add(hideGroups);
        selectMenu.addSeparator();
        JCheckBoxMenuItem toggleSelectAllLEDs = new JCheckBoxMenuItem("Select All LEDs");
        toggleSelectAllLEDs.setState(true);
        toggleSelectAllLEDs.addActionListener(e -> {
            footballFieldPanel.setSelectAllLEDs(!footballFieldPanel.isSelectAllLEDs());
        });
        selectMenu.add(toggleSelectAllLEDs);

        // View menu
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);

        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("Dark Mode");
        darkModeItem.setState(OsThemeDetector.getDetector().isDark());
        darkModeItem.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (darkModeItem.isSelected()) {
                        UIManager.setLookAndFeel(new FlatDarkLaf());
                    } else {
                        UIManager.setLookAndFeel(new FlatLightLaf());
                    }
                    SwingUtilities.updateComponentTreeUI(frame);
                    // Rebuild timeline widgets so any hard-coded backgrounds are recreated under the new LAF
                    if (timelineGUI != null) {
                        updateTimelinePanel();
                    }
                    // Ensure effect panel widgets and popups update
                    if (effectViewPanel != null) {
                        effectViewPanel.revalidate();
                        effectViewPanel.repaint();
                    }
                    if (footballFieldPanel != null) footballFieldPanel.repaint();
                } catch (UnsupportedLookAndFeelException ex) {
                    System.err.println("Failed to update look and feel: " + ex.getMessage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
        viewMenu.add(darkModeItem);

        JCheckBoxMenuItem toggleFloorCoverImage = new JCheckBoxMenuItem("Show Floor Cover Image");
        toggleFloorCoverImage.setState(true);
        toggleFloorCoverImage.addActionListener(e -> {
            footballFieldBackground.setShowFloorCoverImage(!footballFieldBackground.isShowFloorCoverImage());
            footballFieldBackground.justResized = true;
            footballFieldBackground.repaint();
        });
        viewMenu.add(toggleFloorCoverImage);
        JCheckBoxMenuItem toggleSurfaceImage = new JCheckBoxMenuItem("Show Surface Image");
        toggleSurfaceImage.setState(true);
        toggleSurfaceImage.addActionListener(e -> {
            footballFieldBackground.setShowSurfaceImage(!footballFieldBackground.isShowSurfaceImage());
            footballFieldBackground.justResized = true;
            footballFieldBackground.repaint();
        });
        viewMenu.add(toggleSurfaceImage);
        JCheckBoxMenuItem toggleShowLabels = new JCheckBoxMenuItem("Show Drill IDs");
        toggleShowLabels.setState(false);
        toggleShowLabels.addActionListener(e -> {
            footballFieldPanel.setShowLabels(!footballFieldPanel.isShowLabels());
            footballFieldPanel.repaint();
        });
        toggleShowLabels.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        viewMenu.add(toggleShowLabels);

        viewMenu.addSeparator();

        showIndividualView = new JCheckBoxMenuItem("Show Individual View");
        showIndividualView.setSelected(false);
        showIndividualView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        showIndividualView.addActionListener(e -> {
            if (showIndividualView.isSelected()) {
                ArrayList<LEDStrip> ledStrips = new ArrayList<>(footballFieldPanel.selectedLEDStrips);
                ledStripViewGUI = new LEDStripViewGUI(ledStrips, effectManager);
                ledStripViewGUI.setCurrentMS(footballFieldPanel.currentMS);
                ledStripViewGUI.setCurrentSet(footballFieldPanel.getCurrentSet());
                if (footballField.isShowing()) {
                    mainContentPanel.remove(footballField);
                } else if (ledConfigurationGUI.isShowing()) {
                    mainContentPanel.remove(ledConfigurationGUI);
                } else if (flowViewGUI.isShowing()) {
                    removeFlowViewer();
                }
                mainContentPanel.add(ledStripViewGUI);
                mainContentPanel.revalidate();
                mainContentPanel.repaint();
            } else {
                mainContentPanel.remove(ledStripViewGUI);
                mainContentPanel.add(footballField);
                mainContentPanel.revalidate();
                mainContentPanel.repaint();
            }
        });
        viewMenu.add(showIndividualView);


        // Run menu
        JMenu runMenu = new JMenu("Run");
        menuBar.add(runMenu);
        runShowItem = new JMenuItem("Run Show Linked to Viewport");
        runMenu.add(runShowItem);
        flowViewerItem = new JMenuItem("Run Show via Flow View");
        runMenu.add(flowViewerItem);
        lightBoardFlowViewerItem = new JMenuItem("Run Parade Mode via View");
        runMenu.add(lightBoardFlowViewerItem);
        stopShowItem = new JMenuItem("Stop show");
        stopShowItem.setEnabled(false);
        runMenu.add(stopShowItem);
        runMenu.addSeparator();
        runWebServer = new JMenuItem("Run Web Server");
        runLightBoardWebServer = new JMenuItem("Run Parade Mode Web Server");
        stopWebServer = new JMenuItem("Stop Web Server");
        runMenu.add(runWebServer);
        runMenu.add(runLightBoardWebServer);
        runMenu.add(stopWebServer);
        runMenu.addSeparator();
        runRSSILogger = new JMenuItem("Run RSSI Logger");
        stopRSSILogger = new JMenuItem(("Stop RSSI Logger"));
        runMenu.add(runRSSILogger);
        runMenu.add(stopRSSILogger);

        // Update Visual Status Of Server Menu Items
        if (server == null) {
            stopWebServer.setEnabled(false);
        } else {
            runWebServer.setEnabled(false);
            runLightBoardWebServer.setEnabled(false);
        }

        if (rssiServer == null) {
            stopRSSILogger.setEnabled(false);
        } else {
            runRSSILogger.setEnabled(false);
        }

        // Option Action Listeners
        runWebServer.addActionListener(e -> {
            runWebServer.setEnabled(false);
            runLightBoardWebServer.setEnabled(false);
            stopWebServer.setEnabled(true);
            runServer("", false);
        });
        runLightBoardWebServer.addActionListener(e -> {
            runWebServer.setEnabled(false);
            runLightBoardWebServer.setEnabled(false);
            stopWebServer.setEnabled(true);
            runServer("", true);
        });
        stopWebServer.addActionListener(e -> {
            stopServer();
            webServerFrame.dispose();
        });
        stopShowItem.addActionListener(e -> {
            if (flowViewGUI != null) {
                mainContentPanel.remove(flowViewGUI);
                mainContentPanel.add(footballField);
                mainContentPanel.revalidate();
                mainContentPanel.repaint();
            }
            /* Adding a Signal to Turn Status LED back on */

            SerialTransmitter st = comPortPrompt("Transmitter");
            st.writeToSerialPort("h");

            st = null;
            stopShowItem.setEnabled(false);
            runShowItem.setEnabled(true);
            flowViewerItem.setEnabled(true);
            lightBoardFlowViewerItem.setEnabled(true);
        });
        flowViewerItem.addActionListener(e -> {
            if (count2RFTrigger == null) {
                JOptionPane.showMessageDialog(null, "There is no project currently open. Please open a project file to run show.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            isLightBoardMode = false;
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) {return;}

            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);

            flowViewGUI = new FlowViewGUI(count2RFTrigger, this, footballFieldPanel.drill.sets);

            if (footballField.isShowing()) {
                mainContentPanel.remove(footballField);
            } else if (ledStripViewGUI.isShowing()) {
                showIndividualView.setState(false);
                mainContentPanel.remove(ledStripViewGUI);
            } else if (ledConfigurationGUI.isShowing()) {
                mainContentPanel.remove(ledConfigurationGUI);
            }
            mainContentPanel.add(flowViewGUI);
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
        });

        lightBoardFlowViewerItem.addActionListener(e -> {
            if (count2RFTrigger == null) {
                JOptionPane.showMessageDialog(null, "There is no project currently open. Please open a project file to run show.",
                                               "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            isLightBoardMode = true;

            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
            flowViewGUI = new FlowViewGUI(count2RFTrigger, this, footballFieldPanel.drill.sets);
            if (footballField.isShowing()) {
                mainContentPanel.remove(footballField);
            } else if (ledStripViewGUI.isShowing()) {
                showIndividualView.setState(false);
                mainContentPanel.remove(ledStripViewGUI);
            } else if (ledConfigurationGUI.isShowing()) {
                mainContentPanel.remove(ledConfigurationGUI);
            }
            mainContentPanel.add(flowViewGUI);
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
        });

        runShowItem.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            footballFieldPanel.addSetToField(footballFieldPanel.drill.sets.get(0));
            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
        });

        // Marker
        runRSSILogger.addActionListener(e -> {
            runRSSILogger.setEnabled(false);
            stopRSSILogger.setEnabled(true);
            runRSSIServer();
        });

        // Marker
        stopRSSILogger.addActionListener(e -> {
            runRSSILogger.setEnabled(true);
            stopRSSILogger.setEnabled(false);
        });

        /* Verify Menu */
        JMenu verifyMenu = new JMenu("Verify");
        menuBar.add(verifyMenu);
        JMenuItem verifyShowItem = new JMenuItem("Verify Show");
        verifyMenu.add(verifyShowItem);
        JMenuItem verifyLightBoardItem = new JMenuItem("Verify Light Board");
        verifyMenu.add(verifyLightBoardItem);
        verifyMenu.addSeparator();

        JMenuItem checkColor = new JMenuItem("Check Color");
        verifyMenu.add(checkColor);

        /* Action Listeners For Buttons */
        // Verify Show
        verifyShowItem.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;
            // ask for token
            String token = JOptionPane.showInputDialog(null, "Enter the show token:",
                    "Show Token Input", JOptionPane.QUESTION_MESSAGE);
            if (token == null) return;
            st.writeToSerialPort("v" + token);
        });

        // Verify Light Board
        verifyLightBoardItem.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("w");
        });

        /* Check Color */
        checkColor.addActionListener(e -> {
            /* Check for Board Receiver Type */
            SerialTransmitter st = comPortPrompt("Receiver");
            if (st == null) return;

            /* Add the java color wheel */
            JColorChooser colorChooser = new JColorChooser(Color.WHITE);

            // Create custom dialog that stays open
            JDialog dialog = new JDialog(frame, "Color Check - Select and Send Colors", false);
            dialog.setLayout(new BorderLayout());

            // Create button panel with Send and Close buttons
            JButton sendButton = new JButton("Send to Lights");
            JButton closeButton = new JButton("Close");
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(sendButton);
            buttonPanel.add(closeButton);

            // Add components to dialog
            dialog.add(colorChooser, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // Configure dialog properties
            dialog.setSize(650, 450);
            dialog.setLocationRelativeTo(frame);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // Add button actions
            sendButton.addActionListener(ev -> {
                Color selectedColor = colorChooser.getColor();
                if (st == null) return;

                st.writeColorCheck(selectedColor);

                // Visual feedback
                sendButton.setText("Sent! Click to Send Again");
                sendButton.setBackground(new Color(220, 255, 220));
                Timer timer = new Timer(750, event -> {
                    sendButton.setText("Send to Lights");
                    sendButton.setBackground(null);
                });
                timer.setRepeats(false);
                timer.start();
            });

            closeButton.addActionListener(ev -> dialog.dispose());

            // Show the dialog
            dialog.setVisible(true);
        });

        /* Hardware Menu */
        JMenu hardwareMenu = new JMenu("Hardware");
        menuBar.add(hardwareMenu);
        JMenuItem batteryCheck = new JMenuItem("Battery Check");
        hardwareMenu.add(batteryCheck);
        JMenuItem chargingCheck = new JMenuItem("Charging Check");
        hardwareMenu.add(chargingCheck);
        JMenuItem wirelessCheck = new JMenuItem("Wireless Check");
        hardwareMenu.add(wirelessCheck);
        hardwareMenu.addSeparator();

        JMenuItem storageMode = new JMenuItem("Storage Mode");
        hardwareMenu.add(storageMode);
        JMenuItem setMassIdleItem = new JMenuItem("Mass Set Idle");
        hardwareMenu.add(setMassIdleItem);
        JMenuItem massReset = new JMenuItem("Mass Reset");
        hardwareMenu.add(massReset);
        JMenuItem massSleep = new JMenuItem("Mass Sleep");
        hardwareMenu.add(massSleep);
        hardwareMenu.addSeparator();

        JMenuItem modifyBoardItem = new JMenuItem("Modify Board");
        hardwareMenu.add(modifyBoardItem);
        JMenuItem wiredProgramming = new JMenuItem("PIO Wired Show Programming (Old)");
        hardwareMenu.add(wiredProgramming);
        JMenuItem realWiredProgramming = new JMenuItem("Wired Show Programming");
        hardwareMenu.add(realWiredProgramming);
        JMenuItem resetRSSIItem = new JMenuItem("Reset RSSI Log");
        hardwareMenu.add(resetRSSIItem);

        /* Action Listeners For Buttons */
        // Battery Check
        batteryCheck.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("o");
        });

        // Charging Check
        chargingCheck.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("j");
        });

        // Wireless Check
        wirelessCheck.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("c");
        });

        // Storage Mode
        storageMode.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("d");
        });

        // Mass Idle
        setMassIdleItem.addActionListener(e -> {
            // Same signal as stopShowItem
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("h");
        });

        // Mass Reset
        massReset.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("r");
        });

        // Mass Sleep
        massSleep.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            int response = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to put all devices to sleep?",
                    "Confirm Mass Sleep",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (response == JOptionPane.YES_OPTION) {
                st.writeToSerialPort("e");
                JOptionPane.showMessageDialog(
                        null,
                        "Sleep command sent successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });


        // Modify Board
        modifyBoardItem.addActionListener(e -> {
            try {
                SerialTransmitter st = comPortPrompt("Receiver");
                if (!st.getType().equals("Receiver")) {
                    throw new IllegalStateException("Not a receiver");
                }
            } catch (IllegalStateException er) {
                JOptionPane.showMessageDialog(
                        null,
                        "Transmitter Detected, Please plug in a receiver.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            } catch (Exception err) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please plug a board in before proceeding.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            /* Create st Object for Later Handling */
            SerialTransmitter st = comPortPrompt("Receiver");

            // Marker
            if (archivePaths == null) { // No Project Open
                JTextField boardIDField = new JTextField();
                JCheckBox boardIDEnable = new JCheckBox("Write new Board ID");
                boardIDEnable.setSelected(true);
                JTextField ledCountField = new JTextField();
                JCheckBox enableLedCount = new JCheckBox("Write new LED Count");
                enableLedCount.setSelected(true);

                Object[] inputs = {
                        new JLabel("Board ID: "), boardIDField, boardIDEnable,
                        new JLabel("LED Count: "), ledCountField, enableLedCount
                };

                int option = JOptionPane.showConfirmDialog(null, inputs, "Enter board parameters:", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    if (boardIDEnable.isSelected()) {
                        try {
                            int id = Integer.parseInt(boardIDField.getText());
                            String position = "";
                            if (!footballFieldPanel.drill.ledStrips.isEmpty()) {
                                LEDStrip ledStrip = footballFieldPanel.drill.ledStrips.get(id);
                                position = ledStrip.getLedConfig().getLabel();
                            }


                            st.writeBoardID(boardIDField.getText(), position);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Board ID Error. Please try again. " + ex.getMessage(),
                                    "Input Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    if (enableLedCount.isSelected()) {
                        st.writeLEDCount(ledCountField.getText());
                    }
                }
            } else { // Project Open
                // Set csv File path
                File showDatapath = new File(PathConverter.pathConverter("show_data", false));

                // Search + Set csvFile
                File[] csvFiles = showDatapath.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
                assert csvFiles != null;
                csvFile = csvFiles[0];

                if (csvFile != null) { // Ensure there is a csvFile in the correct location
                    // Have the user input the physical box label
                    JTextField boardLabelField = new JTextField();
                    Object[] inputs = {
                            new JLabel("Board Label: "), boardLabelField
                    };

                    int option = JOptionPane.showConfirmDialog(null, inputs, "Enter board parameters:", JOptionPane.OK_CANCEL_OPTION);
                    if (option == JOptionPane.OK_OPTION) {
                        // Parse input in
                        String boardLabel = boardLabelField.getText().toUpperCase();

                        // Ensure there is an input
                        if (boardLabel == null || boardLabel.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(null, "No label entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        }

                        // Search csv file
                        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                            String line;
                            boolean found = false;

                            String boardID = "";
                            String ledCount = "";

                            while ((line = br.readLine()) != null) {
                                String[] tokens = line.split(",");

                                for (int i = 0; i < tokens.length; i++) {
                                    if (tokens[i].equalsIgnoreCase(boardLabel)) {
                                        boardID = (i > 0) ? tokens[i - 1].trim() : null;
                                        ledCount = (i < tokens.length - 1) ? tokens[i + 1].trim() : null;

                                        if (boardID == null || ledCount == null) {
                                            JOptionPane.showMessageDialog(null, "CSV format is invalid around label: " + boardLabel,
                                                    "Parsing Error", JOptionPane.ERROR_MESSAGE);
                                            return;
                                        }

                                        // Found variables
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) break;
                            }

                            // Write BoardID and ledCount
                            // Board ID
                            String position = "";
                            if (!footballFieldPanel.drill.ledStrips.isEmpty()) {
                                LEDStrip ledStrip = footballFieldPanel.drill.ledStrips.get(Integer.parseInt(boardID));
                                position = ledStrip.getLedConfig().getLabel();
                            }

                            st.writeBoardID(boardID, position);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }

                            // ledCount
                            st.writeLEDCount(ledCount);

                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Error reading CSV: " + ex.getMessage(),
                                    "File Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    System.out.println("CSV File is Null");
                }
            }
        });

        // Wired Show Programming
        wiredProgramming.addActionListener(j -> {
            /* Check for Board Receiver Type */
            try {
                SerialTransmitter st = comPortPrompt("Receiver");
                if (!st.getType().equals("Receiver")) {
                    throw new IllegalStateException("Not a receiver");
                }
            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Transmitter Detected, Please plug in a receiver.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please plug a board in before proceeding.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            /* Check for Windows OS */
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                JOptionPane.showMessageDialog(null,
                        "PlatformIO check is only supported on Windows at this time.",
                        "Unsupported OS",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            /* Check for Platform.io Default Location */
            if (!PlatformIOFunction.verifyInstallation()) {
                JOptionPane.showMessageDialog(null,
                        "PlatformIO not found or an error occurred. Please install PlatformIO.",
                        "PlatformIO Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            /* Create properties and config file */
            Properties props = new Properties();
            File configFile = new File(System.getProperty("user.home"), ".board_config.properties");

            /* Create text fields with persistent data */
            JTextField pathToDataFolderField = new JTextField();
            JTextField pathToExtractedPacketsField = new JTextField();
            JTextField pathToCSVFileField = new JTextField();
            JTextField showTokenField = new JTextField();
            JTextField verificationColorField = new JTextField();
            JTextField marcherLabelField = new JTextField(); // Removed LED count field

            /* Load saved properties */
            if (configFile.exists()) {
                try (FileInputStream in = new FileInputStream(configFile)) {
                    props.load(in);
                    pathToDataFolderField.setText(props.getProperty("data.dir", ""));
                    pathToExtractedPacketsField.setText(props.getProperty("packets.dir", ""));
                    pathToCSVFileField.setText(props.getProperty("csv.file", ""));
                    showTokenField.setText(props.getProperty("show.token", ""));
                    verificationColorField.setText(props.getProperty("verification.color", ""));
                    marcherLabelField.setText(props.getProperty("marcher.label", ""));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                            "Error loading settings: " + e.getMessage(),
                            "Config Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

            Object[] inputs = {
                    new JLabel("Path to Data Directory: "), pathToDataFolderField,
                    new JLabel("Path to Packets Directory: "), pathToExtractedPacketsField,
                    new JLabel("Path to .csv File: "), pathToCSVFileField,
                    new JLabel("Show Token: "), showTokenField,
                    new JLabel("RGB Verification Color ('R,G,B'): "), verificationColorField,
                    new JLabel("Marcher Label: "), marcherLabelField  // Removed LED count field
            };

            /* Create custom dialog with save-on-close functionality */
            JOptionPane pane = new JOptionPane(
                    inputs,
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION
            );
            JDialog dialog = pane.createDialog("Enter board parameters:");

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    savePropertiesToFile(
                            props,
                            configFile,
                            pathToDataFolderField.getText(),
                            pathToExtractedPacketsField.getText(),
                            pathToCSVFileField.getText(),
                            showTokenField.getText(),
                            verificationColorField.getText(),
                            marcherLabelField.getText()
                    );
                }
            });

            dialog.setVisible(true);

            /* Handle user selection */
            Object selectedValue = pane.getValue();
            if (selectedValue != null && (Integer)selectedValue == JOptionPane.OK_OPTION) {
                // Save properties on OK
                savePropertiesToFile(
                        props,
                        configFile,
                        pathToDataFolderField.getText(),
                        pathToExtractedPacketsField.getText(),
                        pathToCSVFileField.getText(),
                        showTokenField.getText(),
                        verificationColorField.getText(),
                        marcherLabelField.getText()
                );

                /* Process parameters */
                File dataDir = new File(pathToDataFolderField.getText());
                File packetDir = new File(pathToExtractedPacketsField.getText());
                File csv = new File(pathToCSVFileField.getText());
                String token = showTokenField.getText();
                String color = verificationColorField.getText();
                String label = marcherLabelField.getText().toUpperCase();

                /* Automatically get LED count from CSV */
                String numLeds;
                try {
                    numLeds = CSVLEDCounter.getLedCount(label, String.valueOf(csv));
                } catch (Error e) {
                    JOptionPane.showMessageDialog(null,
                            "Error reading LED count from CSV: " + e.getMessage(),
                            "CSV Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                /* Process Show Data, Update the Platform.io Data .txt Files */
                SetupFileSystem.processShowData(dataDir, packetDir, csv, token, color, numLeds, label);

                /* Upload Filesystem via Platform.io */
                PlatformIOFunction.uploadFilesystem(dataDir);
            }
        });

        realWiredProgramming.addActionListener(e -> { // i only tested all the correct paths here
            //first check if a show is open
            // idk check if csv is here or not and if so ask for label
            // we also need to ask for a token and verification color
            // then get that led strip and run the packet exporter on it and write it to serial transmitter
            if (archivePaths == null) {
                JOptionPane.showMessageDialog(null, "There is no project currently open. Please open a project file to program a board.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String label = JOptionPane.showInputDialog(null, "Enter the board label to program (case insensitive):",
                    "Board Label Input", JOptionPane.QUESTION_MESSAGE);
            if (label == null || label.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "No label entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            LEDStrip targetStrip = null;
            for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
                if (ledStrip.getLabel().equalsIgnoreCase(label.trim())) {
                    targetStrip = ledStrip;
                    break;
                }
            }
            if (targetStrip == null) {
                JOptionPane.showMessageDialog(null, "No LED strip found with label: " + label,
                        "Lookup Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ask for token and verification color
            String token = JOptionPane.showInputDialog(null, "Enter the show token:",
                    "Show Token Input", JOptionPane.QUESTION_MESSAGE);
            if (token == null || token.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "No token entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            SerialTransmitter st = comPortPrompt("Receiver");
            if (st == null) {
                JOptionPane.showMessageDialog(null, "No Receiver found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!st.getType().equals("Receiver")) {
                return;
            }

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

            ArrayList<LEDStrip> targetStripAL = new ArrayList<>();
            targetStripAL.add(targetStrip);
            PacketExport pe = new PacketExport(targetStripAL, timesMS);

            // now make a string to send to the board
            // first it will start "p, <token>, <verification color>\n"
            // then append all data in File(PathConverter.pathConverter("tmp/" + l.getId()
            // createNewFile
            try {
                File dir = new File(PathConverter.pathConverter("tmp/", false));
                dir.mkdirs();
                File newFile = new File(PathConverter.pathConverter("tmp/" + targetStrip.getId(), false));
                newFile.createNewFile();
                pe.run(); // fills the tmp file

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(newFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null,
                            "Error reading packet file: " + ex.getMessage(),
                            "File Error",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    return;
                }
                st.writeShow(token.trim(), sb.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Error creating temporary packet file: " + ex.getMessage(),
                        "File Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        /* Resetting RSSI Log On Board */
        resetRSSIItem.addActionListener(j -> {
            /* Check for Board Receiver Type */
            try {
                SerialTransmitter st = comPortPrompt("Receiver");
                if (!st.getType().equals("Receiver")) {
                    throw new IllegalStateException("Not a receiver");
                }
            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Transmitter Detected, Please plug in a receiver.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please plug a board in before proceeding.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            /* Check for Windows OS */
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                JOptionPane.showMessageDialog(null,
                        "PlatformIO check is only supported on Windows at this time.",
                        "Unsupported OS",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            /* Send the Clear Message Over Serial */
            try {
                SerialTransmitter st = comPortPrompt("Receiver");
                if (!st.getType().equals("Receiver")) {
                    throw new IllegalStateException("Not a receiver");
                }

                // Clear RSSI Data
                st.clearRSSIData();

                JOptionPane.showMessageDialog(
                        null,
                        "RSSI Data Cleared Successfully",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Transmitter Detected, Please Plug In A Receiver.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please Plug In A Board Before Proceeding",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
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

        // TODO: Actually implement this with a real login server or delete this feature
        JMenuItem loginItem = new JMenu("Account");
        menuBar.add(loginItem);

        JMenuItem signIn = new JMenuItem("Sign In");
        signIn.addActionListener(e -> {
            writeSysMsg("Signing in...");
            new UserAuthGUI(frame, this); // This assumes UserAuthGUI sets itself visible
        });
        loginItem.add(signIn);

        // System message
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(sysMsg);
        
        // Add small spacer between system message and hardware indicator
        menuBar.add(Box.createRigidArea(new Dimension(10, 0)));
        
        // Add hardware status indicator
        hardwareStatusIndicator = new HardwareStatusIndicator(this);
        menuBar.add(hardwareStatusIndicator);
        

        //Light menu. and adjust its menu location
        JComponent effectOptions = getEffectOptionsButton();
        effectViewPanel.add(effectOptions, BorderLayout.NORTH);


        // Display the window
        if (archivePaths == null) {
            frame.setJMenuBar(menuBar);

            // Show welcome screen on first run (no project loaded)
            JPanel welcome = buildWelcomePanel(this);
            // keep scrub/play controls visible under the welcome screen
            replaceMainView(welcome, scrubBarPanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setTitle("Emrick Designer");
        } else {
            frame.revalidate();
            frame.repaint();
        }
    }

    // Helper method to save properties
    private void savePropertiesToFile(Properties props,
                                      File configFile,
                                      String dataDir,
                                      String packetsDir,
                                      String csvFile,
                                      String showToken,
                                      String verificationColor,
                                      String marcherLabel) {
        props.setProperty("data.dir", dataDir);
        props.setProperty("packets.dir", packetsDir);
        props.setProperty("csv.file", csvFile);
        props.setProperty("show.token", showToken);
        props.setProperty("verification.color", verificationColor);
        props.setProperty("marcher.label", marcherLabel);

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Board Configuration");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Error saving settings: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Recursively empties and deletes the specified file/directory.
     *
     * @param directoryToBeDeleted - File or directory that should be emptied and/or deleted
     * @return true - if the directory was deleted successfully. false - otherwise
     */
    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    /**
     * Initializes the Effect Options button and the effects popup menu and returns the Effect Options button.
     *
     * @return JButton button that displays a popup menu with all the effect options when pressed
     */
    private JComponent getEffectOptionsButton() {
        JPopupMenu lightMenuPopup = new JPopupMenu();

        JMenuItem fadePattern = new JMenuItem("Create Fade Effect");
        fadePattern.addActionListener(e -> {
            selectedEffectType = EffectList.GENERATED_FADE;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(fadePattern);

        JMenuItem staticColorPattern = new JMenuItem("Create Static Color Effect");
        staticColorPattern.addActionListener(e -> {
            selectedEffectType = EffectList.STATIC_COLOR;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(staticColorPattern);

        JMenuItem wavePattern = new JMenuItem("Create Wave Effect");
        wavePattern.addActionListener(e -> {
            selectedEffectType = EffectList.WAVE;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(wavePattern);

        JMenuItem alternatingColorPattern = new JMenuItem("Create Alternating Color Effect");
        alternatingColorPattern.addActionListener(e -> {
            selectedEffectType = EffectList.ALTERNATING_COLOR;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(alternatingColorPattern);

        JMenuItem ripplePattern = new JMenuItem("Create Ripple Effect");
        ripplePattern.addActionListener(e -> {
            selectedEffectType = EffectList.RIPPLE;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(ripplePattern);

        JMenuItem circleChasePattern = new JMenuItem("Create Circle Chase Effect");
        circleChasePattern.addActionListener(e -> {
            selectedEffectType = EffectList.CIRCLE_CHASE;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(circleChasePattern);

        JMenuItem chasePattern = new JMenuItem("Create Chase Effect");
        chasePattern.addActionListener(e -> {
            selectedEffectType = EffectList.CHASE;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(chasePattern);

        JMenuItem gridPattern = new JMenuItem("Create Grid Effect");
        gridPattern.addActionListener(e -> {
            selectedEffectType = EffectList.GRID;
            createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(gridPattern);

        JMenuItem randomNoisePattern = new JMenuItem("Create Random Noise Effect");
        randomNoisePattern.addActionListener(e -> {
           selectedEffectType = EffectList.NOISE;
           createEffectAtCurrentTime(selectedEffectType);
        });
        lightMenuPopup.add(randomNoisePattern);

        // Button that triggers the popup menu
        JButton lightButton = new JButton("Create Effect");
        lightButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int x = 0;
                int y = lightButton.getHeight();
                lightMenuPopup.show(lightButton, x, y);
            }
        });

        // Create RF Trigger button placed next to the effect options
        JButton createRFBtn = new JButton("Create RF Trigger");
        createRFBtn.addActionListener(e -> {
            if (timeManager == null || footballFieldPanel == null || effectManager == null) return;
            int currentCount = footballFieldPanel.getCurrentCount();
            Long ts = timeManager.getCount2MSec().get(currentCount);
            long timestamp = ts == null ? 0L : ts;
            RFTrigger rf = new RFTrigger(currentCount, timestamp, "", "", "");
            // delegate to the existing handler which will validate and add trigger
            onCreateRFTrigger(rf);
        });

        // Checkbox to toggle showing all effects vs current (old behavior)
        JCheckBox showAllChk = new JCheckBox("Show All Effects", showAllEffects);
        showAllChk.addActionListener(e -> {
            showAllEffects = showAllChk.isSelected();
            // If a timeline exists, update it (we recreate timeline in updateTimelinePanel)
            if (timelineGUI != null) {
                timelineGUI.setShowAllEffects(showAllEffects);
                updateTimelinePanel();
            }
        });

        // Pack into a small panel
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnl.add(lightButton);
        pnl.add(createRFBtn);
        pnl.add(showAllChk);
        return pnl;
    }

    /**
     * Helper method to create a 1-second effect of the given type at the current timeline position
     */
    private void createEffectAtCurrentTime(EffectList effectType) {
        if (footballFieldPanel == null || timeManager == null || effectManager == null) {
            return;
        }
        
        // current ms is the offset of where the set ms starts plus the current ms in the football field panel
        long currentMS = footballFieldPanel.currentMS;
        if (timeManager.getCount2MSec() != null && timeManager.getCount2MSec().containsKey(footballFieldPanel.getCurrentCount())) {
            currentMS = timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
        }
        // make the endms either 0.001 seconds before the next effect/trigger or 8 seconds after currentms whichever is smaller
        long endMS = currentMS + 8000; // default to 8 seconds
        
        Long nextEffectMS = effectManager.getNextEffectOrTriggerStartMS(currentMS);
        if (nextEffectMS != null) {
            endMS = Math.min(endMS, nextEffectMS - 1);
        }
        
        // Create a basic effect with default colors
        GeneratedEffect newEffect = GeneratedEffectAdapter.createDefaultEffect(
                effectType,
                currentMS,
                endMS,
                effectManager.nextId()
        );
        
        // Convert GeneratedEffect to Effect before passing to onCreateEffect
        Effect effectToCreate = newEffect.generateEffectObj();
        
        // Add the effect through the normal effect creation flow
        onCreateEffect(effectToCreate);
    }

    /**
     * Used to get a Serial Transmitter object.
     * Now uses the hardware status indicator instead of prompting the user each time.
     *
     * @param type The type of hardware that should be detected ("Transmitter" or "Receiver").
     * @return A SerialTransmitter object for the specified type.
     * If no hardware of the requested type is available, this method returns null.
     */
    public SerialTransmitter comPortPrompt(String type) {
        if (hardwareStatusIndicator == null) {
            // Fallback to old behavior if indicator not initialized
            return oldComPortPrompt(type);
        }
        
        SerialTransmitter result = null;
        
        if ("Transmitter".equals(type)) {
            result = hardwareStatusIndicator.getTransmitter();
            if (result == null) {
                writeSysMsg("No transmitter available - check hardware status indicator");
                JOptionPane.showMessageDialog(frame,
                        "No transmitter detected. Please connect an Emrick transmitter and make sure it is not busy to appear in the hardware scanner.",
                        "No Transmitter Available",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                writeSysMsg("Using transmitter from hardware status indicator");
            }
        } else if ("Receiver".equals(type)) {
            result = hardwareStatusIndicator.getReceiver();
            if (result == null) {
                writeSysMsg("No receiver available - check hardware status indicator");
                JOptionPane.showMessageDialog(frame,
                        "No receiver detected. Please connect an Emrick receiver and make sure it is not busy to appear in the hardware scanner.",
                        "No Receiver Available",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                writeSysMsg("Using receiver from hardware status indicator");
            }
        }
        
        return result;
    }

    public SerialTransmitter comPortPromptFlow() {
        // just return current transmitter from hardware status indicator manually to bypass type check
        if (hardwareStatusIndicator == null) {
            // Fallback to old behavior if indicator not initialized
            return oldComPortPrompt("Transmitter");
        }
        return hardwareStatusIndicator.getTransmitterBypass();
    }
    
    /**
     * Legacy method for hardware detection - kept as fallback
     */
    private SerialTransmitter oldComPortPrompt(String type) {
        SerialTransmitter st = new SerialTransmitter();
        SerialPort[] allPorts = SerialTransmitter.getPortNames();
        if (allPorts.length == 0) {
            return null;
        }
        String[] allPortNames = new String[allPorts.length];
        writeSysMsg("Attempting to find Emrick Hardware");
        for (int i = 0; i < allPorts.length; i++) {
            allPortNames[i] = allPorts[i].getDescriptivePortName();
        }
        String port = "";
        for (int i = 0; i < allPortNames.length; i++) {
            if (st.getBoardType(allPortNames[i]).equals(type)) {
                if (port.isEmpty()) {
                    port = allPortNames[i];
                } else {
                    port = "";
                    break;
                }
            }
        }

        if (port.isEmpty()) {
            port = (String) JOptionPane.showInputDialog(null, "Choose",
                    "Menu", JOptionPane.INFORMATION_MESSAGE,
                    new ImageIcon(PathConverter.pathConverter("icon.ico", true)),
                    allPortNames, allPortNames[0]);
        } else {
            writeSysMsg("Found Emrick Hardware at: " + port);
        }
        st.setSerialPort(port);
        return st;
    }

    /**
     * Removes the flow viewer from the main content panel and restores the run menu to be used again
     */
    private void removeFlowViewer() {
        mainContentPanel.remove(flowViewGUI);
        runShowItem.setEnabled(true);
        flowViewerItem.setEnabled(true);
        lightBoardFlowViewerItem.setEnabled(true);
        stopShowItem.setEnabled(false);
    }

    /**
     * Stops the currently running web server, restores the run menu to be used again,
     * and cleans the filesystem of any files created by the web server
     */
    private void stopServer() {
        server.stop(0);
        noRequestTimer.stop();
        server = null;
        requestIDs = null;
        stopWebServer.setEnabled(false);
        runWebServer.setEnabled(true);
        runLightBoardWebServer.setEnabled(true);

        File dir = new File(PathConverter.pathConverter("tmp/", false));
        File[] files = dir.listFiles();
        for (File f : files) {
            f.delete();
        }
        dir.delete();
        File f = new File(PathConverter.pathConverter("tempPkt.pkt", false));
        if (f.exists()) {
            f.delete();
        }
    }

    /**
     * Prompts the user for information and then starts a web server using this information
     *
     * @param path A path to the .pkt file whose contents should be served by the web server.
     * @param lightBoard true - Run the web server to serve light board packets
     *                   false - Run the web server to serve show packets
     */
    private void runServer(String path, boolean lightBoard) {
        try {
            /* File PKT Selection */
            File f;
            // If a project is loaded, generate the packets from the project and write them to a temp file in project directory.
            // delete file after server is stopped.
            if(archivePaths == null) { //if no project open
                if (path.isEmpty()) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select Packets (.pkt) file");
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Emrick Designer Packets File (*.pkt)", "pkt"));
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                        stopWebServer.setEnabled(false);
                        runWebServer.setEnabled(true);
                        runLightBoardWebServer.setEnabled(true);
                        return;
                    }
                    f = fileChooser.getSelectedFile();
                } else {
                    f = new File(path);
                }
            }
            else{ //there is a project open
                if (path.isEmpty()) {
                    f = new File(PathConverter.pathConverter("tempPkt.pkt", false));
                    exportPackets(f);
                } else {
                    f = new File(path);
                }
            }

            /* WiFi Credentials Input */
            JTextField ssidField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JTextField portField = new JTextField("8080");
            JCheckBox useSavedCred = new JCheckBox("Use Saved Credentials");
            useSavedCred.setSelected(true);
            JCheckBox rememberCredentials = new JCheckBox("Remember Credentials");

            Object[] inputs = {
                    new JLabel("WiFi SSID:"), ssidField,
                    new JLabel("WiFi Password:"), passwordField,
                    new JLabel("Server Port:"), portField,
                    useSavedCred, rememberCredentials
            };

            int option = 0;


            option = JOptionPane.showConfirmDialog(null, inputs, "Enter WiFi Credentials", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) {
                stopWebServer.setEnabled(false);
                runWebServer.setEnabled(true);
                runLightBoardWebServer.setEnabled(true);
                deleteDirectory(f);
                return;
            }

            if (useSavedCred.isSelected()) {
                File cred = new File (PathConverter.pathConverter("wifiConfig.txt", false));
                if (cred.exists()) {
                    // TODO: add encryption
                    BufferedReader bfr = new BufferedReader(new FileReader(cred));
                    ssidField.setText(bfr.readLine());
                    StringBuilder pass = new StringBuilder(bfr.readLine());
                    String[] tmp = pass.toString().split(", ");
                    pass = new StringBuilder();
                    for (String s : tmp) {
                        pass.append(s);
                    }
                    passwordField.setText(pass.substring(1, pass.length() - 1));
                    portField.setText(bfr.readLine());
                }
            }
            if (rememberCredentials.isSelected()) {
                File cred = new File (PathConverter.pathConverter("wifiConfig.txt", false));
                BufferedWriter bfw = new BufferedWriter(new FileWriter(cred));
                String out = ssidField.getText() + "\n" + Arrays.toString(passwordField.getPassword()) + "\n" + portField.getText() + "\n";
                bfw.write(out);
                bfw.flush();
                bfw.close();
            }

            ssid = ssidField.getText();
            char[] passwordChar = passwordField.getPassword();
            password = new String(passwordChar);
            port = Integer.parseInt(portField.getText());

            /* Check For Transmitter */
            try {
                SerialTransmitter st1 = comPortPrompt("Transmitter");
                if (!st1.getType().equals("Transmitter")) {
                    /* Reset Menu Options */
                    stopWebServer.setEnabled(false);
                    runWebServer.setEnabled(true);
                    runLightBoardWebServer.setEnabled(true);
                    deleteDirectory(f);

                    JOptionPane.showMessageDialog(
                            null,
                            "Receiver Detected, Please plug in a transmitter.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please plug a transmitter board in before proceeding.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );

                /* Reset Menu Options */
                stopWebServer.setEnabled(false);
                runWebServer.setEnabled(true);
                runLightBoardWebServer.setEnabled(true);
                deleteDirectory(f);
                return;
            }

            /* Unzip .pkt File */
            Unzip.unzip(f.getAbsolutePath(), PathConverter.pathConverter("tmp/", false));

            /* Entering / Generating a New Token */
            String input = JOptionPane.showInputDialog(null, "Enter verification token (leave blank for new token)\n\nDon't use this feature to program more than 200 units");

            if (input != null) {    // User input something
                if (input.isEmpty()) {  // New Token Generation
                    Random r = new Random();
                    token = r.nextInt(0, Integer.MAX_VALUE);
                    JOptionPane.showMessageDialog(null, new JTextArea("The token for this show is: " + token + "\n Save this token in case some boards are not programmed"));
                } else {
                    token = Integer.parseInt(input);
                }
            } else {    // User didn't input anything + closed box
                stopWebServer.setEnabled(false);
                runWebServer.setEnabled(true);
                runLightBoardWebServer.setEnabled(true);
                deleteDirectory(f);
                return;
            }

            // Set Current ID To All Boards Available. Max WiFi router can handle is 200 connections, otherwise it thinks a DDOS attack.
            // Changed from Old Version to follow show programming flow better. Case by case worked better than all at once.
            //Old Version -> currentID = Math.min(MAX_CONNECTIONS, footballFieldPanel.drill.ledStrips.size()); // Set Current ID to Smaller Option Between Max Connections + Boards Available
            currentID = footballFieldPanel.drill.ledStrips.size();

            /*  Verification Color */
            // Check to see if vColor.txt file exists. If so, check to ensure token matches and then extract verification color. Else create file + store color.
            File vColor = new File(PathConverter.pathConverter("vColor.txt", false));
            if (!vColor.exists()) { // No Verification Color. Create file and save user input.
                // Create vColor.txt File
                try {
                    if (vColor.createNewFile()) {
                        System.out.println("File created: " + vColor.getAbsolutePath());
                    } else {
                        System.out.println("vColor.txt already exists.");
                    }
                } catch (IOException e) {
                    System.out.println("An error occurred while creating vColor.txt file.");
                    e.printStackTrace();
                }

                // Get User Entered Verification Color
                verificationColor = JColorChooser.showDialog(this, "Select verification color", Color.WHITE);
                if (verificationColor == null) {
                    stopWebServer.setEnabled(false);
                    runWebServer.setEnabled(true);
                    runLightBoardWebServer.setEnabled(true);
                    return;
                }

                // Write to vColor.txt
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(vColor))) {
                    // Save Token:
                    writer.write("Token: " + token);
                    writer.newLine();

                    // Format color as "R,G,B"
                    String rgbString = verificationColor.getRed() + "," +
                            verificationColor.getGreen() + "," +
                            verificationColor.getBlue();

                    writer.write("Verification Color: " + rgbString);
                } catch (IOException e) {
                    System.out.println("An error occurred while writing to vColor.txt.");
                    e.printStackTrace();
                }
            } else {    // vColor.txt file exists already. Check to see if token matches. If so pull color otherwise write a new one.
                // Read File
                try (BufferedReader reader = new BufferedReader(new FileReader(vColor))) {
                    // Find Token & Verification Color
                    String tokenLine = reader.readLine();
                    String colorLine = reader.readLine();

                    if (tokenLine != null && colorLine != null && tokenLine.startsWith("Token:") && colorLine.startsWith("Verification Color:")) {
                        String storedToken = tokenLine.substring("Token:".length()).trim();
                        if (token == (Integer.parseInt(storedToken))) {     // Token Matches
                            // Parse Verification Color R,G,B
                            String[] rgbParts = colorLine.substring("Verification Color:".length()).trim().split(",");
                            if (rgbParts.length == 3) {
                                int r = Integer.parseInt(rgbParts[0]);
                                int g = Integer.parseInt(rgbParts[1]);
                                int b = Integer.parseInt(rgbParts[2]);
                                verificationColor = new Color(r, g, b);
                            }
                        } else { // Token Didn't Match
                            // Get a New Verification Color Since Token is Different:
                            verificationColor = JColorChooser.showDialog(this, "Select verification color", Color.WHITE);
                            if (verificationColor == null) {
                                stopWebServer.setEnabled(false);
                                runWebServer.setEnabled(true);
                                runLightBoardWebServer.setEnabled(true);
                                return;
                            }

                            // Write new token + verification color to vColor.txt
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter(vColor))) {
                                // Save Token:
                                writer.write("Token: " + token);
                                writer.newLine();

                                // Format color as "R,G,B"
                                String rgbString = verificationColor.getRed() + "," +
                                        verificationColor.getGreen() + "," +
                                        verificationColor.getBlue();

                                writer.write("Verification Color: " + rgbString);
                            } catch (IOException e) {
                                System.out.println("An error occurred while writing to vColor.txt.");
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error reading vColor.txt");
                    e.printStackTrace();
                }
            }

            /* Create Webserver */
            server = HttpServer.create(new InetSocketAddress(port), 250);
            writeSysMsg("server started at " + port);
            requestIDs = new HashSet<>();

            server.createContext("/", new GetHandler(PathConverter.pathConverter("tmp/", false), this));
            server.setExecutor(new ServerExecutor());
            server.start();

            webServerFrame = new JFrame("Board Programming Tracker");
            webServerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            webServerFrame.setSize(800, 600);
            webServerFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(PathConverter.pathConverter("res/images/icon.png", true)));

            /* Create a new Private Class Instance of Programming Tracker */
            programmingTracker = new ProgrammingTracker(footballFieldPanel.drill.ledStrips, requestIDs);

            /* Create the Pane of All Board Labels */
            JScrollPane scrollPane = new JScrollPane(programmingTracker);
            JPanel fullPanel = new JPanel();
            fullPanel.setLayout(new BoxLayout(fullPanel, BoxLayout.Y_AXIS));
            fullPanel.add(scrollPane);

            /* Create a Progess Bar */
            programmingProgressBar = new JProgressBar(0, footballFieldPanel.drill.ledStrips.size());
            programmingProgressBar.setValue(requestIDs.size());
            programmingProgressBar.setStringPainted(false); // We'll show summary separately
            programmingProgressBar.setPreferredSize(new Dimension(300, 20));
            //fullPanel.add(programmingProgressBar);

            /* Add a Descriptive Label */
            updateProgressLabel(programmingProgressLabel, requestIDs, programmingTracker.getAlreadyProgrammedStrips(), footballFieldPanel.drill.ledStrips.size());

            fullPanel.add(programmingProgressLabel);
            webServerFrame.add(fullPanel);
            webServerFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    stopServer();
                    super.windowClosing(e);
                }
            });
            webServerFrame.setVisible(true);
            lightBoardMode = lightBoard;

            SerialTransmitter serialTransmitter = comPortPrompt("Transmitter");
            System.out.println("Starting Programming Mode");
            serialTransmitter.enterProgMode(ssid, password, port, currentID, token, verificationColor, lightBoardMode);

            noRequestTimer.start();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    //

    private void updateProgressLabel(JLabel label, java.util.Set<Integer> newlyProgrammed, java.util.Set<Integer> previouslyProgrammed, int totalBoards) {
        int totalCompleted = newlyProgrammed.size() + previouslyProgrammed.size();
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<b>Previously Programmed:</b> ").append(previouslyProgrammed.size()).append("<br>");
        sb.append("<b>Newly Programmed:</b> ").append(newlyProgrammed.size()).append("<br>");
        sb.append("<b>Total Boards:</b> ").append(totalCompleted).append(" / ").append(totalBoards);
        sb.append("</html>");
        label.setText(sb.toString());

        /* Handle Font Color For Light / Dark Mode */
        Color foreground = UIManager.getLookAndFeel() instanceof FlatDarkLaf
                ? Color.WHITE
                : Color.BLACK;
        label.setForeground(foreground);
    }

    // Marker3
    /**
     * Prompts the user for information and then starts a web server using this information for RSSI Logging
     */
    private void runRSSIServer() {
        try {
            /* Select Results Save Path */
            String rssiResultsSavePath = "";

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select RSSI Results Save Location");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                rssiResultsSavePath = fileChooser.getSelectedFile().getAbsolutePath();
            }

            /* WiFi Credentials Input */
            JTextField ssidField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JTextField portField = new JTextField("8080");
            JCheckBox useSavedCred = new JCheckBox("Use Saved Credentials");
            useSavedCred.setSelected(true);
            JCheckBox rememberCredentials = new JCheckBox("Remember Credentials");

            Object[] inputs = {
                    new JLabel("WiFi SSID:"), ssidField,
                    new JLabel("WiFi Password:"), passwordField,
                    new JLabel("Server Port:"), portField,
                    useSavedCred, rememberCredentials
            };

            int option = 0;


            option = JOptionPane.showConfirmDialog(null, inputs, "Enter WiFi Credentials", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) {
                stopRSSILogger.setEnabled(false);
                runRSSILogger.setEnabled(true);
                return;
            }

            if (useSavedCred.isSelected()) {
                File cred = new File (PathConverter.pathConverter("wifiConfig.txt", false));
                if (cred.exists()) {
                    // TODO: add encryption
                    BufferedReader bfr = new BufferedReader(new FileReader(cred));
                    ssidField.setText(bfr.readLine());
                    StringBuilder pass = new StringBuilder(bfr.readLine());
                    String[] tmp = pass.toString().split(", ");
                    pass = new StringBuilder();
                    for (String s : tmp) {
                        pass.append(s);
                    }
                    passwordField.setText(pass.substring(1, pass.length() - 1));
                    portField.setText(bfr.readLine());
                }
            }
            if (rememberCredentials.isSelected()) {
                File cred = new File (PathConverter.pathConverter("wifiConfig.txt", false));
                BufferedWriter bfw = new BufferedWriter(new FileWriter(cred));
                String out = ssidField.getText() + "\n" + Arrays.toString(passwordField.getPassword()) + "\n" + portField.getText() + "\n";
                bfw.write(out);
                bfw.flush();
                bfw.close();
            }

            ssid = ssidField.getText();
            char[] passwordChar = passwordField.getPassword();
            password = new String(passwordChar);
            port = Integer.parseInt(portField.getText());

            /* Check For Transmitter */
            try {
                SerialTransmitter st1 = comPortPrompt("Transmitter");
                if (!st1.getType().equals("Transmitter")) {
                    /* Reset Menu Options */
                    stopRSSILogger.setEnabled(false);
                    runRSSILogger.setEnabled(true);

                    JOptionPane.showMessageDialog(
                            null,
                            "Receiver Detected, Please plug in a transmitter.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Please plug a transmitter board in before proceeding.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );

                /* Reset Menu Options */
                stopRSSILogger.setEnabled(false);
                runRSSILogger.setEnabled(true);
                return;
            }

            /* NEW */
            /* Define The Amount of Allowed Connections to Webserver */
            int allowedConnections = footballFieldPanel.drill.ledStrips.size();

            /* Create Webserver */
            rssiServer = HttpServer.create(new InetSocketAddress(port), 250);
            writeSysMsg("server started at " + port);
            System.out.println("Server Started at " + port);
            requestIDs = new HashSet<>();

            rssiServer.createContext("/upload", new RSSIFileUploaderHandler(rssiResultsSavePath));
            rssiServer.setExecutor(null);
            rssiServer.start();


        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * Loads a new .emrick file to the viewport to be edited.
     * @param path Path pointing to the intended .emrick file
     */

    private void loadProject(File path) {
        try {

            if (archivePaths != null) {
                // reinitialize everything
                createAndShowGUI();
            }

            emrickPath = path;

            File showDataDir = new File(PathConverter.pathConverter("show_data/", false));
            showDataDir.mkdirs();
            File[] cleanFiles = showDataDir.listFiles();
            for (File f : cleanFiles) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
            Unzip.unzip(path.getAbsolutePath(), PathConverter.pathConverter("show_data/", false));
            File[] dataFiles = showDataDir.listFiles();
            for (File f : dataFiles) {
                if (!f.isDirectory()) {
                    if (f.getName().substring(f.getName().lastIndexOf(".")).equals(".json")) {
                        path = f;
                    }
                }
            }

            ProjectFile pf = null;
            OldProjectFile opf = null;
            FileReader r = new FileReader(path);

            pf = gson.fromJson(r, ProjectFile.class);

            //outdated file processing
            if (pf == null || pf.archiveNames == null) {
                r.close();
                r = new FileReader(path);
                opf = gson.fromJson(r, OldProjectFile.class);
                pf = null;
            }

            r.close();
            ImportArchive ia = new ImportArchive(this);


            archivePaths = new ArrayList<>();
            if (pf != null) {
                for (String s : pf.archiveNames) {
                    archivePaths.add(new File(PathConverter.pathConverter("show_data/" + s, false)));
                }

                //System.out.println(archivePaths.get(0));
                ia.fullImport(archivePaths, null);
                footballFieldPanel.drill = pf.drill;
                footballFieldPanel.drill.performers.sort(Comparator.comparingInt(Performer::getPerformerID));
                for (Performer p : footballFieldPanel.drill.performers) {
                    p.setLedStrips(new ArrayList<>());
                }
                for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
                    Performer p = footballFieldPanel.drill.performers.get(ledStrip.getPerformerID());
                    p.addLEDStrip(ledStrip.getId());
                    ledStrip.setPerformer(p);

                }
                for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
                    for (Effect e : ledStrip.getEffects()) {
                        if (e.getEffectType() == EffectList.GRID) {
                            GridShape[] shapes = ((GridEffect) e.getGeneratedEffect()).getShapes();
                            for (GridShape g : shapes) {
                                g.recoverLEDStrips(footballFieldPanel.drill.ledStrips);
                            }
                        }
                    }
                }
                ledStripViewGUI = new LEDStripViewGUI(new ArrayList<>(), effectManager);
                footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(0));
                ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(0));

                footballFieldBackground.justResized = true;
                footballFieldBackground.repaint();

                ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);

                groupsGUI.setGroups(pf.selectionGroups, footballFieldPanel.drill.ledStrips);
                groupsGUI.initializeButtons();

                if (pf.timeSync != null && pf.startDelay != null) {
                    timeSync = pf.timeSync;
                    onSync(timeSync, pf.startDelay);
                    scrubBarGUI.setTimeSync(timeSync);
                    startDelay = pf.startDelay;
                    count2RFTrigger = pf.count2RFTrigger;
                    footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
                    setupEffectView(pf.ids);
                    rebuildPageTabCounts();
                    updateTimelinePanel();
                    updateEffectViewPanel(selectedEffectType, null);
                }
            }
            else if (opf != null){
                //for outdated .emrick files
                archivePaths.add(new File(PathConverter.pathConverter("show_data/" + opf.archivePath, false)));

                ia.fullImport(archivePaths, null);
                footballFieldPanel.drill = opf.drill;
                for (Set s : footballFieldPanel.drill.sets) {
                    s.label = "1-" + s.label;
                }
                for (SyncTimeGUI.Pair time : opf.timeSync) {
                    time.setKey("1-" + time.getKey());
                }
                for (Coordinate c : footballFieldPanel.drill.coordinates) {
                    c.set = "1-" + c.set;
                }
                for (Performer p : footballFieldPanel.drill.performers) {
                    for (Coordinate c : p.getCoordinates()) {
                        c.set = "1-" + c.set;
                    }
                }
                footballFieldPanel.drill.performers.sort(Comparator.comparingInt(Performer::getPerformerID));
                for (Performer p : footballFieldPanel.drill.performers) {
                    p.setLedStrips(new ArrayList<>());
                }
                for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
                    Performer p = footballFieldPanel.drill.performers.get(ledStrip.getPerformerID());
                    p.addLEDStrip(ledStrip.getId());
                    ledStrip.setPerformer(p);

                }
                for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
                    for (Effect e : ledStrip.getEffects()) {
                        if (e.getEffectType() == EffectList.GRID) {
                            GridShape[] shapes = ((GridEffect) e.getGeneratedEffect()).getShapes();
                            for (GridShape g : shapes) {
                                g.recoverLEDStrips(footballFieldPanel.drill.ledStrips);
                            }
                        }
                    }
                }


                ledStripViewGUI = new LEDStripViewGUI(new ArrayList<>(), effectManager);
                footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(0));
                ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(0));

                footballFieldBackground.justResized = true;
                footballFieldBackground.repaint();

                ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);

                groupsGUI.setGroups(opf.selectionGroups, footballFieldPanel.drill.ledStrips);
                groupsGUI.initializeButtons();

                if (opf.timeSync != null && opf.startDelay != null) {
                    timeSync = opf.timeSync;
                    onSync(timeSync, opf.startDelay);
                    scrubBarGUI.setTimeSync(timeSync);
                    startDelay = opf.startDelay;
                    count2RFTrigger = opf.count2RFTrigger;
                    footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
                    setupEffectView(opf.ids);
                    rebuildPageTabCounts();
                    updateTimelinePanel();
                    updateEffectViewPanel(selectedEffectType, null);
                }
            }
            else {
                System.out.println("Project File Null");
                return;
            }
            currentMovement = 1;
            scrubBarGUI.setCurrAudioPlayer(this.currentAudioPlayer);
            // Record recent project and switch main view to the football field
            try {
                    if (emrickPath != null) addToRecentProjects(emrickPath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
                if (mainContentPanel != null) {
                    // keep scrub/play controls visible
                    replaceMainView(footballField, scrubBarPanel);
                }

        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            writeSysMsg("Failed to open to `" + path + "`.");
            throw new RuntimeException(e);
        }
    }
    private void concatenateProject(File path) {
        try {

            emrickPath = path;

            File showDataDir = new File(PathConverter.pathConverter("show_data/", false));

            Unzip.unzip(path.getAbsolutePath(), PathConverter.pathConverter("show_data/", false));
            File[] dataFiles = showDataDir.listFiles();
            for (File f : dataFiles) {
                if (!f.isDirectory()) {
                    if (f.getName().substring(f.getName().lastIndexOf(".")).equals(".json") && f.getName().contains(path.getName().substring(0, path.getName().indexOf(".")))) {
                        path = f;
                    }
                }
            }


            FileReader r = new FileReader(path);

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Color.class, new ColorAdapter());
            builder.registerTypeAdapter(Point2D.class, new Point2DAdapter());
            builder.registerTypeAdapter(SyncTimeGUI.Pair.class, new PairAdapter());
            builder.registerTypeAdapter(Duration.class, new DurationAdapter());
            builder.registerTypeAdapter(GeneratedEffect.class, new GeneratedEffectAdapter());
            builder.registerTypeAdapter(JButton.class, new JButtonAdapter());
            builder.serializeNulls();

            ProjectFile pf = null;
            OldProjectFile opf = null;
            pf = gson.fromJson(r, ProjectFile.class);

            if (pf == null || pf.archiveNames == null) {
                r.close();
                r = new FileReader(path);
                opf = gson.fromJson(r, OldProjectFile.class);
                pf = null;
            }
            r.close();
            ImportArchive ia = new ImportArchive(this);

            ArrayList<File> aPaths = new ArrayList<>();
            ArrayList<Integer> ids;

            if (pf != null) {
                for (String s : pf.archiveNames) {
                    aPaths.add(new File(PathConverter.pathConverter("show_data/" + s, false)));
                }

                ia.concatImport(aPaths, null);
                archivePaths.addAll(aPaths);

                //Append drill
                int oldNumSets = footballFieldPanel.drill.sets.size();
                pf.drill.sets.get(0).duration = 1;
                int movementIndex = getMovementIndex();

                //ensure lists are sorted
                footballFieldPanel.drill.performers.sort(Comparator.comparingInt(Performer::getPerformerID));
                pf.drill.performers.sort(Comparator.comparingInt(Performer::getPerformerID));

                //total time of last project
                long oldProjectLenMs = 0;

                int i;
                for (Map.Entry<Integer, Long> entry : timeManager.getCount2MSec().entrySet()) {
                    if (entry.getValue() > oldProjectLenMs) {
                        oldProjectLenMs = entry.getValue();
                    }
                }

                int oldNumCounts = 0;
                for (Set s : footballFieldPanel.drill.sets) {
                    oldNumCounts += s.duration;
                }
                oldNumCounts++;

                //enhanced for loop may result in concurrent modification exception
                for (i = 0; i < pf.drill.performers.size(); i++) {
                    Performer current = pf.drill.performers.get(i);
                    for (int j = 0; j < current.getCoordinates().size(); j++) {
                        current.getCoordinates().get(j).setSet(movementIndex + "-" + current.getCoordinates().get(j).getSet()
                                .substring(current.getCoordinates().get(j).getSet().indexOf("-") + 1));
                        footballFieldPanel.drill.performers.get(i).getCoordinates().add(current.getCoordinates().get(j));
                    }
                }

                //edit and append coordinates array from the drill class
                for (Coordinate c : pf.drill.coordinates) {
                    c.setSet(movementIndex + "-" + c.getSet().substring(c.getSet().indexOf("-") + 1));
                    footballFieldPanel.drill.coordinates.add(c);
                }

                for (Set s : pf.drill.sets) {
                    s.label = movementIndex + "-" + s.label.substring(s.label.indexOf("-") + 1);
                    s.index += oldNumSets;
                    footballFieldPanel.drill.sets.add(s);
                }
                //again, I'm not sure if the references are identical so this is here just in case
                if (pf.drill.sets.get(0).index < oldNumSets) {
                    for (Set s : pf.drill.sets) {
                        s.index += oldNumSets;
                        footballFieldPanel.drill.sets.add(s);
                    }
                }

                for (SyncTimeGUI.Pair p : pf.timeSync) {
                    p.setKey(movementIndex + p.getKey().substring(p.getKey().indexOf("-")));
                }
                timeSync.addAll(pf.timeSync);

                //readjust the counts and timestamps in the new RFTriggers and add them to count2RFTrigger
                for (Map.Entry<Integer, RFTrigger> e: pf.count2RFTrigger.entrySet()) {
                    e.getValue().setCount(e.getValue().getCount() + oldNumCounts);
                    e.getValue().setTimestampMillis(e.getValue().getTimestampMillis() + oldProjectLenMs);
                    count2RFTrigger.put(e.getKey() + oldNumCounts, pf.count2RFTrigger.get(e.getKey()));
                }

                footballFieldPanel.drill.ledStrips.sort(Comparator.comparingInt(LEDStrip::getId));
                pf.drill.ledStrips.sort(Comparator.comparingInt(LEDStrip::getId));

                int maxID = 0;
                //find the highest effect ID so there is no ID overlap between movements
                for (LEDStrip l : footballFieldPanel.drill.ledStrips) {
                    for (Effect e : l.getEffects()) {
                        if (e.getId() > maxID) {
                            maxID = e.getId();
                        }
                    }
                }
                maxID++;

                //increment all effect IDs by the maxID
                i = 0;
                for (LEDStrip l : pf.drill.getLedStrips()) {
                    for (Effect e : l.getEffects()) {

                        Effect copyEffect = e.makeDeepCopy();
                        copyEffect.setId(e.getId() + maxID);
                        copyEffect.setStartTimeMSec(e.getStartTimeMSec() + oldProjectLenMs);
                        copyEffect.setEndTimeMSec(e.getEndTimeMSec() + oldProjectLenMs);

                        GeneratedEffect ge = e.getGeneratedEffect();
                        Effect geEffect = ge.generateEffectObj();
                        geEffect.setId(copyEffect.getId());
                        GeneratedEffect genEffect;
                        switch (e.getEffectType()) {
                            case CHASE -> genEffect = GeneratedEffectLoader.generateChaseEffectFromEffect(geEffect);
                            case GRID -> genEffect = GeneratedEffectLoader.generateGridEffectFromEffect(geEffect);
                            case RIPPLE -> genEffect = GeneratedEffectLoader.generateRippleEffectFromEffect(geEffect);
                            case WAVE -> genEffect = GeneratedEffectLoader.generateWaveEffectFromEffect(geEffect);
                            case CIRCLE_CHASE -> genEffect = GeneratedEffectLoader.generateCircleChaseEffectFromEffect(geEffect);
                            case GENERATED_FADE -> genEffect = GeneratedEffectLoader.generateFadeEffectFromEffect(geEffect);
                            case ALTERNATING_COLOR -> genEffect = GeneratedEffectLoader.generateAlternatingColorEffectFromEffect(geEffect);
                            case NOISE -> genEffect = GeneratedEffectLoader.generateRandomNoiseEffectFromEffect(geEffect);
                            default -> genEffect = GeneratedEffectLoader.generateStaticColorEffectFromEffect(geEffect);
                        }
                        genEffect.setStartTime(genEffect.getStartTime() + oldProjectLenMs);
                        genEffect.setEndTime(genEffect.getEndTime() + oldProjectLenMs);
                        copyEffect.setGeneratedEffect(genEffect);


                        footballFieldPanel.drill.ledStrips.get(i).addEffect(copyEffect);
                        System.out.println("START TIME = " + copyEffect.getStartTimeMSec() + "ID = " + copyEffect.getId());
                    }
                    i++;
                }

                for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
                    for (Effect e : ledStrip.getEffects()) {
                        if (e.getEffectType() == EffectList.GRID) {
                            GridShape[] shapes = ((GridEffect) e.getGeneratedEffect()).getShapes();
                            for (GridShape g : shapes) {
                                g.recoverLEDStrips(footballFieldPanel.drill.ledStrips);
                            }
                        }
                    }
                }
                ids = new ArrayList<>(effectManager.getIds());

                for (Integer id : pf.ids) {
                    id += maxID;
                    if (!ids.contains(id)) {
                        ids.add(id);
                    }
                }

                //copy RFTriggers (onSync creates new Table)
                HashMap<Integer, RFTrigger> copy = new HashMap<>(count2RFTrigger);
                onSync(timeSync, startDelay);
                scrubBarGUI.setTimeSync(timeSync);

                //putRFTriggers back in
                count2RFTrigger.putAll(copy);

                rebuildPageTabCounts();
                footballFieldPanel.setCount2RFTrigger(count2RFTrigger);

                for (SelectionGroupGUI.SelectionGroup group : pf.selectionGroups) {
                    if (!groupsGUI.getGroups().contains(group)) {
                        groupsGUI.getGroups().add(group);
                    }
                }

                ledStripViewGUI = new LEDStripViewGUI(footballFieldPanel.drill.ledStrips, effectManager);
                footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(0));
                ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(0));

                footballFieldBackground.justResized = true;
                footballFieldBackground.repaint();

                ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);

                groupsGUI.initializeButtons();

            }
            else if (opf != null) {  //old emrick file support
                aPaths.add(new File(PathConverter.pathConverter("show_data/" + opf.archivePath, false)));
                ia.concatImport(aPaths, null);
                archivePaths.addAll(aPaths);
                //Append drill
                int oldNumSets = footballFieldPanel.drill.sets.size();
                opf.drill.sets.get(0).duration = 1;
                int movementIndex = getMovementIndex();

                //ensure lists are sorted
                footballFieldPanel.drill.performers.sort(Comparator.comparingInt(Performer::getPerformerID));
                opf.drill.performers.sort(Comparator.comparingInt(Performer::getPerformerID));

                //total time of last project
                long oldProjectLenMs = 0;

                int i;
                int count = 0;
                for (Map.Entry<Integer, Long> entry : timeManager.getCount2MSec().entrySet()) {
                    if (entry.getValue() > oldProjectLenMs) {
                        oldProjectLenMs = entry.getValue();
                    }
                    count++;
                }

                int oldNumCounts = 0;
                for (Set s : footballFieldPanel.drill.sets) {
                    oldNumCounts += s.duration;
                }
                oldNumCounts++;

                //enhanced for loop may result in concurrent modification
                for (i = 0; i < opf.drill.performers.size(); i++) {
                    Performer current = opf.drill.performers.get(i);
                    for (int j = 0; j < current.getCoordinates().size(); j++) {
                        current.getCoordinates().get(j).setSet(movementIndex + "-" + current.getCoordinates().get(j).getSet());
                        footballFieldPanel.drill.performers.get(i).getCoordinates().add(current.getCoordinates().get(j));
                    }
                }

                //edit and append coordinates array from the drill class
                for (Coordinate c : opf.drill.coordinates) {
                    c.setSet(movementIndex + "-" + c.getSet());
                    footballFieldPanel.drill.coordinates.add(c);
                }


                //I'm not sure if this is necessary since I don't know if the sets above are the same references
                //as the sets below.
                for (Set s : opf.drill.sets) {
                    s.label = movementIndex + "-" + s.label;
                    s.index += oldNumSets;
                    footballFieldPanel.drill.sets.add(s);
                }

                //again, I'm not sure if the references are identical so this is here just in case
                if (opf.drill.sets.get(0).index < oldNumSets) {
                    for (Set s : opf.drill.sets) {
                        s.index += oldNumSets;
                        footballFieldPanel.drill.sets.add(s);
                    }
                }

                for (SyncTimeGUI.Pair p : opf.timeSync) {
                    p.setKey(movementIndex + "-" + p.getKey());
                }
                timeSync.addAll(opf.timeSync);

                //readjust the counts and timestamps in the new RFTriggers and add them to count2RFTrigger
                for (Map.Entry<Integer, RFTrigger> e : opf.count2RFTrigger.entrySet()) {
                    e.getValue().setCount(e.getValue().getCount() + oldNumCounts);
                    e.getValue().setTimestampMillis(e.getValue().getTimestampMillis() + oldProjectLenMs);
                    count2RFTrigger.put(e.getKey() + oldNumCounts, opf.count2RFTrigger.get(e.getKey()));
                }



                footballFieldPanel.drill.ledStrips.sort(Comparator.comparingInt(LEDStrip::getId));
                opf.drill.ledStrips.sort(Comparator.comparingInt(LEDStrip::getId));

                int maxID = 0;
                //find the highest effect ID so there is no ID overlap between movements
                for (LEDStrip l : footballFieldPanel.drill.ledStrips) {
                    for (Effect e : l.getEffects()) {
                        if (e.getId() > maxID) {
                            maxID = e.getId();
                        }
                    }
                }
                maxID++;
                i = 0;
                //increment all effect IDs by the maxID
                for (LEDStrip l : opf.drill.getLedStrips()) {
                    for (Effect e : l.getEffects()) {

                        Effect copyEffect = new Effect(e.getStartTimeMSec(),
                                e.getStartColor(), e.getEndColor(), e.getDelay(), e.getDuration(), e.getTimeout(),
                                e.isUSE_DURATION(), e.isSET_TIMEOUT(), e.isDO_DELAY(), e.isINSTANT_COLOR(), e.getId());
                        copyEffect.setEffectType(e.getEffectType());
                        copyEffect.setId(e.getId() + maxID+1);
                        copyEffect.setStartTimeMSec(e.getStartTimeMSec() + oldProjectLenMs);
                        copyEffect.setEndTimeMSec(e.getEndTimeMSec() + oldProjectLenMs);
                        copyEffect.setChaseSequence(e.getChaseSequence());
                        copyEffect.setFunction(e.getFunction());
                        copyEffect.setUpOrSide(e.isUpOrSide());
                        copyEffect.setSpeed(e.getSpeed());

                        GeneratedEffect ge = e.getGeneratedEffect();
                        Effect geEffect = ge.generateEffectObj();
                        geEffect.setId(copyEffect.getId());
                        GeneratedEffect genEffect;
                        switch (e.getEffectType()) {
                            case CHASE -> genEffect = GeneratedEffectLoader.generateChaseEffectFromEffect(geEffect);
                            case GRID -> genEffect = GeneratedEffectLoader.generateGridEffectFromEffect(geEffect);
                            case RIPPLE -> genEffect = GeneratedEffectLoader.generateRippleEffectFromEffect(geEffect);
                            case WAVE -> genEffect = GeneratedEffectLoader.generateWaveEffectFromEffect(geEffect);
                            case CIRCLE_CHASE -> genEffect = GeneratedEffectLoader.generateCircleChaseEffectFromEffect(geEffect);
                            case GENERATED_FADE -> genEffect = GeneratedEffectLoader.generateFadeEffectFromEffect(geEffect);
                            case ALTERNATING_COLOR -> genEffect = GeneratedEffectLoader.generateAlternatingColorEffectFromEffect(geEffect);
                            case NOISE -> genEffect = GeneratedEffectLoader.generateRandomNoiseEffectFromEffect(geEffect);
                            default -> genEffect = GeneratedEffectLoader.generateStaticColorEffectFromEffect(geEffect);
                        }
                        genEffect.setStartTime(genEffect.getStartTime() + oldProjectLenMs);
                        genEffect.setEndTime(genEffect.getEndTime() + oldProjectLenMs);
                        copyEffect.setGeneratedEffect(genEffect);

                        copyEffect.setAngle(e.getAngle());
                        copyEffect.setDirection(e.isDirection());
                        copyEffect.setHeight(e.getHeight());
                        copyEffect.setShapes(e.getShapes());
                        copyEffect.setSize(e.getSize());
                        copyEffect.setWidth(e.getWidth());


                        footballFieldPanel.drill.ledStrips.get(i).addEffect(copyEffect);
                        System.out.println("START TIME = " + copyEffect.getStartTimeMSec() + "ID = " + copyEffect.getId());
                    }
                    i++;
                }

                for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
                    for (Effect e : ledStrip.getEffects()) {
                        if (e.getEffectType() == EffectList.GRID) {
                            GridShape[] shapes = ((GridEffect) e.getGeneratedEffect()).getShapes();
                            for (GridShape g : shapes) {
                                g.recoverLEDStrips(footballFieldPanel.drill.ledStrips);
                            }
                        }
                    }
                }
                
                ids = new ArrayList<>(effectManager.getIds());

                for (Integer id : opf.ids) {
                    id += maxID;
                    if (!ids.contains(id)) {
                        ids.add(id);
                    }
                }

                //copy RFTriggers (onSync creates new table)
                HashMap<Integer, RFTrigger> copy = new HashMap<>(count2RFTrigger);

                onSync(timeSync, startDelay);

                //put RFTriggers back in
                count2RFTrigger.putAll(copy);
                System.out.println(oldNumCounts);

                rebuildPageTabCounts();
                footballFieldPanel.setCount2RFTrigger(count2RFTrigger);

                for (SelectionGroupGUI.SelectionGroup group : opf.selectionGroups) {
                    if (!groupsGUI.getGroups().contains(group)) {
                        groupsGUI.getGroups().add(group);
                    }
                }


                int prevTotalCounts = 0;
                for (Set s : footballFieldPanel.drill.sets) {
                    prevTotalCounts += s.duration; //add up total counts
                }
                ledStripViewGUI = new LEDStripViewGUI(footballFieldPanel.drill.ledStrips, effectManager);
                footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(0));
                ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(0));

                footballFieldBackground.justResized = true;
                footballFieldBackground.repaint();

                ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);

                groupsGUI.initializeButtons();
            }
            else {
                return;  //better option?
            }

            footballFieldPanel.setCount2RFTrigger(count2RFTrigger);

            setupEffectView(ids);
            rebuildPageTabCounts();
            updateTimelinePanel();
            updateEffectViewPanel(selectedEffectType, null);
            currentMovement = 1;

        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            writeSysMsg("Failed to open to `" + path + "`.");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private int getMovementIndex() {
        int movementIndex;

        if (footballFieldPanel.drill.sets.get(0).label.contains("-")) {

            //takes the last set in the old drill and finds the index value of its movement and adds one
            movementIndex = Integer.parseInt(footballFieldPanel.drill.sets.get(footballFieldPanel.drill.sets.size() - 1)
                    .label.substring(0, footballFieldPanel.drill.sets.get(footballFieldPanel.drill.sets.size() - 1)
                            .label.indexOf('-'))) + 1;
        }
        else {
            movementIndex = 2;  //only one movement in the old drill
        }
        if (movementIndex < 1) {
            movementIndex = 1;
        }
        return movementIndex;
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

        mainContentPanel.add(scrubBarPanel, BorderLayout.SOUTH);

        // IMPORTANT
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    @Override
    public void onMultiSelect(HashSet<Integer> labels, HashSet<String> symbols) {
        footballFieldPanel.selectedLEDStrips.clear();
        for (Performer p : footballFieldPanel.drill.performers) {
            if (labels.contains(p.getLabel()) && symbols.contains(p.getSymbol())) {
                for (Integer i : p.getLedStrips()) {
                    LEDStrip l = footballFieldPanel.drill.ledStrips.get(i);
                    footballFieldPanel.selectedLEDStrips.add(l);
                }
            }
        }
        footballFieldPanel.repaint();
    }

    @Override
    public void onGroupSelection(LEDStrip[] ledStrips) {
        footballFieldPanel.selectedLEDStrips.clear();
        footballFieldPanel.selectedLEDStrips.addAll(Arrays.asList(ledStrips));
        footballFieldPanel.repaint();
    }

    @Override
    public void ctrlGroupSelection(LEDStrip[] ledStrips){
        boolean allSelected = true;
        for(LEDStrip l : ledStrips) {
            if (!footballFieldPanel.selectedLEDStrips.contains(l)) {
                allSelected = false;
            }
        }
        if(allSelected) {
            for(LEDStrip l : ledStrips) {
                footballFieldPanel.selectedLEDStrips.remove(l);
            }
        }
        else{
            footballFieldPanel.selectedLEDStrips.addAll(Arrays.asList(ledStrips));
        }
        footballFieldPanel.repaint();
    }

    @Override
    public LEDStrip[] onSaveGroup() {
        Iterator<LEDStrip> iterator = footballFieldPanel.selectedLEDStrips.iterator();
        LEDStrip[] ledStrips = new LEDStrip[footballFieldPanel.selectedLEDStrips.size()];
        for (int i = 0; i < ledStrips.length; i++) {
            ledStrips[i] = iterator.next();
        }
        return ledStrips;
    }

    @Override
    public void onUpdateGroup() {
        updateEffectViewPanel(selectedEffectType, null);
    }

    /**
     * Builds and exports a csv file containing configuration data for led strips and performers
     * <p>
     *     Format:
     * <p>
     *     The first line contains headers and the last element contains the expected number of led strips.
     * <p>
     *     A new performer starts on a new line where its label (Ex. "R31") is written as the first element
     * <p>
     *     Below each performer label will be a list of all of this performer's led strips,
     *     each line beginning with an empty element.
     *     The led strips lines will contain their label, id, and a set of configuration data.
     * <p>
     *     Ex. ",226,R31L,50,12,6,-6,-6"
     *
     * @param selectedFile The desired file location to write the csv file.
     */
    private void exportCsvFileForPerformerDeviceIDs(File selectedFile) {
        try (FileWriter fileWriter = new FileWriter(selectedFile)) {
            fileWriter.write("Performer Label,LED ID,LED Label,LED Count,Height,Width,Horizontal Offset,VerticalOffset,,Size:," + footballFieldPanel.drill.ledStrips.size());
            fileWriter.write("\n");
            fileWriter.flush();
            for (Performer performer : footballFieldPanel.drill.performers) {
                fileWriter.write(performer.getIdentifier());
                fileWriter.write("\n");
                fileWriter.flush();
                for (Integer i : performer.getLedStrips()) {
                    LEDStrip l = footballFieldPanel.drill.ledStrips.get(i);
                    fileWriter.write(",");
                    fileWriter.write(Integer.toString(l.getId()));
                    fileWriter.write(",");
                    fileWriter.write(performer.getIdentifier() + l.getLedConfig().getLabel());
                    fileWriter.write(",");
                    fileWriter.write(Integer.toString(l.getLedConfig().getLEDCount()));
                    fileWriter.write(",");
                    fileWriter.write(Integer.toString(l.getLedConfig().getHeight()));
                    fileWriter.write(",");
                    fileWriter.write(Integer.toString(l.getLedConfig().getWidth()));
                    fileWriter.write(",");
                    fileWriter.write(Integer.toString(l.getLedConfig().gethOffset()));
                    fileWriter.write(",");
                    fileWriter.write(Integer.toString(l.getLedConfig().getvOffset()));
                    fileWriter.write("\n");
                    fileWriter.flush();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies a default led configuration to all performers
     */
    private void applyDefaultLEDConfiguration() {
        footballFieldPanel.drill.performers.sort(new Comparator<Performer>() {
            @Override
            public int compare(Performer o1, Performer o2) {
                return o1.getIdentifier().compareTo(o2.getIdentifier());
            }
        });
        int id = 0;
        int pid = 0;
        footballFieldPanel.drill.ledStrips = new ArrayList<>();
        for (Performer p : footballFieldPanel.drill.performers) {
            p.setPerformerID(pid);
            pid++;
            LEDConfig c1 = new LEDConfig();
            c1.setLabel("L");
            c1.sethOffset(1);
            LEDConfig c2 = new LEDConfig();
            c2.setLabel("R");
            LEDStrip l1 = new LEDStrip(id, p, c1);
            id++;
            LEDStrip l2 = new LEDStrip(id, p, c2);
            id++;
            p.setLedStrips(new ArrayList<>());
            p.getLedStrips().add(l1.getId());
            p.getLedStrips().add(l2.getId());
            footballFieldPanel.drill.ledStrips.add(l1);
            footballFieldPanel.drill.ledStrips.add(l2);
        }
    }

    /**
     * Imports a configuration csv file and applies the config to an open project.
     * @param inputFile csv configuration file
     */
    private void parseCsvFileForPerformerDeviceIDs(File inputFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line = reader.readLine();
            int size = 0;
            if (line != null) {
                String[] split = line.split(",");
                size = Integer.parseInt(split[10]);
                line = reader.readLine();
            }
            Performer currPerformer = null;
            ArrayList<Performer> newPerformerList = new ArrayList<>();
            ArrayList<LEDStrip> newLedStripList = new ArrayList<>();
            int currStripID = 0;
            int currPerformerID = 0;

            // Capture old strips so we can preserve effects by matching labels
            java.util.Map<String, LEDStrip> oldStripByKey = new java.util.HashMap<>();
            if (footballFieldPanel.drill.ledStrips != null) {
                for (LEDStrip old : footballFieldPanel.drill.ledStrips) {
                    Performer p = old.getPerformer();
                    String key = "";
                    if (p != null && p.getIdentifier() != null && old.getLedConfig() != null && old.getLedConfig().getLabel() != null) {
                        key = p.getIdentifier() + old.getLedConfig().getLabel();
                        oldStripByKey.put(key, old);
                    }
                }
            }

            // Very strange buffered reader bug occurs for large csv files
            // The current code works so don't touch it unless major changes need to happen
            while (line != null && currStripID < size) {
                if (!line.startsWith(",")) {
                    String[] tmp = line.replaceAll("\\.", "").split(",");
                    try {
                        if (footballFieldPanel.drill.performers.isEmpty()) {
                            break;
                        }
                        currPerformer = footballFieldPanel.drill.performers.stream().filter(p -> p.getIdentifier().equals(tmp[0])).findFirst().get();
                        footballFieldPanel.drill.performers.remove(currPerformer);
                        currPerformer.setLedStrips(new ArrayList<>());
                        currPerformer.setPerformerID(currPerformerID);
                        currPerformerID++;
                        newPerformerList.add(currPerformer);
                    } catch (NoSuchElementException e) {
                        // TODO: show error message and prompt for a new csv file
                        throw new RuntimeException(e);
                    }
                } else {
                    String[] tmp = line.split(",");
                    String label = tmp[2].substring(tmp[2].length() - 1);
                    int ledCount = Integer.parseInt(tmp[3]);
                    int height = Integer.parseInt(tmp[4]);
                    int width = Integer.parseInt(tmp[5]);
                    int hOffset = Integer.parseInt(tmp[6]);
                    int vOffset = Integer.parseInt(tmp[7]);
                    LEDStrip ledStrip = new LEDStrip(currStripID, currPerformer, new LEDConfig(ledCount, height, width, hOffset, vOffset, label));
                    // If there was an equivalent old strip (matching performer identifier + label), copy its effects
                    String compositeKey = currPerformer.getIdentifier() + label;
                    LEDStrip oldEquivalent = oldStripByKey.get(compositeKey);
                    if (oldEquivalent != null) {
                        // Deep-copy effects to avoid shared references and set them via setEffects
                        ArrayList<Effect> copied = new ArrayList<>();
                        for (Effect e : oldEquivalent.getEffects()) {
                            if (e != null) {
                                Effect copy = e.makeDeepCopy();
                                copied.add(copy);
                            }
                        }
                        ledStrip.setEffects(copied);
                    }
                    currStripID++;
                    newLedStripList.add(ledStrip);
                    currPerformer.getLedStrips().add(ledStrip.getId());
                }
                line = reader.readLine();
            }
            footballFieldPanel.drill.performers = newPerformerList;
            footballFieldPanel.drill.ledStrips = newLedStripList;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    ////////////////////////// Sync Listeners //////////////////////////

    /**
     * Opens a prompt for the user to select a project to open.
     */
    private void openProjectDialog() {
        writeSysMsg("Opening project...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Project");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Emrick Project Files (*.emrick)","emrick"));

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            writeSysMsg("Opening file `" + fileChooser.getSelectedFile().getAbsolutePath() + "`.");
            loadProject(fileChooser.getSelectedFile());
        }
    }

    private void concatenateDialog() {
        writeSysMsg("Concatenating Project");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Project");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Emrick Project Files (*.emrick)", "emrick"));

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            writeSysMsg("Opening file '" + fileChooser.getSelectedFile().getAbsolutePath() + "' for concatenation.");
            concatenateProject(fileChooser.getSelectedFile());
        }
    }

    /**
     * Attempts to save the project to a file.
     * If the currently open project is a new project, the user will be prompted to specify
     * a save location before the project is saved.
     */
    private void saveProjectDialog() {
        if (archivePaths == null) {
            System.out.println("Nothing to save.");
            writeSysMsg("Nothing to save!");
            return;
        }

        writeSysMsg("Saving Project...");
        if (emrickPath != null) {
            writeSysMsg("Saving file `" + emrickPath + "`.");
            saveProject(emrickPath, archivePaths);
        } else {
            saveAsProjectDialog();
        }
    }

    /**
     * Prompts the user for a location to save the current project.
     */
    private void saveAsProjectDialog() {
        if (archivePaths == null) {
            System.out.println("Nothing to save.");
            writeSysMsg("Nothing to save!");
            return;
        }

        writeSysMsg("Saving New Project...");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Project");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Emrick Project Files (*.emrick)",
                "emrick"));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".emrick")) {
                path += ".emrick";
            }
            writeSysMsg("Saving file `" + path + "`.");
            saveProject(new File(path), archivePaths);
        }
    }



    ////////////////////////// Effect Listeners //////////////////////////

    ////////////////////////// Football Field Listeners //////////////////////////

    ////////////////////////// Importing Listeners //////////////////////////

    @Override
    public void onBeginImport() {
        if (effectManager != null) {
            if (!effectManager.getUndoStack().isEmpty()) {
                int resp = JOptionPane.showConfirmDialog(frame,
                        "Would you like to save before quitting?",
                        "Save and Quit?",
                        JOptionPane.YES_NO_OPTION);
                if (resp == JOptionPane.YES_OPTION) {
                    System.out.println("User saving and quitting.");
                    saveProjectDialog();
                } else if (resp == JOptionPane.NO_OPTION) {
                    System.out.println("User not saving but quitting anyway.");
                }
            }
        }
    }
    @Override
    public void onImport() {
        scrubBarGUI.setReady(true);
    }

    @Override
    public void onFileSelect(ArrayList<File> archivePaths, File csvFile) {
        if (this.archivePaths != null) {
            createAndShowGUI();
        }
        this.archivePaths = archivePaths;
        /*
        if (archivePaths != null) {
            for (File f : archivePaths) {

                if (f != null) {
                    this.archivePaths.add(f);
                }
            }
        }
         */
        this.csvFile = csvFile;
        emrickPath = null;
    }

    @Override
    public void onAudioImport(ArrayList<File> audioFiles) {
        // Playing or pausing audio is done through the AudioPlayer service class
        audioPlayers = new ArrayList<AudioPlayer>();
        for (File f : audioFiles) {
            audioPlayers.add(new AudioPlayer(f));
            scrubBarGUI.setAudioPlayer(audioPlayers);
        }

    }
    @Override
    public void onConcatAudioImport(ArrayList<File> audioFiles) {
        for (File f : audioFiles) {
            audioPlayers.add(new AudioPlayer(f));
        }
        scrubBarGUI.setAudioPlayer(audioPlayers);
    }

    @Override
    public void onDrillImport(String drill) {
        String text = DrillParser.extractText(drill);
        footballFieldPanel.drill = DrillParser.parseWholeDrill(text);
        if (csvFile != null) {
            parseCsvFileForPerformerDeviceIDs(csvFile);
        } else {
            applyDefaultLEDConfiguration();
        }
        footballFieldPanel.addSetToField(footballFieldPanel.drill.sets.get(0));
        currentMovement = 1;  //cannot be other movements if drill is being imported
        count2RFTrigger = new HashMap<>();
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        footballFieldBackground.justResized = true;
        footballFieldBackground.repaint();
        updateEffectViewPanel(selectedEffectType, null);
        updateTimelinePanel();
        rebuildPageTabCounts();


        ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);

        replaceMainView(ledConfigurationGUI, scrubBarPanel);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    /**
     * Rebuilds the map of set labels to set start counts that is used in ScrubBarGUI.
     */
    private void rebuildPageTabCounts() {
        Map<String, Integer> pageTabCounts = new HashMap<>();
        int startCount = 0;
        int totalCounts;
        for (Set s : footballFieldPanel.drill.sets) {
            startCount += s.duration;
            pageTabCounts.put(s.label, startCount);
        }
        totalCounts = startCount;



        scrubBarGUI.updatePageTabCounts(pageTabCounts, totalCounts);
        buildScrubBarPanel();
    }

    ////////////////////////// Sync Listeners //////////////////////////

    @Override
    public void onSync(ArrayList<SyncTimeGUI.Pair> times, float startDelay) {
        writeSysMsg("Got Synced Times");

        if (this.timeSync == null) {
            count2RFTrigger = new HashMap<>();
            footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        }

        this.timeSync = times;
        this.startDelay = startDelay;

        scrubBarGUI.setTimeSync(timeSync);
            count2RFTrigger = new HashMap<>();
            footballFieldPanel.setCount2RFTrigger(count2RFTrigger);

        setupEffectView(null);
        rebuildPageTabCounts();
        updateTimelinePanel();
        effectGUI = new EffectGUI(EffectGUI.selectEffectMsg);
        
        ledStripViewGUI = new LEDStripViewGUI(new ArrayList<>(), effectManager);
    }

    /**
     * Initializes the effect panel and its dependencies
     * @param ids List of effect ids
     */
    private void setupEffectView(ArrayList<Integer> ids) {

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
        if (ids != null) {
            this.effectManager.setIds(ids);
        }
        this.footballFieldPanel.setEffectManager(this.effectManager);

        updateEffectViewPanel(selectedEffectType, null);
    }

    ////////////////////////// Scrub Bar Listeners //////////////////////////

    @Override
    public boolean onPlay() {
        long period;
        if (timeSync == null) {
            JOptionPane.showMessageDialog(frame, "Cannot play without syncing time!",
                    "Playback Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (audioPlayers != null && scrubBarGUI.getAudioCheckbox().isSelected()) {
            playAudioFromCorrectPosition();
        }
        if (scrubBarGUI.isUseFps()) {
            period = (long) (1 / scrubBarGUI.getFps() * 1000.0 / playbackSpeed);
            footballFieldPanel.setUseFps(true);
            System.out.println("Start time: " + timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount()));
        } else {
            period = getPlaybackTimerTimeByCounts();
            footballFieldPanel.setUseFps(false);
        }
        playbackTimer = new java.util.Timer();
        playbackTimer.scheduleAtFixedRate(new PlaybackTask(), 0, period);

        return true;
    }

    @Override
    public boolean onPause() {
        if (timeSync == null) {
            JOptionPane.showMessageDialog(frame, "Cannot play without syncing time!",
                    "Playback Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (audioPlayers != null) {
            audioPlayers.get(currentMovement - 1).pauseAudio();
        }
        if (playbackTimer != null) {
            playbackTimer.cancel();
            playbackTimer.purge();
            playbackTimer = null;
        }
        return true;
    }

    @Override
    public long onScrub() {
        // If time cursor is at start of first set, arm the start-delay
        useStartDelay = scrubBarGUI.isAtFirstSet() && scrubBarGUI.isAtStartOfSet();
        // If triggers are ready to be used, refresh on scroll
        if (count2RFTrigger != null) {
            //updateRFTriggerButton(); this one might need to be re-added later
        }
        if (scrubBarGUI.isPlaying() && canSeekAudio) {
            System.out.println("Called onScrub() -> Seeking audio...");
            playAudioFromCorrectPosition();
        }
        if (timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount()) != null) {
            return timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
        }
        return 0;
    }

    /////////////////////// SCRUBBAR LISTENERS //////////////////////////
    @Override
    public void onTimeChange(long time) {
        footballFieldPanel.currentMS = time;
        ledStripViewGUI.setCurrentMS(time);
        if (playbackTimer != null) { // this if for timeline when playing, ever so slightly off
            timelineGUI.scrubToMS(scrubBarGUI.getTime() * 1000);
        } else { // accurate
            timelineGUI.scrubToCount(footballFieldPanel.getCurrentCount());
        }
    }

    @Override
    public void onSetChange(int setIndex) {
        if (footballFieldPanel.getCurrentSet().label.contains("-")) {
            int nextSetMvmt = Integer.parseInt(footballFieldPanel.drill.sets.get(setIndex).label.substring(0,1));

            if (nextSetMvmt < 1 || nextSetMvmt > audioPlayers.size()) {
                return;
            }

            if (currentMovement != nextSetMvmt) {
                audioPlayers.get(currentMovement - 1).pauseAudio();
                currentMovement = nextSetMvmt;
                currentAudioPlayer = audioPlayers.get(currentMovement - 1);
                scrubBarGUI.setCurrAudioPlayer(currentAudioPlayer);
                if (scrubBarGUI.isPlaying() && scrubBarGUI.getAudioCheckbox().isSelected()) {
                    playAudioFromCorrectPosition();
                }
            }

        }
        footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(setIndex));
        ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(setIndex));

    }


    /**
     * Begin playing audio in sync with the drill playback
     */
    private void playAudioFromCorrectPosition() {
        // Get audio to correct position before playing
        if (!scrubBarGUI.getAudioCheckbox().isSelected()) {
            audioPlayers.get(currentMovement - 1).pauseAudio();
            return;
        }
        long timestampMillis = timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
        if (useStartDelay) {
            timestampMillis -= (long) (startDelay * 1000);
        }
        if (currentMovement < 1) {
            audioPlayers.get(0).playAudio(timestampMillis);
            System.out.println("Less than one");
        }
        else {
            audioPlayers.get(currentMovement - 1).pauseAudio();
            //finds correct time stamp for audio player based upon the point in the show and the total duration of the players that were before it
            audioPlayers.get(currentMovement - 1).playAudio(timestampMillis - getPrevAudioPlayerDurations(currentMovement - 1));
        }
    }

    public long getPrevAudioPlayerDurations(int index) {
        long totalMS = 0;
        for (int i = 0; i < index; i++) {
            totalMS += audioPlayers.get(i).getAudioLength();
        }
        return totalMS;
    }

    @Override
    public void onSpeedChange(float playbackSpeed) {
        System.out.println("MediaEditorGUI: playbackSpeed = " + playbackSpeed);
        // If playback speed is not normal, don't play the audio (simple solution)
        if (playbackSpeed != 1) {
            scrubBarGUI.getAudioCheckbox().setSelected(false);
            scrubBarGUI.getAudioCheckbox().setEnabled(false);
            if (audioPlayers != null) audioPlayers.get(currentMovement).pauseAudio();
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

    /**
     * Display an error message to the user that indicates an RF Trigger has not been placed yet.
     */
    private void showEffectBeforeFirstTriggerError() {
        JOptionPane.showMessageDialog(null,
                "Could not create effect. Ensure that an RF Trigger is placed before or at the same time as the first effect.",
                "Create Effect: Error", JOptionPane.ERROR_MESSAGE);
    }
    @Override
    public void onCreateEffect(Effect effect) {
        if (count2RFTrigger.values().size() == 0) {
            showEffectBeforeFirstTriggerError();
            return;
        }
        if (effect.getStartTimeMSec() < ((RFTrigger) count2RFTrigger.values().toArray()[0]).getTimestampMillis()) {
            showEffectBeforeFirstTriggerError();
            return;
        }
        boolean successful = this.effectManager.addEffectToSelectedLEDStrips(effect);
        if (ledStripViewGUI != null && ledStripViewGUI.isShowing()) {
            ledStripViewGUI.repaint();
        } else {
            this.footballFieldPanel.repaint();
        }
        if (successful) {
            updateEffectViewPanel(effect.getEffectType(), effect);
            updateTimelinePanel();
        }
    }

    @Override
    public void onUpdateEffect(Effect oldEffect, Effect newEffect) {
        // if valid for all selected strips, update
        for (LEDStrip l : effectManager.getLEDStripsWithEffect(oldEffect)) {
            if (!effectManager.isValid(newEffect, l, oldEffect)) {
                effectManager.showAddEffectErrorDialog(l);
                updateTimelinePanel();
                return;
            }
        }
        
        this.effectManager.replaceEffectForSelectedLEDStrips(oldEffect, newEffect);
        if (ledStripViewGUI.isShowing()) {
            ledStripViewGUI.repaint();
        } else {
            this.footballFieldPanel.repaint();
        }
        updateTimelinePanel();
    }

    @Override
    public void onDeleteEffect(Effect effect) {
        this.effectManager.removeEffectFromSelectedLEDStrips(effect);
        if (ledStripViewGUI.isShowing()) {
            ledStripViewGUI.repaint();
        } else {
            this.footballFieldPanel.repaint();
        }
        updateTimelinePanel();
        effectGUI = new EffectGUI(EffectGUI.selectEffectMsg);
        replaceEffectView(effectGUI.getEffectPanel(), null);
    }

    @Override
    public void onUpdateEffectPanel(Effect effect, boolean isNew, int index) {

        //this keeps the effect panel from flickering while the show is playing
        if (isPlaying()) {
            return;
        }
        this.effectViewPanel.remove(effectGUI.getEffectPanel());
        effectGUI = new EffectGUI(effect, effect.getStartTimeMSec(), this, effect.getEffectType(), isNew, index);
        this.effectViewPanel.add(effectGUI.getEffectPanel());
        this.effectViewPanel.revalidate();
        this.effectViewPanel.repaint();
    }

    @Override
    public void onChangeSelectionMode(boolean isInnerSelect, HashSet<LEDStrip> strips) {
        footballFieldPanel.innerSelect = isInnerSelect;
        if (isInnerSelect) {
            footballFieldPanel.innerSelectedLEDStrips = new HashSet<>();
            footballFieldPanel.innerSelectedLEDStrips.addAll(strips);
            footballFieldPanel.repaint();
        } else {
            footballFieldPanel.innerSelectedLEDStrips = new HashSet<>();
        }
    }

    @Override
    public HashSet<LEDStrip> onInnerSelectionRequired() {
        return footballFieldPanel.innerSelectedLEDStrips;
    }

    @Override
    public HashSet<LEDStrip> onSelectionRequired() {
        return footballFieldPanel.selectedLEDStrips;
    }

    @Override
    public TimeManager onTimeRequired() {
        return timeManager;
    }

    ////////////////////////// Football Field Listeners //////////////////////////

    @Override
    public void onPerformerSelect() {
        if (effectManager != null) {
            LEDStrip l = effectManager.getSelectedLEDStrips().get(0);
            long msec = footballFieldPanel.currentMS;
            if (!l.getEffects().isEmpty()) {
                Effect effect = effectManager.getEffect(l, msec);
                if (effect != null) {
                    if (selectedEffectType != EffectList.SHOW_GROUPS) {
                        selectedEffectType = effect.getEffectType();
                    }
                }
            }
            updateTimelinePanel();
        }
    }

    @Override
    public void onPerformerDeselect() {
        updateTimelinePanel();
    }

    //////////////////////////// Timeline Listeners //////////////////////////
    
    @Override
    public void onTimelineScrub(double count) {
        scrubBarGUI.setScrub((int)count);
    }

    ////////////////////////// RF Trigger Listeners //////////////////////////

    @Override
    public void onCreateRFTrigger(RFTrigger rfTrigger) {
        if (count2RFTrigger.containsKey(rfTrigger.getCount())) { // already an rf trigger at this count
            effectManager.showAddRFTriggerErrorDialog(null);
            return;
        }
        if (!effectManager.isValid(rfTrigger)) {
            return;
        }
        count2RFTrigger.put(footballFieldPanel.getCurrentCount(), rfTrigger);
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        updateTimelinePanel();
    }

    public void onUpdateRFTrigger(RFTrigger rfTrigger, int count) {
        if (!effectManager.isValid(rfTrigger)) {
            return;
        }
        count2RFTrigger.remove(count);
        count2RFTrigger.put(count, rfTrigger);
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        updateTimelinePanel();
    }

    @Override
    public void onDeleteRFTrigger(int count) {
        count2RFTrigger.remove(count);
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        updateTimelinePanel();
        effectGUI = new EffectGUI(EffectGUI.selectEffectMsg);
        replaceEffectView(effectGUI.getEffectPanel(), null);
    }

    @Override
    public void onPressRFTrigger(RFTrigger rfTrigger) {
        // scrub to this rf trigger
        System.out.println("MediaEditorGUI: onPressRFTrigger() called with count: " + rfTrigger.getCount() + " and ms: " + rfTrigger.getTimestampMillis());
        scrubBarGUI.setScrub(rfTrigger.getCount());
        // When an RF trigger is pressed, ensure the RF trigger create/delete panel is visible
        // and hide effect/group panels so only one view is shown at a time.
        int currentCount = footballFieldPanel.getCurrentCount();
        RFTrigger currentRFTrigger = count2RFTrigger.get(currentCount);
        rfTriggerGUI = new RFTriggerGUI(currentCount, timeManager.getCount2MSec().get(currentCount), currentRFTrigger, this);
        // Show only RF trigger panel
        replaceEffectView(rfTriggerGUI.getCreateDeletePnl(), null);
    }

    ////////////////////////// Effect Listeners //////////////////////////

    @Override
    public void onResizeBackground() {
        footballFieldPanel.setFieldHeight(footballFieldBackground.getFieldHeight());
        footballFieldPanel.setFieldWidth(footballFieldBackground.getFieldWidth() * 5.0/6.0);
        footballFieldPanel.setFrontSideline50(footballFieldBackground.getFrontSideline50());
        footballFieldPanel.repaint();
    }

    @Override
    public double getFrameRate() {
        return scrubBarGUI.getFps();
    }

    @Override
    public boolean isPlaying() {
        return scrubBarGUI.isPlaying();
    }

    @Override
    public void onPressEffect(Effect effect) {
        // selcting first ensures the effect appears on the Effect View
        // search all ledstrips for the effect and select them if found
        if (effectManager != null) {
            ArrayList<LEDStrip> ledStrips = effectManager.getLEDStripsWithEffect(effect);
            if (!ledStrips.isEmpty()) {
                onGroupSelection(ledStrips.toArray(new LEDStrip[ledStrips.size()]));
            }
        }

        // scrub to this effect
        // make effects based on count pls
        int count = 0;
        long ms = effect.getStartTimeMSec();
        if (timeManager != null) {
            count = timeManager.MSec2Count(ms);
        }
        scrubBarGUI.setScrub(count);
        
        replaceEffectView(effectGUI.getEffectPanel(), null);
        updateEffectViewPanel(effect.getEffectType(), effect);
    }

    /**
     * Update the effect panel to display the currently selected effect
     * @param effectType - The type of effect that is currently selected.
     */
    private void updateEffectViewPanel(EffectList effectType, Effect effect) {

        // No point in updating effect view if can't use effects
        if (effectManager == null) return;

        // Use atomic replace to ensure only one view is shown at a time
        // We'll construct the new center component below, then call replaceEffectView(center, south)
        // where south is used for RFTrigger create/delete panel (usually null for effect/group views)

        // Effects
        if (selectedEffectType != EffectList.SHOW_GROUPS) {
            if (footballFieldPanel.selectedLEDStrips.isEmpty()) {
                currentEffect = null;
                effectGUI = new EffectGUI(EffectGUI.selectEffectMsg);
                replaceEffectView(effectGUI.getEffectPanel(), null);
                return;
            }


            long currentMSec = timeManager.getCount2MSec().get(footballFieldPanel.getCurrentCount());
            currentEffect = effectManager.getEffectsFromSelectedLEDStrips(currentMSec);
            if (selectedEffectType == EffectList.HIDE_GROUPS) {
                if (!currentEffect.equals(new Effect(0))) {
                    selectedEffectType = currentEffect.getEffectType();
                } else {
                    selectedEffectType = EffectList.STATIC_COLOR;
                }

            }
            if (currentEffect == null) {
                effectGUI = new EffectGUI(EffectGUI.noCommonEffectMsg);
                replaceEffectView(effectGUI.getEffectPanel(), null);
                return;
            } else if (currentEffect.getEffectType() != EffectList.HIDE_GROUPS) {
                selectedEffectType = currentEffect.getEffectType();
            }
            if (currentEffect.getGeneratedEffect() != null) {
                if (currentEffect.getEffectType() == EffectList.WAVE) {
                    WaveEffect waveEffect = (WaveEffect) currentEffect.getGeneratedEffect();
                    currentEffect = new Effect(waveEffect.getStartTime());
                    currentEffect.setEndTimeMSec(waveEffect.getEndTime());
                    currentEffect.setStartColor(waveEffect.getStaticColor());
                    currentEffect.setEndColor(waveEffect.getWaveColor());
                    currentEffect.setDuration(waveEffect.getDuration());
                    currentEffect.setSpeed(waveEffect.getSpeed());
                    currentEffect.setUpOrSide(waveEffect.isVertical());
                    currentEffect.setDirection(waveEffect.isUpRight());
                    currentEffect.setEffectType(EffectList.WAVE);
                    currentEffect.setId(waveEffect.getId());
                } else if (currentEffect.getEffectType() == EffectList.STATIC_COLOR) {
                    StaticColorEffect staticColorEffect = (StaticColorEffect) currentEffect.getGeneratedEffect();
                    currentEffect = staticColorEffect.generateEffectObj();
                } else if (currentEffect.getEffectType() == EffectList.GENERATED_FADE) {
                    FadeEffect fadeEffect = (FadeEffect) currentEffect.getGeneratedEffect();
                    currentEffect = fadeEffect.generateEffectObj();
                } else if (currentEffect.getEffectType() == EffectList.ALTERNATING_COLOR) {
                    AlternatingColorEffect alternatingColorEffect = (AlternatingColorEffect) currentEffect.getGeneratedEffect();
                    currentEffect = alternatingColorEffect.generateEffectObj();
                } else if (currentEffect.getEffectType() == EffectList.RIPPLE) {
                    RippleEffect rippleEffect = (RippleEffect) currentEffect.getGeneratedEffect();
                    currentEffect = rippleEffect.generateEffectObj();
                } else if (currentEffect.getEffectType() == EffectList.CIRCLE_CHASE) {
                    CircleChaseEffect circleChaseEffect = (CircleChaseEffect) currentEffect.getGeneratedEffect();
                    currentEffect = circleChaseEffect.generateEffectObj();
                } else if (currentEffect.getEffectType() == EffectList.CHASE) {
                    ChaseEffect chaseEffect = (ChaseEffect) currentEffect.getGeneratedEffect();
                    currentEffect = chaseEffect.generateEffectObj();
                } else if (currentEffect.getEffectType() == EffectList.GRID) {
                    GridEffect gridEffect = (GridEffect) currentEffect.getGeneratedEffect();
                    currentEffect = gridEffect.generateEffectObj();
                } else if (currentEffect.getEffectType() == EffectList.NOISE) {
                    RandomNoiseEffect randomNoiseEffect = (RandomNoiseEffect) currentEffect.getGeneratedEffect();
                    currentEffect = randomNoiseEffect.generateEffectObj();
                }
            }
            effectGUI = new EffectGUI(currentEffect, currentMSec, this, selectedEffectType, false, -1);
            // Atomically replace center (and clear south)
            replaceEffectView(effectGUI.getEffectPanel(), null);
        } else {
            groupsGUI.initializeSelectionPanel();
            JPanel panel = groupsGUI.getSelectionPanel();
            replaceEffectView(panel, null);
        }
    }

    /**
     * Atomically replace the effect view center and south components so only the desired
     * panels are visible. Pass null for either argument to omit that region.
     */
    private void replaceEffectView(JComponent center, JComponent south) {
        // Preserve the NORTH component (effect options button panel) and only
        // replace the CENTER and SOUTH regions so the options stay at the top.
        LayoutManager lm = effectViewPanel.getLayout();
        if (lm instanceof BorderLayout) {
            BorderLayout bl = (BorderLayout) lm;
            Component oldCenter = bl.getLayoutComponent(effectViewPanel, BorderLayout.CENTER);
            Component oldSouth = bl.getLayoutComponent(effectViewPanel, BorderLayout.SOUTH);
            if (oldCenter != null) effectViewPanel.remove(oldCenter);
            if (oldSouth != null) effectViewPanel.remove(oldSouth);

            if (center != null) {
                effectViewPanel.add(center, BorderLayout.CENTER);
            }
            if (south != null) {
                effectViewPanel.add(south, BorderLayout.SOUTH);
            }
        } else {
            // Fallback: replace all if layout isn't BorderLayout
            effectViewPanel.removeAll();
            if (center != null) effectViewPanel.add(center, BorderLayout.CENTER);
            if (south != null) effectViewPanel.add(south, BorderLayout.SOUTH);
        }
        effectViewPanel.revalidate();
        effectViewPanel.repaint();
    }

    /**
     * Atomically replace the main content center and south components so only the desired
     * panels are visible. Pass null for either argument to omit that region.
     */
    private void replaceMainView(JComponent center, JComponent south) {
        if (mainContentPanel == null) return;
        LayoutManager lm = mainContentPanel.getLayout();
        if (lm instanceof BorderLayout) {
            BorderLayout bl = (BorderLayout) lm;
            Component oldCenter = bl.getLayoutComponent(mainContentPanel, BorderLayout.CENTER);
            Component oldSouth = bl.getLayoutComponent(mainContentPanel, BorderLayout.SOUTH);
            if (oldCenter != null) mainContentPanel.remove(oldCenter);
            if (oldSouth != null) mainContentPanel.remove(oldSouth);

            if (center != null) {
                mainContentPanel.add(center, BorderLayout.CENTER);
            }
            if (south != null) {
                mainContentPanel.add(south, BorderLayout.SOUTH);
            }
        } else {
            // Fallback: replace all if layout isn't BorderLayout
            mainContentPanel.removeAll();
            if (center != null) mainContentPanel.add(center, BorderLayout.CENTER);
            if (south != null) mainContentPanel.add(south, BorderLayout.SOUTH);
        }
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    /**
     * Build the welcome panel displayed when no project is loaded.
     * Contains a clickable/open control and a list of recently opened projects.
     */
    private JPanel buildWelcomePanel(MediaEditorGUI mediaEditorGUI) {
        JPanel pnl = new JPanel(new BorderLayout(10,10));
        pnl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        Color accent = UIManager.getColor("Component.focusColor");
        if (accent == null) accent = UIManager.getColor("TextPane.selectionBackground");
        if (accent == null) accent = Color.BLUE;

        // Top area: big open project link — use plain JLabel with bold+underline via font attrs to avoid HTML wrapping
        JLabel openLabel = new JLabel("\uD83D\uDCC4  Open a Project");
        openLabel.setForeground(accent);
        Font of = openLabel.getFont().deriveFont(Font.BOLD, 16f);
        @SuppressWarnings("unchecked") java.util.Map<TextAttribute,Object> oattrs = new java.util.HashMap<>(of.getAttributes()); oattrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        openLabel.setFont(of.deriveFont(oattrs));
        openLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openLabel.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ openProjectDialog(); }});

        JLabel newLabel = new JLabel("\uD83D\uDCC4  Create New Project");
        newLabel.setForeground(accent);
        Font nf = newLabel.getFont().deriveFont(Font.BOLD, 16f);
        @SuppressWarnings("unchecked") java.util.Map<TextAttribute,Object> nattrs = new java.util.HashMap<>(nf.getAttributes()); nattrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        newLabel.setFont(nf.deriveFont(nattrs));
        newLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newLabel.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ new SelectFileGUI(frame, mediaEditorGUI); }});

        // top making a list down
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(newLabel);
        top.add(Box.createVerticalStrut(8));
        top.add(openLabel);
        
        pnl.add(top, BorderLayout.NORTH);

        // Center: recently opened list
        JPanel recentPnl = new JPanel();
        recentPnl.setLayout(new BoxLayout(recentPnl, BoxLayout.Y_AXIS));
        recentPnl.setBorder(BorderFactory.createTitledBorder("Recently Opened"));

        java.util.List<File> recent = loadRecentProjects();
        JLabel status = new JLabel("Found " + recent.size() + " recent entries (" + userHome.toString() + "\\recent_projects.txt)");
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 10f));
        recentPnl.add(status);

        if (recent.isEmpty()) {
            JLabel none = new JLabel("No recent projects");
            //none.setFont(none.getFont().deriveFont(Font.PLAIN, 12f));
            recentPnl.add(Box.createVerticalStrut(6));
            recentPnl.add(none);
        } else {
            recentPnl.add(Box.createVerticalStrut(6));
            for (File f : recent) {
                String name = f.getName();
                String path = f.getAbsolutePath();

                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);

                // Plain label (compact): bold+underline via TextAttribute to avoid HTML wrapping
                JLabel nameLbl = new JLabel("\uD83D\uDCCE  " + name);
                nameLbl.setForeground(accent);
                Font ff = nameLbl.getFont().deriveFont(Font.BOLD);
                @SuppressWarnings("unchecked") java.util.Map<TextAttribute,Object> fattrs = new java.util.HashMap<>(ff.getAttributes()); fattrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                nameLbl.setFont(ff.deriveFont(fattrs));
                nameLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                nameLbl.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){
                    if (f.exists()){ 
                        loadProject(f); replaceMainView(footballField, scrubBarPanel);
                    } else {
                        JOptionPane.showMessageDialog(frame, "File not found: " + f.getAbsolutePath(), "Open Project", JOptionPane.ERROR_MESSAGE);
                    } 
                }});

                JLabel pathLbl = new JLabel(path);
                pathLbl.setForeground(new Color(80,80,80));

                JPanel text = new JPanel();
                text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
                text.setOpaque(false);
                text.add(nameLbl);
                text.add(pathLbl);

                // Force left alignment and limit the maximum height so BoxLayout doesn't stretch rows too much.
                row.add(text, BorderLayout.WEST);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                text.setAlignmentX(Component.LEFT_ALIGNMENT);
                Dimension pref = new Dimension(Integer.MAX_VALUE,
                        nameLbl.getPreferredSize().height + pathLbl.getPreferredSize().height + 8);
                row.setMaximumSize(pref);

                recentPnl.add(row);
                recentPnl.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scroll = new JScrollPane(recentPnl);
        scroll.setBorder(null);
        pnl.add(scroll, BorderLayout.CENTER);

        return pnl;
    }

    private static String toHex(Color c) {
        if (c == null) return "#0000FF";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Load recent projects from user folder. Only returns existing files.
     */
    private java.util.List<File> loadRecentProjects() {
        java.util.List<File> list = new ArrayList<>();
        try {
            File dir = userHome.toFile();
            if (!dir.exists()) dir.mkdirs();
            File recentFile = new File(dir, "recent_projects.txt");
            if (!recentFile.exists()) return list;
            try (BufferedReader br = new BufferedReader(new FileReader(recentFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    File f = new File(line.trim());
                    if (f.exists()) list.add(f);
                }
            }
        } catch (Exception ex) {
            // ignore, return empty
            ex.printStackTrace();
        }
        return list;
    }

    private void saveRecentProjects(java.util.List<File> list) {
        try {
            File dir = userHome.toFile();
            if (!dir.exists()) dir.mkdirs();
            File recentFile = new File(dir, "recent_projects.txt");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(recentFile, false))) {
                int count = 0;
                for (File f : list) {
                    if (f != null && f.exists()) {
                        bw.write(f.getAbsolutePath());
                        bw.newLine();
                        count++;
                        if (count >= 5) break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addToRecentProjects(File f) {
        if (f == null) return;
        try {
            java.util.List<File> list = loadRecentProjects();
            // remove same path if exists
            list.removeIf(x -> x.getAbsolutePath().equals(f.getAbsolutePath()));
            list.add(0, f);
            // trim to 5 handled in save
            saveRecentProjects(list);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Update the timeline panel to show data relevant to the currently selected performers.
     */
    private void updateTimelinePanel() {
        // Remove existing timeline data if it exists
        if (timelineGUI != null) {
            timelinePanel.remove(timelineGUI.getTimelineScrollPane());
            timelinePanel.revalidate();
            timelinePanel.repaint();
        }

        // No point in updating timeline if project has not been synced
        if (timeManager == null) {
            System.out.println("null");
            return;
        }


        // Get effects to display in the timeline. If showAllEffects is set, show every effect in project,
        // otherwise show only effects for the currently selected performers.
        HashSet<Effect> effectsSet = new HashSet<>();
        if (showAllEffects) {
            for (LEDStrip l : footballFieldPanel.drill.ledStrips) {
                for (Effect e : l.getEffects()) {
                    if (e.getGeneratedEffect() != null) {
                        effectsSet.add(e.getGeneratedEffect().generateEffectObj());
                    } else {
                        effectsSet.add(e);
                    }
                }
            }
        } else {
            for (LEDStrip l : footballFieldPanel.selectedLEDStrips) {
                for (Effect e : l.getEffects()) {
                    effectsSet.add(e.getGeneratedEffect().generateEffectObj());
                }
            }
        }
        ArrayList<Effect> effectsList = new ArrayList<>(effectsSet);
        if (timelineGUI == null) {
            timelineGUI = new TimelineGUI(effectsList, count2RFTrigger, timeManager, scrubBarGUI.getLastCount());
            timelineGUI.setTimelineListener(this);
            timelineGUI.setShowAllEffects(showAllEffects);
            timelinePanel.add(timelineGUI.getTimelineScrollPane());
        } else {
            // Reuse existing timeline GUI so we don't rebuild UI state on every performer selection change
            timelineGUI.setShowAllEffects(showAllEffects);
            timelineGUI.updateData(effectsList, count2RFTrigger, scrubBarGUI.getLastCount());
            // If the scrollpane isn't already in the panel (first run), add it
            if (Arrays.stream(timelinePanel.getComponents()).noneMatch(c -> c == timelineGUI.getTimelineScrollPane())) {
                timelinePanel.add(timelineGUI.getTimelineScrollPane());
            }
        }
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }

    /**
     * Save the current project to a .emrick file.
     * @param path The file location to save the project.
     * @param archivePaths The locations of the .3dz files in user files when the project is loaded.
     */
    private void saveProject(File path, ArrayList<File> archivePaths) {
        ProjectFile pf;

        ArrayList<SelectionGroupGUI.SelectionGroup> groupsList = new ArrayList<>();
        for(SelectionGroupGUI.SelectionGroup group: groupsGUI.getGroups()){
            SelectionGroupGUI.SelectionGroup toAdd = group.clone();
            toAdd.setTitleButton(null);
            groupsList.add(toAdd);
        }

        ArrayList<Performer> recoverPerformers = new ArrayList<>();
        for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
            recoverPerformers.add(ledStrip.getPerformer());
            ledStrip.setPerformer(null);
        }

        ArrayList<String> archiveNames = new ArrayList<>();
        if (archivePaths == null) {
            System.out.println("Archive Paths Null!");
        }
        for (File f : archivePaths) {
            archiveNames.add(f.getName());
        }
        if (archiveNames.size() <= 0) {
            System.out.println("SIZE <= 0 3421" + archiveNames.size());
        }
        if (this.effectManager != null) {
            pf = new ProjectFile(footballFieldPanel.drill, archiveNames, timeSync, startDelay, count2RFTrigger, effectManager.getIds(), groupsList);
        } else {
            pf = new ProjectFile(footballFieldPanel.drill, archiveNames, timeSync, startDelay, count2RFTrigger, null, groupsList);
        }
        String g = gson.toJson(pf);

        writeSysMsg("saving to `" + path + "`");

        String jsonName = path.getName();
        jsonName = jsonName.substring(0, jsonName.indexOf(".emrick")) + ".json";
        File dir = new File(PathConverter.pathConverter("show_data/", false));
        dir.mkdirs();
        File[] cleanJson = dir.listFiles();
        for (File f : cleanJson) {
            if (f.getName().endsWith(".json")) {
                f.delete();
            }
        }

        try {
            FileWriter w = new FileWriter(PathConverter.pathConverter("show_data/" + jsonName, false));
            w.write(g);
            w.close();
            emrickPath = path;
        } catch (IOException e) {
            writeSysMsg("Failed to save to `" + path + "`.");
            throw new RuntimeException(e);
        }

        for (int i = 0; i < recoverPerformers.size(); i++) {
            footballFieldPanel.drill.ledStrips.get(i).setPerformer(recoverPerformers.get(i));
        }

        File showDataDir = new File(PathConverter.pathConverter("show_data/", false));
        showDataDir.mkdirs();
        File[] saveFiles = showDataDir.listFiles();
        ArrayList<String> files = new ArrayList<>();
        for (File f : saveFiles) {
            if (!f.isDirectory()) {
                files.add(f.getAbsolutePath());
            }
        }
        Unzip.zip(files, path.getAbsolutePath(), false);

        writeSysMsg("Saved project to `" + path + "`.");
    }

    /**
     * Calculates the time between the end of the previous effect/RF Trigger and the start of the current effect.
     * @param index Current RF Trigger index
     * @param e Effect to find the time before
     * @param effects List of effects on the relevant led strip
     * @param timesMS A list of times in milliseconds that RF Triggers occur
     * @return The time in milliseconds between the current effect and the previous effect/RF Trigger.
     */
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
        if (effects.get(index-1).getStartTimeMSec() >= lastTrigger) {
            return e.getStartTimeMSec() - effects.get(index-1).getEndTimeMSec();
        } else {
            return e.getStartTimeMSec() - lastTrigger;
        }
    }

    /**
     * Calculates the time between the end of the current effect and the start of the next effect/RF Trigger.
     * @param index Current RF Trigger index
     * @param e Effect to find the time before
     * @param effects List of effects on the relevant led strip
     * @param timesMS A list of times in milliseconds that RF Triggers occur
     * @return The time in milliseconds between the current effect and the next effect/RF Trigger.
     */
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
        if (nextTrigger <= e.getStartTimeMSec()) {
            nextTrigger = -1;
        }
        if (effects.get(index+1).getStartTimeMSec() < nextTrigger || nextTrigger == -1) {
            if (effects.get(index+1).getStartTimeMSec() >= e.getEndTimeMSec()) {
                return effects.get(index+1).getStartTimeMSec() - e.getEndTimeMSec();
            } else {
                return Long.MAX_VALUE;
            }
        } else {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Calculates the index of the RF Trigger immediately before the relevant effect.
     * @param e The relevant effect.
     * @param timesMS A list of times in milliseconds that RF Triggers occur
     * @return The index of the RF Trigger immediately before the relevant effect.
     */
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

    /**
     * Multithreaded export of firmware-readable packet data to a .pkt file.
     * @param path Location to write .pkt file.
     */
    private void exportPackets(File path) {
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
            String out = "";
            File dir = new File(PathConverter.pathConverter("tmp/", false));
            dir.mkdirs();
            ArrayList<String> files = new ArrayList<>();
            ArrayList<LEDStrip> list0 = new ArrayList<>();
            ArrayList<LEDStrip> list1 = new ArrayList<>();
            ArrayList<LEDStrip> list2 = new ArrayList<>();
            ArrayList<LEDStrip> list3 = new ArrayList<>();
            ArrayList<LEDStrip> list4 = new ArrayList<>();
            ArrayList<LEDStrip> list5 = new ArrayList<>();
            ArrayList<LEDStrip> list6 = new ArrayList<>();
            ArrayList<LEDStrip> list7 = new ArrayList<>();
            for (int k = 0; k < footballFieldPanel.drill.ledStrips.size(); k++) {
                LEDStrip l = footballFieldPanel.drill.ledStrips.get(k);
                l.getEffects().sort(Comparator.comparingLong(Effect::getStartTimeMSec));
                File curr = new File(PathConverter.pathConverter("tmp/" + l.getId(), false));
                curr.createNewFile();
                files.add(curr.getAbsolutePath());

                switch (k % 8) {
                    case 0: list0.add(l); break;
                    case 1: list1.add(l); break;
                    case 2: list2.add(l); break;
                    case 3: list3.add(l); break;
                    case 4: list4.add(l); break;
                    case 5: list5.add(l); break;
                    case 6: list6.add(l); break;
                    case 7: list7.add(l); break;
                }
            }
            Thread[] threads = new Thread[8];
            threads[0] = new Thread(new PacketExport(list0, timesMS));
            threads[1] = new Thread(new PacketExport(list1, timesMS));
            threads[2] = new Thread(new PacketExport(list2, timesMS));
            threads[3] = new Thread(new PacketExport(list3, timesMS));
            threads[4] = new Thread(new PacketExport(list4, timesMS));
            threads[5] = new Thread(new PacketExport(list5, timesMS));
            threads[6] = new Thread(new PacketExport(list6, timesMS));
            threads[7] = new Thread(new PacketExport(list7, timesMS));
            for (i = 0; i < 8; i++) {
                threads[i].start();
            }
            for (i = 0; i < 8; i++) {
                threads[i].join();
            }
            Unzip.zip(files, path.getAbsolutePath(), true);
            deleteDirectory(dir);
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes a system message in the top right of the screen
     * @param msg Message to be written
     */
    private void writeSysMsg(String msg) {
        clearSysMsg.stop();
        sysMsg.setText(msg);
        clearSysMsg.start();
    }

    @Override
    public void onRFSignal(int i) {
        SerialTransmitter st = comPortPromptFlow();

        if (st != null) {
            st.writeSet(i, isLightBoardMode);
        }
    }

    @Override
    public synchronized void onRequestComplete(int id) {
        if (id != -1) {
            requestIDs.add(id);
            programmingProgressBar.setValue(requestIDs.size());
            programmingProgressBar.setString(programmingProgressBar.getValue() + "/" + programmingProgressBar.getMaximum());
            programmingProgressBar.setStringPainted(true);

            programmingTracker.addCompletedStrip(id);
            programmingTracker.revalidate();
            programmingTracker.repaint();

            updateProgressLabel(programmingProgressLabel, requestIDs, programmingTracker.getAlreadyProgrammedStrips(), footballFieldPanel.drill.ledStrips.size());
        }

        int highestID = footballFieldPanel.drill.ledStrips.size() - 1;
        if (currentID <= highestID) {
            currentID++;
        }
        noRequestTimer.stop();

        boolean allReceived = true;
        for (LEDStrip l : footballFieldPanel.drill.ledStrips) {
            if (!requestIDs.contains(l.getId())) {
                allReceived = false;
                break;
            }
        }
        if (!allReceived) {
            if (lastRun + 20000 < System.currentTimeMillis()) {
                SerialTransmitter serialTransmitter = comPortPrompt("Transmitter");
                serialTransmitter.enterProgMode(ssid, password, port, currentID, token, verificationColor, lightBoardMode);
                lastRun = System.currentTimeMillis();
            }
            noRequestTimer.setDelay(10000);
            noRequestTimer.start();
        } else {
            server.stop(0);
            runWebServer.setEnabled(true);
            runLightBoardWebServer.setEnabled(true);
            stopWebServer.setEnabled(false);
            server = null;
            requestIDs = null;
            webServerFrame.dispose();
        }
    }

    @Override
    public void onExitConfig() {
        replaceMainView(footballField, scrubBarPanel);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    @Override
    public boolean onNewFileSelect(File drill, File archive) {
        ImportArchive importArchive = new ImportArchive(new ImportListener() {
            @Override
            public void onBeginImport() {}
            @Override
            public void onImport() {}

            @Override
            public void onFileSelect(ArrayList<File> archivePaths, File csvFile) {

            }

            @Override
            public void onAudioImport(ArrayList<File> audioFiles) {

            }

            @Override
            public void onDrillImport(String drill) {
                Drill newDrill = DrillParser.parseWholeDrill(DrillParser.extractText(drill));
                Drill oldDrill = footballFieldPanel.drill;
                boolean same = true;
                for (int i = 0 ; i < oldDrill.sets.size(); i++) {
                    if (newDrill.sets.size() > i) {
                        if (!newDrill.sets.get(i).equals(oldDrill.sets.get(i))
                            || newDrill.sets.get(i).duration != oldDrill.sets.get(i).duration) {
                            same = false;
                            break;
                        }
                    } else {
                        same = false;
                        break;
                    }
                }
                if (oldDrill.sets.size() != newDrill.sets.size()) {
                    same = false;
                }
                /*
                * Strategy
                * Build edit list with insert new, add existing, update existing, and delete existing
                * update should use add or delete process depending on change
                * add existing is nop
                *
                * Build a sorted list of events from rf triggers and effects built from generated effects
                * */

                if (!same) {
                    ArrayList<EditItem> editList = new ArrayList<>();
                    int insertCount = 0;
                    int deleteCount = 0;
                    for (int i = 0; i < oldDrill.sets.size(); i++) {
                        if (newDrill.sets.contains(oldDrill.sets.get(i))) {
                            if (!oldDrill.sets.get(i).label.equals(newDrill.sets.get(i+insertCount).label)) {
                                while (!oldDrill.sets.get(i).label.equals(newDrill.sets.get(i+insertCount).label)) {
                                    editList.add(new EditItem("INSERT", newDrill.sets.get(i+insertCount)));
                                    insertCount++;
                                }
                            } else if (oldDrill.sets.get(i).duration != newDrill.sets.get(i+insertCount).duration) {
                                if (editList.get(editList.size() - 1).operation.equals("DELETE")) {
                                    if (oldDrill.sets.get(i - 1).duration != newDrill.sets.get(i+insertCount).duration) {
                                        editList.add(new EditItem("UPDATE", newDrill.sets.get(i + insertCount)));
                                    } else {
                                        editList.add(new EditItem("NOP", oldDrill.sets.get(i)));
                                    }
                                } else {
                                    editList.add(new EditItem("UPDATE", newDrill.sets.get(i + insertCount)));
                                }
                            } else {
                                editList.add(new EditItem("NOP", oldDrill.sets.get(i)));
                            }
                        } else {
                            editList.add(new EditItem("DELETE", oldDrill.sets.get(i)));
                            insertCount--;
                            deleteCount++;
                        }
                    }

                    //reverse traversal of editList

                    for (int i = editList.size() - 1; i >= 0; i--) {
                        switch (editList.get(i).getOperation()) {
                            case "INSERT": {
                                /*
                                NOT CURRENTLY FUNCTIONAL - DO NOT USE
                                 */
                                int modIndex = 0;
                                for (Set set : newDrill.sets) {
                                    if (set.equals(editList.get(i).set)) {
                                        break;
                                    } else if (oldDrill.sets.contains(set)) {
                                        modIndex = set.index;
                                    }
                                }
                                long sliceMsec = 0;
                                int sliceCount = 0;
                                JLabel tempoLabel = new JLabel("Enter tempo for set: " + editList.get(i).set.label);
                                JTextField tempoField = new JTextField();
                                Object[] input = {tempoLabel, tempoField};
                                JOptionPane.showInputDialog(frame, input);
                                long durationCounts = editList.get(i).set.duration;
                                long durationMsec = (long)((float) durationCounts / Float.parseFloat(tempoField.getText()) * 60000);
                                for (int j = 0; j < oldDrill.sets.size(); j++) { // find slice time in seconds/counts
                                    sliceMsec += (long) (timeSync.get(j).getValue() * 1000);
                                    sliceCount += oldDrill.sets.get(j+1).duration;
                                    if (j == modIndex - 1) {
                                        break;
                                    }
                                }

                                ArrayList<Integer> removedIDs = new ArrayList<>();
                                for (int j = 0; j < oldDrill.ledStrips.size(); j++) {
                                    ArrayList<Effect> removeEffects = new ArrayList<>();
                                    LEDStrip l = oldDrill.ledStrips.get(j);
                                    for (Effect e : l.getEffects()) {
                                        if (e.getStartTimeMSec() < sliceMsec && e.getEndTimeMSec() > sliceMsec) {
                                            removeEffects.add(e);
                                            if (!removedIDs.contains(e.getId())) {
                                                removedIDs.add(e.getId());
                                            }
                                        } else if (e.getStartTimeMSec() > sliceCount) {
                                            e.setStartTimeMSec(e.getStartTimeMSec() + durationMsec);
                                            e.setEndTimeMSec(e.getEndTimeMSec() + durationMsec);
                                            e.getGeneratedEffect().setStartTime(e.getGeneratedEffect().getStartTime() + durationMsec);
                                            e.getGeneratedEffect().setEndTime(e.getGeneratedEffect().getEndTime() + durationMsec);
                                        }
                                    }
                                    for (Effect e : removeEffects) {
                                        l.getEffects().remove(e);
                                    }
                                }
                                ArrayList<Integer> ids = effectManager.getIds();
                                for (Integer rem: removedIDs) {
                                    ids.remove(rem);
                                }

                                timeSync.add(modIndex + 1, new SyncTimeGUI.Pair(editList.get(i).set.label, durationMsec));

                                ArrayList<RFTrigger> moveRFTriggers = new ArrayList<>();
                                for (RFTrigger rfTrigger : count2RFTrigger.values()) {
                                    if (rfTrigger.getCount() > sliceCount) {
                                        moveRFTriggers.add(rfTrigger);
                                    }
                                }
                                for (RFTrigger rfTrigger : moveRFTriggers) {
                                    int oldCount = rfTrigger.getCount();
                                    rfTrigger.setCount(oldCount + sliceCount);
                                    rfTrigger.setTimestampMillis(rfTrigger.getTimestampMillis() + durationMsec);
                                    count2RFTrigger.remove(oldCount);
                                    count2RFTrigger.put(rfTrigger.getCount(), rfTrigger);
                                }

                                break;
                            }
                            case "UPDATE": {
                                int modIndex = 0;
                                for (Set set : oldDrill.sets) {
                                    if (set.label.equals(editList.get(i).set.label)) {
                                        modIndex = set.index;
                                        break;
                                    }
                                }
                                long startMsec = 0;
                                long endMsec = 0;
                                int startCount = 0;
                                int endCount = 0;
                                int durationCounts = 0;
                                long durationMsec = 0;
                                for (int j = 0; j < oldDrill.sets.size(); j++) { // find start/end time in seconds/counts
                                    endMsec += (long) (timeSync.get(j).getValue() * 1000);
                                    endCount += oldDrill.sets.get(j+1).duration;
                                    if (j < modIndex - 1) {
                                        startMsec += (long) (timeSync.get(j).getValue() * 1000);
                                        startCount += oldDrill.sets.get(j+1).duration;
                                    } else {
                                        break;
                                    }
                                }
                                durationCounts = endCount - startCount;
                                durationMsec = endMsec - startMsec;
                                float rate = (((float) durationMsec) / 1000) / (float) durationCounts;
                                int newDurationCounts = editList.get(i).set.duration;
                                long newDurationMsec = (long)((float)newDurationCounts * rate * 1000);
                                int countDiff = newDurationCounts - durationCounts;
                                long msecDiff = newDurationMsec - durationMsec;
                                if (countDiff > 0) {
                                    int sliceCount = endCount;
                                    long sliceMsec = endMsec;
                                    durationCounts += countDiff;
                                    durationMsec += msecDiff;
                                    ArrayList<Integer> removedIDs = new ArrayList<>();
                                    for (int j = 0; j < oldDrill.ledStrips.size(); j++) {
                                        ArrayList<Effect> removeEffects = new ArrayList<>();
                                        LEDStrip l = oldDrill.ledStrips.get(j);
                                        for (Effect e : l.getEffects()) {
                                            if (e.getStartTimeMSec() < sliceMsec && e.getEndTimeMSec() > sliceMsec) {
                                                removeEffects.add(e);
                                                if (!removedIDs.contains(e.getId())) {
                                                    removedIDs.add(e.getId());
                                                }
                                            } else if (e.getStartTimeMSec() > sliceMsec) {
                                                e.setStartTimeMSec(e.getStartTimeMSec() + msecDiff);
                                                e.setEndTimeMSec(e.getEndTimeMSec() + msecDiff);
                                                e.getGeneratedEffect().setStartTime(e.getGeneratedEffect().getStartTime() + msecDiff);
                                                e.getGeneratedEffect().setEndTime(e.getGeneratedEffect().getEndTime() + msecDiff);
                                            }
                                        }
                                        for (Effect e : removeEffects) {
                                            l.getEffects().remove(e);
                                        }
                                    }
                                    ArrayList<Integer> ids = effectManager.getIds();
                                    for (Integer rem: removedIDs) {
                                        ids.remove(rem);
                                    }

                                    timeSync.set(modIndex - 1, new SyncTimeGUI.Pair(oldDrill.sets.get(modIndex - 1).label, ((float) durationMsec) / 1000));

                                    ArrayList<RFTrigger> moveRFTriggers = new ArrayList<>();
                                    for (RFTrigger rfTrigger : count2RFTrigger.values()) {
                                        if (rfTrigger.getCount() > sliceCount) {
                                            moveRFTriggers.add(rfTrigger);
                                        }
                                    }
                                    for (RFTrigger rfTrigger : moveRFTriggers) {
                                        int oldCount = rfTrigger.getCount();
                                        rfTrigger.setCount(oldCount + sliceCount);
                                        rfTrigger.setTimestampMillis(rfTrigger.getTimestampMillis() + msecDiff);
                                        count2RFTrigger.remove(oldCount);
                                        count2RFTrigger.put(rfTrigger.getCount(), rfTrigger);
                                    }
                                } else {
                                    startCount = endCount + countDiff;
                                    startMsec = endMsec + msecDiff;
                                    durationMsec += msecDiff;
                                    durationCounts += countDiff;
                                    ArrayList<Integer> removedIDs = new ArrayList<>();
                                    /*
                                    1. Find start and end time/count and thus duration
                                    2. Loop though all effects/triggers for all performers and remove all overlapping effects
                                    3. For non-overlapping effects, decrease their start/end times by the duration if they
                                       are after the deleted set end time
                                    4. Remove relevant item from timeSync
                                     */
                                    for (int j = 0; j < footballFieldPanel.drill.ledStrips.size(); j++) { // adjust effects to match new sets
                                        LEDStrip l = footballFieldPanel.drill.ledStrips.get(j);
                                        ArrayList<Effect> removeEffects = new ArrayList<>();
                                        for (Effect e : l.getEffects()) {
                                            if (e.getStartTimeMSec() < endMsec && e.getEndTimeMSec() > startMsec) { // check if  effect overlaps with removed set
                                                if (!removedIDs.contains(e.getId())) {
                                                    removedIDs.add(e.getId());
                                                }
                                                removeEffects.add(e);
                                            } else if (e.getStartTimeMSec() > endMsec) { // check if effect is after removed set
                                                // update effect and generated effect times to subtract duration of removed set
                                                e.setStartTimeMSec(e.getStartTimeMSec() - (Math.abs(msecDiff) - 2));
                                                e.setEndTimeMSec(e.getEndTimeMSec() - (Math.abs(msecDiff) - 2));
                                                e.getGeneratedEffect().setStartTime(e.getGeneratedEffect().getStartTime() - (Math.abs(msecDiff) - 2));
                                                e.getGeneratedEffect().setEndTime(e.getGeneratedEffect().getEndTime() - (Math.abs(msecDiff) - 2));
                                            }
                                        }
                                        for (Effect e : removeEffects) { // remove overlapped effects
                                            l.getEffects().remove(e);
                                        }
                                    }
                                    ArrayList<Integer> ids = effectManager.getIds();
                                    for (Integer rem : removedIDs) { // delete effect ids of removed effects
                                        ids.remove(rem);
                                    }

                                    timeSync.set(modIndex-1, new SyncTimeGUI.Pair(oldDrill.sets.get(modIndex-1).label, (float)durationMsec / 1000));

                                    for (int j = startCount; j < endCount; j++) { // remove rf triggers that existed in the removed set
                                        count2RFTrigger.remove(j);
                                    }

                                    ArrayList<RFTrigger> moveRFTriggers = new ArrayList<>();

                                    for (RFTrigger rfTrigger : count2RFTrigger.values()) {
                                        if (rfTrigger.getCount() > endCount + Math.abs(countDiff)) {
                                            moveRFTriggers.add(rfTrigger);
                                        }
                                    }
                                    for (RFTrigger rfTrigger : moveRFTriggers) {
                                        int oldCount = rfTrigger.getCount();
                                        rfTrigger.setCount(oldCount - (Math.abs(countDiff)));
                                        rfTrigger.setTimestampMillis(rfTrigger.getTimestampMillis() - (Math.abs(msecDiff) - 2));
                                        count2RFTrigger.remove(oldCount);
                                        count2RFTrigger.put(rfTrigger.getCount(), rfTrigger);
                                    }
                                }

                                break;
                            }
                            case "DELETE": {
                                int modIndex = 0;
                                for (Set set : oldDrill.sets) {
                                    if (set.label.equals(editList.get(i).set.label)) {
                                        modIndex = set.index;
                                        break;
                                    }
                                }
                                long startMsec = 0;
                                long endMsec = 0;
                                int startCount = 0;
                                int endCount = 0;
                                ArrayList<Integer> removedIDs = new ArrayList<>();
                                if (i != editList.size() - 1) {
                                    /*
                                    1. Find start and end time/count and thus duration
                                    2. Loop though all effects/triggers for all performers and remove all overlapping effects
                                    3. For non-overlapping effects, decrease their start/end times by the duration if they
                                       are after the deleted set end time
                                    4. Remove relevant item from timeSync
                                     */
                                    for (int j = 0; j < oldDrill.sets.size(); j++) { // find start/end time in seconds/counts
                                        endMsec += (long) (timeSync.get(j).getValue() * 1000);
                                        endCount += oldDrill.sets.get(j+1).duration;
                                        if (j < modIndex) {
                                            startMsec += (long) (timeSync.get(j).getValue() * 1000);
                                            startCount += oldDrill.sets.get(j+1).duration;
                                        } else {
                                            break;
                                        }
                                    }
                                    for (int j = 0; j < footballFieldPanel.drill.ledStrips.size(); j++) { // adjust effects to match new sets
                                        LEDStrip l = footballFieldPanel.drill.ledStrips.get(j);
                                        ArrayList<Effect> removeEffects = new ArrayList<>();
                                        for (Effect e : l.getEffects()) {
                                            if (e.getStartTimeMSec() < endMsec && e.getEndTimeMSec() > startMsec) { // check if  effect overlaps with removed set
                                                if (!removedIDs.contains(e.getId())) {
                                                    removedIDs.add(e.getId());
                                                }
                                                removeEffects.add(e);
                                            } else if (e.getStartTimeMSec() > endMsec) { // check if effect is after removed set
                                                // update effect and generated effect times to subtract duration of removed set
                                                e.setStartTimeMSec(e.getStartTimeMSec() - (endMsec - startMsec - 2));
                                                e.setEndTimeMSec(e.getEndTimeMSec() - (endMsec - startMsec - 2));
                                                e.getGeneratedEffect().setStartTime(e.getGeneratedEffect().getStartTime() - (endMsec - startMsec - 2));
                                                e.getGeneratedEffect().setEndTime(e.getGeneratedEffect().getEndTime() - (endMsec - startMsec - 2));
                                            }
                                        }
                                        for (Effect e : removeEffects) { // remove overlapped effects
                                            l.getEffects().remove(e);
                                        }
                                    }
                                    ArrayList<Integer> ids = effectManager.getIds();
                                    for (Integer rem : removedIDs) { // delete effect ids of removed effects
                                        ids.remove(rem);
                                    }
                                    timeSync.remove(i); // remove timesync item that corresponds to removed effect
                                    for (int j = startCount; j < endCount; j++) { // remove rf triggers that existed in the removed set
                                        count2RFTrigger.remove(j);
                                    }

                                    ArrayList<RFTrigger> moveRFTriggers = new ArrayList<>();

                                    for (RFTrigger rfTrigger : count2RFTrigger.values()) {
                                        if (rfTrigger.getCount() > startCount) {
                                            moveRFTriggers.add(rfTrigger);
                                        }
                                    }
                                    for (RFTrigger rfTrigger : moveRFTriggers) {
                                        int oldCount = rfTrigger.getCount();
                                        rfTrigger.setCount(oldCount - (endCount - startCount));
                                        rfTrigger.setTimestampMillis(rfTrigger.getTimestampMillis() - (endMsec - startMsec - 2));
                                        count2RFTrigger.remove(oldCount);
                                        count2RFTrigger.put(rfTrigger.getCount(), rfTrigger);
                                    }
                                } else {
/*
                                    1. Find start and end time/count and thus duration
                                    2. Loop though all effects/triggers for all performers and remove all overlapping effects
                                    3. Remove relevant item from timeSync
                                     */
                                    for (int j = 0; j < oldDrill.sets.size() - 1; j++) { // find start/end time in seconds/counts
                                        endMsec += (long) (timeSync.get(j).getValue() * 1000);
                                        endCount += oldDrill.sets.get(j+1).duration;
                                        if (j < modIndex - 1) {
                                            startMsec += (long) (timeSync.get(j).getValue() * 1000);
                                            startCount += oldDrill.sets.get(j+1).duration;
                                        } else {
                                            break;
                                        }
                                    }
                                    for (int j = 0; j < footballFieldPanel.drill.ledStrips.size(); j++) { // adjust effects to match new sets
                                        LEDStrip l = footballFieldPanel.drill.ledStrips.get(j);
                                        ArrayList<Effect> removeEffects = new ArrayList<>();
                                        for (Effect e : l.getEffects()) {
                                            if (e.getStartTimeMSec() < endMsec && e.getEndTimeMSec() > startMsec) { // check if  effect overlaps with removed set
                                                if (!removedIDs.contains(e.getId())) {
                                                    removedIDs.add(e.getId());
                                                }
                                                removeEffects.add(e);
                                            } else if (e.getStartTimeMSec() > endMsec) { // check if effect is after removed set
                                                // update effect and generated effect times to subtract duration of removed set
                                                e.setStartTimeMSec(e.getStartTimeMSec() - (endMsec - startMsec - 2));
                                                e.setEndTimeMSec(e.getEndTimeMSec() - (endMsec - startMsec - 2));
                                                e.getGeneratedEffect().setStartTime(e.getGeneratedEffect().getStartTime() - (endMsec - startMsec - 2));
                                                e.getGeneratedEffect().setEndTime(e.getGeneratedEffect().getEndTime() - (endMsec - startMsec - 2));
                                            }
                                        }
                                        for (Effect e : removeEffects) { // remove overlapped effects
                                            l.getEffects().remove(e);
                                        }
                                    }
                                    ArrayList<Integer> ids = effectManager.getIds();
                                    for (Integer rem : removedIDs) { // delete effect ids of removed effects
                                        ids.remove(rem);
                                    }
                                    timeSync.remove(i); // remove timesync item that corresponds to removed effect
                                    for (int j = startCount; j < endCount; j++) { // remove rf triggers that existed in the removed set
                                        count2RFTrigger.remove(j);
                                    }

                                    ArrayList<RFTrigger> moveRFTriggers = new ArrayList<>();

                                    for (RFTrigger rfTrigger : count2RFTrigger.values()) {
                                        if (rfTrigger.getCount() > startCount) {
                                            moveRFTriggers.add(rfTrigger);
                                        }
                                    }
                                    for (RFTrigger rfTrigger : moveRFTriggers) {
                                        int oldCount = rfTrigger.getCount();
                                        rfTrigger.setCount(oldCount - (endCount - startCount));
                                        rfTrigger.setTimestampMillis(rfTrigger.getTimestampMillis() - (endMsec - startMsec - 2));
                                        count2RFTrigger.remove(oldCount);
                                        count2RFTrigger.put(rfTrigger.getCount(), rfTrigger);
                                    }
                                }
                                break;
                            }
                        }
                    }
                    oldDrill.sets = newDrill.sets;
                    onSync(timeSync, 0);
                    rebuildPageTabCounts();
                    setupEffectView(effectManager.getIds());
                    updateTimelinePanel();


                }

                oldDrill.coordinates = newDrill.coordinates;
                for (Performer p : oldDrill.performers) {
                    p.loadCoordinates(oldDrill.coordinates);
                }
                footballFieldPanel.repaint();
            }

            @Override
            public void onConcatAudioImport(ArrayList<File> audioFiles) {

            }
        });
        String aPath = null;
        String dPath = null;
        if (archive != null) {
            aPath = archive.getAbsolutePath();
        }
        if (drill != null) {
            dPath = drill.getAbsolutePath();
        }
        scrubBarGUI.setScrub(0);
        ArrayList<File> paths = new ArrayList<>();
        if (aPath != null) {
            paths.add(new File(aPath));
        }
        importArchive.fullImport(paths, dPath);
        return true;
    }

    private class EditItem {
        private String operation;
        private Set set;

        public EditItem(String operation, Set set) {
            this.operation = operation;
            this.set = set;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public Set getSet() {
            return set;
        }

        public void setSet(Set set) {
            this.set = set;
        }
    }

    /**
     * Object used to track the progress of programming led strips using the web server
     */
    private class ProgrammingTracker extends JPanel {
        private ArrayList<LEDStrip> allStrips;
        private HashSet<Integer> completedStrips;
        private HashSet<Integer> alreadyProgrammedStrips = new HashSet<>();
        private ArrayList<ProgrammableItem> items;
        private boolean painting = false;
        private File completedStripsFile;

        public ProgrammingTracker(ArrayList<LEDStrip> allStrips, HashSet<Integer> completedStrips) {
            this.allStrips = allStrips;
            this.completedStrips = completedStrips;
            this.items = new ArrayList<>();
            this.completedStripsFile = new File(PathConverter.pathConverter("programmedBoards.txt", false));

            loadCompletedStrips();

            this.setLayout(new GridLayout(20, allStrips.size() / 20 + 1));

            for (LEDStrip l : allStrips) {
                ProgrammableItem item = new ProgrammableItem(l);
                if (alreadyProgrammedStrips.contains(l.getId())) {
                    item.setAlreadyProgrammed(true);
                }
                items.add(item);
                this.add(item);
            }

            for (Integer l : completedStrips) {
                setItemCompleted(l);
            }
        }

        private void loadCompletedStrips() {
            if (!completedStripsFile.exists()) {
                // Create new file with token
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(completedStripsFile))) {
                    writer.write("Token: " + token);
                    writer.newLine();
                } catch (IOException e) {
                    System.out.println("Error creating programmedBoards.txt.");
                    e.printStackTrace();
                }
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(completedStripsFile))) {
                String firstLine = reader.readLine();

                if (firstLine == null || !firstLine.startsWith("Token:")) {
                    rewriteFileWithTokenOnly();
                    return;
                }

                int storedToken = Integer.parseInt(firstLine.substring("Token:".length()).trim());
                if (token != storedToken) {
                    System.out.println("Token mismatch. Rewriting file with current token.");
                    rewriteFileWithTokenOnly();
                    return;
                }

                // Load remaining lines as board IDs
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        alreadyProgrammedStrips.add(Integer.parseInt(line.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading programmedBoards.txt");
                e.printStackTrace();
            }
        }

        private void rewriteFileWithTokenOnly() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(completedStripsFile))) {
                writer.write("Token: " + token);
                writer.newLine();
            } catch (IOException e) {
                System.out.println("Error rewriting file with new token.");
                e.printStackTrace();
            }
        }

        public void addCompletedStrip(Integer ledStripId) {
            if (alreadyProgrammedStrips.contains(ledStripId)) {
                return;
            }
            setItemCompleted(ledStripId);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(completedStripsFile, true))) {
                writer.write(String.valueOf(ledStripId));
                writer.newLine();
            } catch (IOException e) {
                System.out.println("Error writing to programmedBoards.txt");
                e.printStackTrace();
            }

            if (!painting) {
                painting = true;
                repaint();
            }
        }

        private void setItemCompleted(Integer ledStripId) {
            for (ProgrammableItem item : items) {
                if (item.getLedStrip().getId() == ledStripId) {
                    item.setProgrammed(true);
                    item.setAlreadyProgrammed(false); // override green with blue
                }
            }
        }

        public java.util.Set<Integer> getAlreadyProgrammedStrips() {
            return new HashSet<>(alreadyProgrammedStrips);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            painting = false;
        }

        private class ProgrammableItem extends JPanel {
            private LEDStrip ledStrip;
            private boolean programmed;
            private boolean alreadyProgrammed;

            public ProgrammableItem(LEDStrip ledStrip) {
                this.ledStrip = ledStrip;
                this.programmed = false;
                this.alreadyProgrammed = false;
                this.setMaximumSize(new Dimension(50, 50));
                this.setPreferredSize(new Dimension(50, 50));
                this.setMinimumSize(new Dimension(50, 50));
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (programmed) {
                    g.setColor(new Color(0, 150, 255)); // Blue text
                } else if (alreadyProgrammed) {
                    g.setColor(new Color(0, 200, 0)); // Green text
                } else {
                    g.setColor(Color.RED); // Red text
                }

                g.drawString(ledStrip.getLabel(), 10, 20);
            }

            public LEDStrip getLedStrip() {
                return ledStrip;
            }

            public void setLedStrip(LEDStrip ledStrip) {
                this.ledStrip = ledStrip;
            }

            public boolean isProgrammed() {
                return programmed;
            }

            public void setProgrammed(boolean programmed) {
                this.programmed = programmed;
            }

            public void setAlreadyProgrammed(boolean alreadyProgrammed) {
                this.alreadyProgrammed = alreadyProgrammed;
            }
        }
    }

    /**
     * Runnable object used to split the load of packet export.
     */
    private class PacketExport implements Runnable {
        private ArrayList<LEDStrip> ledStrips;
        private Long[] timesMS;
        public PacketExport(ArrayList<LEDStrip> ledStrips, Long[] timesMS) {
            this.ledStrips = ledStrips;
            this.timesMS = timesMS;
        }

        @Override
        public void run() {
            try {
                String out = "";
                int a = 0;
                for (LEDStrip l : ledStrips) {
                    a++;
                    File curr = new File(PathConverter.pathConverter("tmp/" + l.getId(), false));

                    BufferedWriter bfw = new BufferedWriter(new FileWriter(curr));

                    l.sortEffects();
                    if (l.getEffects().size() > 0) {
                        out += "Pkt_count: " + l.getEffects().size() + "\n";
                        for (int i = 0; i < l.getEffects().size(); i++) {
                            Effect e = l.getEffects().get(i);
                            int flags = 0;
                            if (timeBeforeEffect(i, e, l.getEffects(), timesMS) > 1 || e.isDO_DELAY()) {
                                flags += DO_DELAY;
                                if (e.isDO_DELAY() && timeBeforeEffect(i, e, l.getEffects(), timesMS) > 1) {
                                    out += "Size: 0, Strip_id: " + l.getId() + ", Set_id: " + getEffectTriggerIndex(e, timesMS)
                                            + ", Flags: 24, Start_color: 0, 0, 0, End_color: 0, 0, 0, Delay: " + timeBeforeEffect(i, e, l.getEffects(), timesMS)
                                            + ", Duration: 0, Function: 0, Timeout: 0\n";
                                    int count = Integer.valueOf(out.substring(out.indexOf(" ") + 1, out.indexOf(",")));
                                    out = out.substring(0, out.indexOf(" ") + 1) + (count + 1) + out.substring(out.indexOf(","));
                                }
                            }
                            if (timeAfterEffect(i, e, l.getEffects(), timesMS) == Long.MAX_VALUE) {
                                flags += SET_TIMEOUT;
                            }
                            if (e.isUSE_DURATION()) {
                                flags += USE_DURATION;
                            }
                            if (e.getEffectType() == EffectList.STATIC_COLOR) {
                                flags += INSTANT_COLOR;
                            }
                            if (e.getFunction() == LightingDisplay.Function.DEFAULT) {
                                flags += DEFAULT_FUNCTION;
                            }
                            if (e.getEffectType() != EffectList.NOISE) {
                                out += "Size: " + e.getSize() + ", ";
                            } else {
                                int size = e.getNoiseCheckpoints().size() * 5 + 1;
                                if (e.isFade()) {
                                    size--;
                                }
                                out += "Size: " + size + ", ";
                            }
                            out += "Strip_id: " + l.getId() + ", ";
                            out += "Set_id: " + getEffectTriggerIndex(e, timesMS) + ", ";
                            out += "Flags: " + flags + ", ";
                            Color startColor = e.getStartColor();
                            out += "Start_color: " + startColor.getRed() + ", " + startColor.getGreen() + ", " + startColor.getBlue() + ", ";
                            Color endColor = e.getEndColor();
                            out += "End_color: " + endColor.getRed() + ", " + endColor.getGreen() + ", " + endColor.getBlue() + ", ";
                            if ((flags & DO_DELAY) > 0) {
                                if (e.isDO_DELAY()) {
                                    out += "Delay: " + e.getDelay().toMillis() + ", ";
                                } else {
                                    out += "Delay: " + timeBeforeEffect(i, e, l.getEffects(), timesMS) + ", ";
                                }

                            } else {
                                out += "Delay: 0, ";
                            }
                            out += "Duration: " + (e.getDuration().toMillis()) + ", ";
                            out += "Function: " + e.getFunction().ordinal() + ", ";
                            out += "Timeout: 0";
                            if (e.getFunction() == LightingDisplay.Function.ALTERNATING_COLOR) {
                                out += ", ExtraParameters: " + e.getSpeed();
                            }
                            if (e.getFunction() == LightingDisplay.Function.CHASE) {
                                out += ", ExtraParameters: " + e.getChaseSequence().size() + "," + e.getSpeed();
                                for (Color c : e.getChaseSequence()) {
                                    out += "," + c.getRed() + "," + c.getGreen() + "," + c.getBlue();
                                }
                            }
                            if (e.getFunction() == LightingDisplay.Function.NOISE) {
                                out += ", ExtraParameters: " + (e.isFade() ? 1 : 0);
                                for (Checkpoint c : e.getNoiseCheckpoints()) {
                                    if (c.time() != 0) {
                                        out += "," + c.time();
                                    }
                                    out += "," + c.color().getRed() + "," + c.color().getGreen() + "," + c.color().getBlue();
                                    out += "," + c.brightness();
                                }
                            }
                            out += "\n";
                        }
                        bfw.write(out);
                        bfw.flush();
                        out = "";
                    }
                    bfw.close();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    /**
     * Get audio player
     * @return AudioPlayer object
     */
    public ArrayList<AudioPlayer> getAudioPlayers() {
        return audioPlayers;
    }

    /**
     * Task used by the playback timer to repeatedly repaint the footballfieldpanel with new frames in fps mode.
     */
    private class PlaybackTask extends TimerTask {

        @Override
        public void run() {
            canSeekAudio = false;
            if (scrubBarGUI.isUseFps()) {
                if (scrubBarGUI.nextStep(playbackSpeed)) {
                    // Reached the end
                    playbackTimer.cancel();
                    playbackTimer.purge();
                    playbackTimer = null;
                    audioPlayers.get(currentMovement - 1).pauseAudio();
                    scrubBarGUI.setIsPlayingPlay();
                    canSeekAudio = true;
                    return;
                }
            } else {
                scrubBarGUI.nextCount();

                if (scrubBarGUI.isAtLastSet()) {
                    // Reached the end
                    playbackTimer.cancel();
                    playbackTimer.purge();
                    playbackTimer = null;
                    audioPlayers.get(currentMovement - 1).pauseAudio();
                    scrubBarGUI.setIsPlayingPlay();
                    canSeekAudio = true;
                    return;
                }  
            }
            canSeekAudio = true;
        }
    }
}