package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;
import com.formdev.flatlaf.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import org.emrick.project.actions.LEDConfig;
import org.emrick.project.audio.*;
import org.emrick.project.effect.*;
import org.emrick.project.serde.*;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.Font;
import java.awt.Image;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/**
 * Main class of Emrick Designer.
 * Contains all GUI elements and logic for light show design and Emrick board interaction
 */
public class MediaEditorGUI extends Component implements ImportListener, ScrubBarListener, SyncListener,
        FootballFieldListener, EffectListener, SelectListener, UserAuthListener, RFTriggerListener, RFSignalListener, RequestCompleteListener,
        LEDConfigListener, ReplaceFilesListener {

    // String definitions
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
    private JPanel timelinePanel;

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
    public AudioPlayer audioPlayer;
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

    // Web Server
    private HttpServer server;
    private String ssid;
    private String password;
    private int port;
    private int currentID;
    private static int MAX_CONNECTIONS = 50;
    private int token;
    private Color verificationColor;
    private Timer noRequestTimer;
    private HashSet<Integer> requestIDs;
    private JMenuItem runWebServer;
    private JMenuItem runLightBoardWebServer;
    private JMenuItem stopWebServer;
    private ProgrammingTracker programmingTracker;
    private JProgressBar programmingProgressBar;
    private boolean lightBoardMode;

    // Flow viewer
    private JMenuItem runShowItem;
    private JMenuItem flowViewerItem;
    private JMenuItem lightBoardFlowViewerItem;
    private JMenuItem stopShowItem;
    private boolean isLightBoardMode;

    private JCheckBoxMenuItem showIndividualView;

    // Project info
    private File archivePath = null;
    private File emrickPath = null;
    private File csvFile;
    private SerialTransmitter serialTransmitter;
    JFrame webServerFrame;

    /**
     * Main method of Emrick Designer.
     *
     * @param args - Only used when opening the application via an associated file type rather than an executable
     */
    public static void main(String[] args) {
        final String file;
        if (args.length != 0) {
            file = args[0];
        } else {
            file = "";
        }
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

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
        frame.setSize(1200, 600);

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
                    if (audioPlayer != null) {
                        audioPlayer.pauseAudio();
                    }
                }
                if (archivePath != null) {
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

        noRequestTimer = new Timer(25000, e -> {
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
     * Builds all major GUI elements and adds them to the main frame.
     * This method should be called on startup and on project loading when another project is already loaded.
     */
    private void createAndShowGUI() {
        RFTrigger.rfTriggerListener = this;
        Effect.effectListener = this;

        if (archivePath != null) {
            frame.remove(mainContentPanel);
            frame.remove(effectViewPanel);
            frame.remove(timelinePanel);
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

        flowViewGUI = new FlowViewGUI(new HashMap<>(), this);

        // Scrub Bar
        scrubBarGUI = new ScrubBarGUI(frame, this, this, footballFieldPanel, getAudioPlayer());

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

        frame.add(mainContentPanel, BorderLayout.CENTER);

        // Timeline panel
        timelinePanel = new JPanel(new BorderLayout());
        timelinePanel.setBorder(BorderFactory.createTitledBorder("Timeline"));
        timelinePanel.setPreferredSize(new Dimension(frame.getWidth(), 120));
        frame.add(timelinePanel, BorderLayout.SOUTH);

        // Effect View panel
        effectGUI = new EffectGUI(EffectGUI.noProjectSyncMsg);
        groupsGUI = new SelectionGroupGUI(this);
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
                updateEffectViewPanel(selectedEffectType);
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
                updateEffectViewPanel(selectedEffectType);
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
            updateEffectViewPanel(selectedEffectType);
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
            updateEffectViewPanel(selectedEffectType);
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
            if (success) updateEffectViewPanel(selectedEffectType);
            this.footballFieldPanel.repaint();
        });
        editMenu.add(pasteCopiedEffect);

        // Select Menu
        JMenu selectMenu = new JMenu("Select");
        menuBar.add(selectMenu);

        // TODO: FIX this feature
        JMenuItem selectByCrit = new JMenuItem("Select by Criteria");
        selectByCrit.addActionListener(e -> {
            if (archivePath == null) {
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
        selectMenu.add(selectByCrit);

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
            updateEffectViewPanel(selectedEffectType);
            hideGroups.setEnabled(true);
            groups.setEnabled(false);
        });
        selectMenu.add(groups);
        hideGroups.addActionListener(e -> {
            selectedEffectType = EffectList.HIDE_GROUPS;
            updateEffectViewPanel(selectedEffectType);
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
        lightBoardFlowViewerItem = new JMenuItem("Run Light Board via View");
        runMenu.add(lightBoardFlowViewerItem);
        stopShowItem = new JMenuItem("Stop show");
        stopShowItem.setEnabled(false);
        runMenu.add(stopShowItem);
        runMenu.addSeparator();
        runWebServer = new JMenuItem("Run Web Server");
        runLightBoardWebServer = new JMenuItem("Run Light Board Web Server");
        stopWebServer = new JMenuItem("Stop Web Server");
        runMenu.add(runWebServer);
        runMenu.add(runLightBoardWebServer);
        runMenu.add(stopWebServer);
        if (server == null) {
            stopWebServer.setEnabled(false);
        } else {
            runWebServer.setEnabled(false);
            runLightBoardWebServer.setEnabled(false);
        }

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
            serialTransmitter = null;
            stopShowItem.setEnabled(false);
            runShowItem.setEnabled(true);
            flowViewerItem.setEnabled(true);
            lightBoardFlowViewerItem.setEnabled(true);
        });
        flowViewerItem.addActionListener(e -> {
            isLightBoardMode = false;
            serialTransmitter = comPortPrompt("Transmitter");
            if (!serialTransmitter.getType().equals("Transmitter")) {
                return;
            }
            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
            flowViewGUI = new FlowViewGUI(count2RFTrigger, this);
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
            isLightBoardMode = true;
            serialTransmitter = comPortPrompt("Transmitter");
            if (!serialTransmitter.getType().equals("Transmitter")) {
                return;
            }
            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
            flowViewGUI = new FlowViewGUI(count2RFTrigger, this);
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
            serialTransmitter = comPortPrompt("Transmitter");

            if (!serialTransmitter.getType().equals("Transmitter")) {
                return;
            }

            footballFieldPanel.addSetToField(footballFieldPanel.drill.sets.get(0));
            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
        });

        JMenu hardwareMenu = new JMenu("Hardware");
        menuBar.add(hardwareMenu);
        JMenuItem verifyShowItem = new JMenuItem("Verify Show");
        hardwareMenu.add(verifyShowItem);
        JMenuItem verifyLightBoardItem = new JMenuItem("Verify Light Board");
        hardwareMenu.add(verifyLightBoardItem);
        JMenuItem wirelessCheck = new JMenuItem("Wireless Check");
        hardwareMenu.add(wirelessCheck);
        JMenuItem storageMode = new JMenuItem("Storage Mode");
        hardwareMenu.add(storageMode);
        JMenuItem batteryCheck = new JMenuItem("Battery Check");
        hardwareMenu.add(batteryCheck);
        JMenuItem massSleep = new JMenuItem("Mass Sleep");
        hardwareMenu.add(massSleep);
        hardwareMenu.addSeparator();
        JMenuItem modifyBoardItem = new JMenuItem("Modify Board");
        hardwareMenu.add(modifyBoardItem);

        verifyShowItem.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("v");
        });

        verifyLightBoardItem.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("w");
        });

        wirelessCheck.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("c");
        });

        // For Storage Mode
        storageMode.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("d");
        });

        batteryCheck.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("o");
        });

        massSleep.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Transmitter");
            if (st == null) return;

            st.writeToSerialPort("e");
        });

        modifyBoardItem.addActionListener(e -> {
            SerialTransmitter st = comPortPrompt("Receiver");
            if (!st.getType().equals("Receiver")) {
                return;
            }

            JTextField boardIDField = new JTextField();
            JCheckBox boardIDEnable = new JCheckBox("Write new Board ID");
            boardIDEnable.setSelected(false);
            JTextField ledCountField = new JTextField();
            JCheckBox enableLedCount = new JCheckBox("Write new LED Count");
            enableLedCount.setSelected(false);

            Object[] inputs = {
                    new JLabel("Board ID: "), boardIDField, boardIDEnable,
                    new JLabel("LED Count: "), ledCountField, enableLedCount
            };

            int option = JOptionPane.showConfirmDialog(null, inputs, "Enter board parameters:", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                if (boardIDEnable.isSelected()) {
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
                }
                if (enableLedCount.isSelected()) {
                    st.writeLEDCount(ledCountField.getText());
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

        //Light menu. and adjust its menu location
        JButton effectOptions = getEffectOptionsButton();
        effectViewPanel.add(effectOptions, BorderLayout.NORTH);


        // Display the window
        if (archivePath == null) {
            frame.setJMenuBar(menuBar);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setTitle("Emrick Designer");
        } else {
            frame.revalidate();
            frame.repaint();
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
    private JButton getEffectOptionsButton() {
        JPopupMenu lightMenuPopup = new JPopupMenu();

        JMenuItem fadePattern = new JMenuItem("Create Fade Effect");
        fadePattern.addActionListener(e -> {
            selectedEffectType = EffectList.GENERATED_FADE;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(fadePattern);

        JMenuItem staticColorPattern = new JMenuItem("Create Static Color Effect");
        staticColorPattern.addActionListener(e -> {
            selectedEffectType = EffectList.STATIC_COLOR;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(staticColorPattern);

        JMenuItem wavePattern = new JMenuItem("Create Wave Effect");
        wavePattern.addActionListener(e -> {
            selectedEffectType = EffectList.WAVE;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(wavePattern);

        JMenuItem alternatingColorPattern = new JMenuItem("Create Alternating Color Effect");
        alternatingColorPattern.addActionListener(e -> {
            selectedEffectType = EffectList.ALTERNATING_COLOR;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(alternatingColorPattern);

        JMenuItem ripplePattern = new JMenuItem("Create Ripple Effect");
        ripplePattern.addActionListener(e -> {
            selectedEffectType = EffectList.RIPPLE;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(ripplePattern);

        JMenuItem circleChasePattern = new JMenuItem("Create Circle Chase Effect");
        circleChasePattern.addActionListener(e -> {
            selectedEffectType = EffectList.CIRCLE_CHASE;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(circleChasePattern);

        JMenuItem chasePattern = new JMenuItem("Create Chase Effect");
        chasePattern.addActionListener(e -> {
            selectedEffectType = EffectList.CHASE;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(chasePattern);

        JMenuItem gridPattern = new JMenuItem("Create Grid Effect");
        gridPattern.addActionListener(e -> {
            selectedEffectType = EffectList.GRID;
            updateEffectViewPanel(selectedEffectType);
        });
        lightMenuPopup.add(gridPattern);


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

    /**
     * Used to get a Serial Transmitter object.
     * If only 1 Emrick board of the desired type is connected, it will be found automatically.
     * Otherwise, the user will be prompted with a menu to select the intended COM port
     *
     * @param type The type of hardware that should be detected.
     * @return A SerialTransmitter object loaded with the specified COM port.
     * If no COM ports are found, this method returns null.
     */
    public SerialTransmitter comPortPrompt(String type) {
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
            File f;
            // If a project is loaded, generate the packets from the project and write them to a temp file in project directory.
            // delete file after server is stopped.
            if(archivePath == null) { //if no project open
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
            currentID = Math.min(MAX_CONNECTIONS, footballFieldPanel.drill.ledStrips.size());

            ssid = ssidField.getText();
            char[] passwordChar = passwordField.getPassword();
            password = new String(passwordChar);
            port = Integer.parseInt(portField.getText());

            serialTransmitter = comPortPrompt("Transmitter");
            if (!serialTransmitter.getType().equals("Transmitter")) {
                stopWebServer.setEnabled(false);
                runWebServer.setEnabled(true);
                runLightBoardWebServer.setEnabled(true);
                deleteDirectory(f);
                return;
            }

            Unzip.unzip(f.getAbsolutePath(), PathConverter.pathConverter("tmp/", false));
            verificationColor = JColorChooser.showDialog(this, "Select verification color", Color.WHITE);
            if (verificationColor == null) {
                stopWebServer.setEnabled(false);
                runWebServer.setEnabled(true);
                runLightBoardWebServer.setEnabled(true);
                return;
            }

            String input = JOptionPane.showInputDialog(null, "Enter verification token (leave blank for new token)\n\nDon't use this feature to program more than 200 units");

            if (input != null) {
                if (input.isEmpty()) {
                    Random r = new Random();
                    token = r.nextInt(0, Integer.MAX_VALUE);
                    JOptionPane.showMessageDialog(null, new JTextArea("The token for this show is: " + token + "\n Save this token in case some boards are not programmed"));
                } else {
                    token = Integer.parseInt(input);
                    currentID = footballFieldPanel.drill.performers.size();
                }
            } else {
                stopWebServer.setEnabled(false);
                runWebServer.setEnabled(true);
                runLightBoardWebServer.setEnabled(true);
                deleteDirectory(f);
                return;
            }

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
            programmingTracker = new ProgrammingTracker(footballFieldPanel.drill.ledStrips, requestIDs);
            JScrollPane scrollPane = new JScrollPane(programmingTracker);
            JPanel fullPanel = new JPanel();
            fullPanel.setLayout(new BoxLayout(fullPanel, BoxLayout.Y_AXIS));
            fullPanel.add(scrollPane);

            programmingProgressBar = new JProgressBar(0, footballFieldPanel.drill.ledStrips.size());
            programmingProgressBar.setValue(0);
            programmingProgressBar.setPreferredSize(new Dimension(300, 40));
            programmingProgressBar.setMaximumSize(new Dimension(300, 40));
            programmingProgressBar.setMinimumSize(new Dimension(300, 40));
            programmingProgressBar.setString(programmingProgressBar.getValue() + "/" + programmingProgressBar.getMaximum());
            programmingProgressBar.setStringPainted(true);
            fullPanel.add(programmingProgressBar);
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

            if (serialTransmitter != null) {
                serialTransmitter.enterProgMode(ssid, password, port, currentID, token, verificationColor, lightBoardMode);
            }
            noRequestTimer.start();
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

            if (archivePath != null) {
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


            FileReader r = new FileReader(path);
            ProjectFile pf = gson.fromJson(r, ProjectFile.class);
            r.close();
            ImportArchive ia = new ImportArchive(this);

            archivePath = new File(PathConverter.pathConverter("show_data/" + pf.archivePath, false));

            ia.fullImport(archivePath.getAbsolutePath(), null);
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
                onSync(pf.timeSync, pf.startDelay);
                scrubBarGUI.setTimeSync(timeSync);
                startDelay = pf.startDelay;
                count2RFTrigger = pf.count2RFTrigger;
                footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
                setupEffectView(pf.ids);
                rebuildPageTabCounts();
                updateTimelinePanel();
                updateEffectViewPanel(selectedEffectType);
            }

        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            writeSysMsg("Failed to open to `" + path + "`.");
            throw new RuntimeException(e);
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

    @Override
    public void onMultiSelect(HashSet<Integer> labels, HashSet<String> symbols) {
        for (Performer p : footballFieldPanel.drill.performers) {
            if (labels.contains(p.getLabel()) || symbols.contains(p.getSymbol())) {
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
        updateTimelinePanel();
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
        updateTimelinePanel();
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
        updateEffectViewPanel(selectedEffectType);
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
            // TODO: rewrite so that effects are not lost during this process
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

    /**
     * Attempts to save the project to a file.
     * If the currently open project is a new project, the user will be prompted to specify
     * a save location before the project is saved.
     */
    private void saveProjectDialog() {
        if (archivePath == null) {
            System.out.println("Nothing to save.");
            writeSysMsg("Nothing to save!");
            return;
        }

        writeSysMsg("Saving Project...");
        if (emrickPath != null) {
            writeSysMsg("Saving file `" + emrickPath + "`.");
            saveProject(emrickPath, archivePath);
        } else {
            saveAsProjectDialog();
        }
    }

    /**
     * Prompts the user for a location to save the current project.
     */
    private void saveAsProjectDialog() {
        if (archivePath == null) {
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
            saveProject(new File(path), archivePath);
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
    public void onFileSelect(File archivePath, File csvFile) {
        if (this.archivePath != null) {
            createAndShowGUI();
        }
        this.archivePath = archivePath;
        this.csvFile = csvFile;
        emrickPath = null;
    }

    @Override
    public void onAudioImport(File audioFile) {
        // Playing or pausing audio is done through the AudioPlayer service class
        audioPlayer = new AudioPlayer(audioFile);
        scrubBarGUI.setAudioPlayer(audioPlayer);
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
        count2RFTrigger = new HashMap<>();
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        footballFieldBackground.justResized = true;
        footballFieldBackground.repaint();
        updateEffectViewPanel(selectedEffectType);
        updateTimelinePanel();
        rebuildPageTabCounts();


        ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);

        mainContentPanel.remove(footballField);
        mainContentPanel.add(ledConfigurationGUI);
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


        setupEffectView(null);
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

        updateEffectViewPanel(selectedEffectType);
        updateRFTriggerButton();
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
        if (audioPlayer != null && scrubBarGUI.getAudioCheckbox().isSelected()) {
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
        if (audioPlayer != null) {
            audioPlayer.pauseAudio();
        }
        playbackTimer.cancel();
        playbackTimer.purge();
        playbackTimer = null;
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
        ledStripViewGUI.setCurrentMS(time);
    }

    @Override
    public void onSetChange(int setIndex) {
        footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(setIndex));
        ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(setIndex));
    }

    /**
     * Create a create/delete button depending on whether there is RF trigger at current count.
     */
    private void updateRFTriggerButton() {
        if (rfTriggerGUI != null) {
            //effectViewPanel.remove(rfTriggerGUI.getCreateDeleteBtn());
            effectViewPanel.remove(rfTriggerGUI.getCreateDeletePnl());
            effectViewPanel.revalidate();
            effectViewPanel.repaint();
        }
        int currentCount = footballFieldPanel.getCurrentCount();
        RFTrigger currentRFTrigger = count2RFTrigger.get(currentCount);
        rfTriggerGUI = new RFTriggerGUI(
                currentCount, timeManager.getCount2MSec().get(currentCount), currentRFTrigger, this);

        //effectViewPanel.add(rfTriggerGUI.getCreateDeleteBtn(), BorderLayout.SOUTH);
        effectViewPanel.add(rfTriggerGUI.getCreateDeletePnl(), BorderLayout.SOUTH);

        effectViewPanel.revalidate();
        effectViewPanel.repaint();
    }

    /**
     * Begin playing audio in sync with the drill playback
     */
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
            updateEffectViewPanel(selectedEffectType);
            updateTimelinePanel();
        }
    }

    @Override
    public void onUpdateEffect(Effect oldEffect, Effect newEffect) {
        this.effectManager.replaceEffectForSelectedLEDStrips(oldEffect, newEffect);
        if (ledStripViewGUI.isShowing()) {
            ledStripViewGUI.repaint();
        } else {
            this.footballFieldPanel.repaint();
        }
        updateEffectViewPanel(selectedEffectType);
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
        updateEffectViewPanel(selectedEffectType);
        updateTimelinePanel();
    }

    @Override
    public void onUpdateEffectPanel(Effect effect, boolean isNew, int index) {
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
            updateEffectViewPanel(selectedEffectType);
            updateTimelinePanel();
        }
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
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        updateRFTriggerButton();
        updateTimelinePanel();
    }

    @Override
    public void onDeleteRFTrigger(int count) {
        count2RFTrigger.remove(count);
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        updateRFTriggerButton();
        updateTimelinePanel();
    }

    @Override
    public void onPressRFTrigger(RFTrigger rfTrigger) {
        // scrub to this rf trigger
        scrubBarGUI.setScrub(rfTrigger.getCount());
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
        scrubBarGUI.setScrub(count-1);
    }

    /**
     * Update the effect panel to display the currently selected effect
     * @param effectType - The type of effect that is currently selected.
     */
    private void updateEffectViewPanel(EffectList effectType) {

        // No point in updating effect view if can't use effects
        if (effectManager == null) return;

        // Remove existing effect data
        if (groupsGUI.getSelectionPanel() != null) {
            effectViewPanel.remove(groupsGUI.getSelectionPanel());
            groupsGUI.setSelectionPanel(null);
        } else {
            effectViewPanel.remove(effectGUI.getEffectPanel());
        }
        effectViewPanel.revalidate();
        effectViewPanel.repaint();

        // Effects
        if (selectedEffectType != EffectList.SHOW_GROUPS) {
            if (footballFieldPanel.selectedLEDStrips.isEmpty()) {
                currentEffect = null;
                effectGUI = new EffectGUI(EffectGUI.noPerformerMsg);
                effectViewPanel.add(effectGUI.getEffectPanel(), BorderLayout.CENTER);

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
                effectViewPanel.add(effectGUI.getEffectPanel(), BorderLayout.CENTER);

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
                }
            }
            effectGUI = new EffectGUI(currentEffect, currentMSec, this, selectedEffectType, false, -1);
            // Add updated data for effect view
            effectViewPanel.add(effectGUI.getEffectPanel(), BorderLayout.CENTER);
            effectViewPanel.revalidate();
            effectViewPanel.repaint();
        } else {
            groupsGUI.initializeSelectionPanel();
            JPanel panel = groupsGUI.getSelectionPanel();
            effectViewPanel.add(panel);
            effectViewPanel.revalidate();
            effectViewPanel.repaint();
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


        // Get effects of selected performers, if applicable, else will be null
        HashSet<Effect> effectsSet = new HashSet<>();
        for (LEDStrip l : footballFieldPanel.selectedLEDStrips) {
            for (Effect e : l.getEffects()) {
                effectsSet.add(e.getGeneratedEffect().generateEffectObj());
            }
        }
        ArrayList<Effect> effectsList = new ArrayList<>(effectsSet);
        timelineGUI = new TimelineGUI(effectsList, count2RFTrigger);

        timelinePanel.add(timelineGUI.getTimelineScrollPane());
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }

    /**
     * Save the current project to a .emrick file.
     * @param path The file location to save the project.
     * @param archivePath The location of the .3dz file in user files when the project is loaded.
     */
    private void saveProject(File path, File archivePath) {
        ProjectFile pf;

        ArrayList<SelectionGroupGUI.SelectionGroup> groupsList = new ArrayList<>();
        for(SelectionGroupGUI.SelectionGroup group: groupsGUI.getGroups()){
            SelectionGroupGUI.SelectionGroup toAdd = group.clone();
            toAdd.setTitleButton(null);
            groupsList.add(toAdd);
        }

        String aPath = archivePath.getName();
        ArrayList<Performer> recoverPerformers = new ArrayList<>();
        for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
            recoverPerformers.add(ledStrip.getPerformer());
            ledStrip.setPerformer(null);
        }
        if (this.effectManager != null) {
            pf = new ProjectFile(footballFieldPanel.drill, aPath, timeSync, startDelay, count2RFTrigger, effectManager.getIds(), groupsList);
        } else {
            pf = new ProjectFile(footballFieldPanel.drill, aPath, timeSync, startDelay, count2RFTrigger, null, groupsList);
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

        if (serialTransmitter != null) {

            serialTransmitter.writeSet(i, isLightBoardMode);
        }
    }

    @Override
    public void onRequestComplete(int id) { // this technically isn't thread safe, but it has been tested with requests at a 1 ms delay so its probably fine
        if (id != -1) {
            requestIDs.add(id);
            programmingProgressBar.setValue(requestIDs.size());
            programmingProgressBar.setString(programmingProgressBar.getValue() + "/" + programmingProgressBar.getMaximum());
            programmingProgressBar.setStringPainted(true);
            programmingTracker.addCompletedStrip(id);
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
            serialTransmitter.enterProgMode(ssid, password, port, currentID, token, verificationColor, lightBoardMode);
            noRequestTimer.setDelay(25000);
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
        mainContentPanel.remove(ledConfigurationGUI);
        mainContentPanel.add(footballField);
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
            public void onFileSelect(File archivePath, File csvFile) {}

            @Override
            public void onAudioImport(File audioFile) {
                audioPlayer = new AudioPlayer(audioFile);
                scrubBarGUI.setAudioPlayer(audioPlayer);
            }

            @Override
            public void onDrillImport(String drill) {
                Drill newDrill = DrillParser.parseWholeDrill(DrillParser.extractText(drill));
                Drill oldDrill = footballFieldPanel.drill;
                boolean same = true;
                for (int i = 0 ; i < oldDrill.sets.size(); i++) {
                    if (newDrill.sets.size() > i) {
                        if (!newDrill.sets.get(i).equals(oldDrill.sets.get(i))) {
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
                                editList.add(new EditItem("UPDATE", newDrill.sets.get(i+insertCount)));
                            } else {
                                editList.add(new EditItem("NOP", oldDrill.sets.get(i)));
                            }
                        } else {
                            editList.add(new EditItem("DELETE", oldDrill.sets.get(i)));
                            insertCount--;
                            deleteCount++;
                        }
                    }
                    HashSet<TimelineEvent> eventSet = new HashSet<>();
                    for (LEDStrip l : footballFieldPanel.drill.ledStrips) {
                        for (Effect e : l.getEffects()) {
                            eventSet.add(e.getGeneratedEffect().generateEffectObj());
                        }
                    }
                    Iterator<RFTrigger> it = count2RFTrigger.values().iterator();
                    while (it.hasNext()) {
                        eventSet.add(it.next());
                    }
                    ArrayList<TimelineEvent> eventList = new ArrayList<>(eventSet);
                    eventList.sort((o1, o2) -> {
                        long time1;
                        long time2;
                        if (o1 instanceof RFTrigger) {
                            time1 = ((RFTrigger) o1).getTimestampMillis();
                        } else {
                            time1 = ((Effect) o1).getStartTimeMSec();
                        }
                        if (o2 instanceof RFTrigger) {
                            time2 = ((RFTrigger) o2).getTimestampMillis();
                        } else {
                            time2 = ((Effect) o2).getStartTimeMSec();
                        }
                        if (o1 instanceof RFTrigger && !(o2 instanceof RFTrigger) && time1 == time2) {
                            time1--;
                        }
                        if (o2 instanceof RFTrigger && !(o1 instanceof RFTrigger) && time2 == time1) {
                            time2--;
                        }
                        return (int) (time1 - time2);
                    });

                    //reverse traversal of editList
                    int offset = oldDrill.sets.size() - editList.size();
                    ArrayList<Map.Entry<String, Long>> set2MSec = timeManager.getSet2MSec();
                    ArrayList<Map.Entry<String, Integer>> set2Count = timeManager.getSet2CountSorted();
                    int[] deleteStartCounts = new int[deleteCount];
                    int[] deleteEndCounts = new int[deleteCount];
                    long[] deleteStartMsecs = new long[deleteCount];
                    long[] deleteEndMsecs = new long[deleteCount];
                    for (int i = editList.size() - 1; i >= 0; i--) {
                        switch (editList.get(i).getOperation()) {
                            case "INSERT": {

                                break;
                            }
                            case "UPDATE": {
                                break;
                            }
                            case "DELETE": {
                                long startMsec;
                                long endMsec;
                                int startCount;
                                int endCount;
                                if (i != editList.size() - 1) {

                                } else {

                                }
                                break;
                            }
                        }
                    }

                    // loop through event list and regenerate all effects


                }

                oldDrill.coordinates = newDrill.coordinates;
                for (Performer p : oldDrill.performers) {
                    p.loadCoordinates(oldDrill.coordinates);
                }
                footballFieldPanel.repaint();
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
        importArchive.fullImport(aPath, dPath);
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
        private ArrayList<ProgrammableItem> items;
        private boolean painting = false;

        public ProgrammingTracker(ArrayList<LEDStrip> allStrips, HashSet<Integer> completedStrips) {
            this.allStrips = allStrips;
            this.completedStrips = completedStrips;
            items = new ArrayList<ProgrammableItem>();
            this.setLayout(new GridLayout(20, allStrips.size() / 20+1));
            for (LEDStrip l : allStrips) {
                ProgrammableItem item = new ProgrammableItem(l);
                items.add(item);
                this.add(item);
            }
            for (Integer l : completedStrips) {
                setItemCompleted(l);
            }
        }

        public ArrayList<LEDStrip> getAllStrips() {
            return allStrips;
        }

        public void setAllStrips(ArrayList<LEDStrip> allStrips) {
            this.allStrips = allStrips;
        }

        public HashSet<Integer> getCompletedStrips() {
            return completedStrips;
        }

        public void setCompletedStrips(HashSet<Integer> completedStrips) {
            this.completedStrips = completedStrips;
        }

        public void addCompletedStrip(Integer ledStrip) {
            setItemCompleted(ledStrip);
            if (!painting) {
                painting = true;
                repaint();
            }
        }

        private void setItemCompleted(Integer ledStrip) {
            for (ProgrammableItem item : items) {
                if (item.getLedStrip().getId() == ledStrip) {
                    item.setProgrammed(true);
                }
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            painting = false;
        }

        private class ProgrammableItem extends JPanel {
            private LEDStrip ledStrip;
            private boolean programmed;

            public ProgrammableItem(LEDStrip ledStrip) {
                this.ledStrip = ledStrip;
                this.programmed = false;
                this.setMaximumSize(new Dimension(50, 50));
                this.setPreferredSize(new Dimension(50, 50));
                this.setMinimumSize(new Dimension(50, 50));
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (programmed) {
                    g.setColor(Color.BLUE);
                } else {
                    g.setColor(Color.RED);
                }
                g.drawString(ledStrip.getLabel(), 15, 15);
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
                        out += "Pkt_count: " + l.getEffects().size() + ", ";
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
                            if (e.isINSTANT_COLOR()) {
                                flags += INSTANT_COLOR;
                            }
                            if (e.getFunction() == LightingDisplay.Function.DEFAULT) {
                                flags += DEFAULT_FUNCTION;
                            }
                            out += "Size: " + e.getSize() + ", ";
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
    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
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
                    audioPlayer.pauseAudio();
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
                    audioPlayer.pauseAudio();
                    scrubBarGUI.setIsPlayingPlay();
                    canSeekAudio = true;
                    return;
                }
            }
            canSeekAudio = true;
        }
    }
}