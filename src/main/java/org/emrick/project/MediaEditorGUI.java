package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;
import com.formdev.flatlaf.*;
import com.google.gson.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sun.net.httpserver.HttpServer;
import org.emrick.project.actions.LEDConfig;
import org.emrick.project.audio.*;
import org.emrick.project.effect.*;
import org.emrick.project.serde.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
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


public class MediaEditorGUI extends Component implements ImportListener, ScrubBarListener, SyncListener,
        FootballFieldListener, EffectListener, SelectListener, UserAuthListener, RFTriggerListener, RFSignalListener, RequestCompleteListener,
        LEDConfigListener{

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
    private LEDConfigurationGUI ledConfigurationGUI;
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
    private SelectionGroupGUI groupsGUI;
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
    private RFTriggerGUI rfTriggerGUI;
    private HashMap<Integer, RFTrigger> count2RFTrigger;
    private boolean runningShow;

    private FlowViewGUI flowViewGUI;
    private boolean isLightBoardMode;

    private LEDStripViewGUI ledStripViewGUI;

    // Time keeping
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
    // Web Server
    private HttpServer server;
    private String ssid;
    private String password;
    private int currentID;
    private static int MAX_CONNECTIONS = 50;
    private int token;
    private Color verificationColor;
    private Timer noRequestTimer;
    private ArrayList<Integer> requestIDs;
    private JMenuItem runWebServer;
    private JMenuItem runLightBoardWebServer;
    private JMenuItem stopWebServer;
    private ProgrammingTracker programmingTracker;
    private JProgressBar programmingProgressBar;
    private boolean lightBoardMode;
    // Project info
    private File archivePath = null;
    private File drillPath = null;
    private File csvFile;
    private Border originalBorder;  // To store the original border of the highlighted component
    private SerialTransmitter serialTransmitter;
    JFrame webServerFrame;

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

                if (scrubBarGUI.isAtLastSet()) {
                    // Reached the end
                    playbackTimer.stop();
                    audioPlayer.pauseAudio();
                    scrubBarGUI.setIsPlayingPlay();
                    canSeekAudio = true;
                    return;
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
        footballFieldPanel = new FootballFieldPanel(this, null);
        footballFieldPanel.setOpaque(false);
        //footballFieldPanel.setBackground(Color.lightGray); // temp. Visual indicator for unfilled space
        JScrollPane fieldScrollPane = new JScrollPane(footballFieldPanel);
        fieldScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        fieldScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        footballFieldBackground = new FootballFieldBackground(this);
        footballField = new JPanel();

        // Main frame
        frame = new JFrame("Emrick Designer");
        Image icon = Toolkit.getDefaultToolkit().getImage(PathConverter.pathConverter("res/images/icon.png", true));
        frame.setIconImage(icon);

        // Scrub Bar
        scrubBarGUI = new ScrubBarGUI(frame, this, this, footballFieldPanel);

        // Scrub bar cursor starts on first count of drill by default
        useStartDelay = true;
        runningShow = false;

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

        noRequestTimer = new Timer(25000, e -> {
           onRequestComplete(-1);
        });

        if (!file.equals("")) {
            if (file.endsWith(".emrick")) {
                createAndShowGUI();
                loadProject(new File(file));
            } else {
                runServer(file, false);
                createAndShowGUI();
            }
        } else {
            createAndShowGUI();
        }

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
// TODO: select stuff
        // TODO: make sfg not local, have it load the project after import finishes// TODO: select stuff
// TODO: select stuff
        // Open Emrick Project// TODO: select stuff
        // https://www.codejava.net/java-se/swing/add-file-filter-for-jfilechooser-dialog// TODO: select stuff
        JMenuItem openItem = new JMenuItem(FILE_MENU_OPEN_PROJECT);// TODO: select stuff
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));// TODO: select stuff
        fileMenu.add(openItem);// TODO: select stuff
        openItem.addActionListener(e -> {// TODO: select stuff
            openProjectDialog();// TODO: select stuff
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
            } else {
                mainContentPanel.remove(flowViewGUI);
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
            //redoColorChange();
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

            // TODO: Below is deprecated. Schedule for removal.
            if (archivePath == null) {
                System.out.println("no project loaded");
                return;
            }

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
            boolean success = this.effectManager.addEffectToSelectedLEDStrips(this.copiedEffect);
            if (success) updateEffectViewPanel(selectedEffectType);
            this.footballFieldPanel.repaint();
        });
        editMenu.add(pasteCopiedEffect);

        editMenu.addSeparator();

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
            System.out.println("selecting by criteria...");
            FilterSelect filterSelect = new FilterSelect(frame, this, labels, symbols);
            filterSelect.show();
        });
        editMenu.add(selectByCrit);
        editMenu.addSeparator();

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
        editMenu.add(groups);
        hideGroups.addActionListener(e -> {
            selectedEffectType = EffectList.HIDE_GROUPS;
            updateEffectViewPanel(selectedEffectType);
            groups.setEnabled(true);
            hideGroups.setEnabled(false);
        });
        hideGroups.setEnabled(false);
        editMenu.add(hideGroups);

        // View menu
        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        JCheckBoxMenuItem toggleFloorCoverImage = new JCheckBoxMenuItem("Show Floor Cover Image");
        toggleFloorCoverImage.setState(true);
        toggleFloorCoverImage.addActionListener(e -> {
            footballFieldBackground.setShowFloorCoverImage(!footballFieldBackground.isShowFloorCoverImage());
            footballFieldBackground.repaint();
        });
        viewMenu.add(toggleFloorCoverImage);
        JCheckBoxMenuItem toggleSurfaceImage = new JCheckBoxMenuItem("Show Surface Image");
        toggleSurfaceImage.setState(true);
        toggleSurfaceImage.addActionListener(e -> {
            footballFieldBackground.setShowSurfaceImage(!footballFieldBackground.isShowSurfaceImage());
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
        JCheckBoxMenuItem toggleSelectAllLEDs = new JCheckBoxMenuItem("Select All LEDs");
        toggleSelectAllLEDs.setState(true);
        toggleSelectAllLEDs.addActionListener(e -> {
            footballFieldPanel.setSelectAllLEDs(!footballFieldPanel.isSelectAllLEDs());
        });
        viewMenu.add(toggleSelectAllLEDs);

        viewMenu.addSeparator();

        JCheckBoxMenuItem showIndividualView = new JCheckBoxMenuItem("Show Individual View");
        showIndividualView.setSelected(false);
        showIndividualView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        showIndividualView.addActionListener(e -> {
            if (showIndividualView.isSelected()) {
                ArrayList<LEDStrip> ledStrips = new ArrayList<>(footballFieldPanel.selectedLEDStrips);
                ledStripViewGUI = new LEDStripViewGUI(ledStrips, effectManager);
                ledStripViewGUI.setCurrentMS(footballFieldPanel.currentMS);
                ledStripViewGUI.setCurrentSet(footballFieldPanel.getCurrentSet());
                mainContentPanel.remove(footballField);
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
        JMenuItem runShowItem = new JMenuItem("Run Show Linked to Viewport");
        runMenu.add(runShowItem);
        JMenuItem flowViewerItem = new JMenuItem("Run Show via Flow View");
        runMenu.add(flowViewerItem);
        JMenuItem lightBoardFlowViewerItem = new JMenuItem("Run Light Board via View");
        runMenu.add(lightBoardFlowViewerItem);
        JMenuItem stopShowItem = new JMenuItem("Stop show");
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
            runServer("", false);
            runWebServer.setEnabled(false);
            runLightBoardWebServer.setEnabled(false);
            stopWebServer.setEnabled(true);
        });
        runLightBoardWebServer.addActionListener(e -> {
            runServer("", true);
            runWebServer.setEnabled(false);
            runLightBoardWebServer.setEnabled(false);
            stopWebServer.setEnabled(true);
        });
        stopWebServer.addActionListener(e -> {
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
            serialTransmitter = comPortPrompt();
            if (serialTransmitter == null) {
                return;
            }
            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
            flowViewGUI = new FlowViewGUI(count2RFTrigger, this);
            mainContentPanel.remove(footballField);
            mainContentPanel.add(flowViewGUI);
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
        });

        lightBoardFlowViewerItem.addActionListener(e -> {
            isLightBoardMode = true;
            serialTransmitter = comPortPrompt();
            if (serialTransmitter == null) {
                return;
            }
            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            lightBoardFlowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
            flowViewGUI = new FlowViewGUI(count2RFTrigger, this);
            mainContentPanel.remove(footballField);
            mainContentPanel.add(flowViewGUI);
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
        });

        runShowItem.addActionListener(e -> {
            serialTransmitter = comPortPrompt();

            if (serialTransmitter == null) {
                return;
            }

            footballFieldPanel.addSetToField(footballFieldPanel.drill.sets.get(0));
            runShowItem.setEnabled(false);
            flowViewerItem.setEnabled(false);
            stopShowItem.setEnabled(true);
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

                    // Open or Create project
                    if (currentTutorialIndex == 0) {
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
        });

        JMenuItem loginItem = new JMenu("Account");
        //loginItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
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

        // handle closing the window
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (server != null) {
                    server.stop(0);
                    server = null;
                    noRequestTimer.stop();
                    requestIDs = null;
                    runWebServer.setEnabled(true);
                    stopWebServer.setEnabled(false);
                    File dir = new File(PathConverter.pathConverter("tmp/", false));
                    File[] files = dir.listFiles();
                    for (File f : files) {
                        f.delete();
                    }
                    dir.delete();
                    File f = new File("tempPkt.pkt");
                    if (f.exists()) {
                        f.delete();
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
            }
        });

        // Display the window
        frame.setJMenuBar(menuBar);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setTitle("Emrick Designer");
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

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

    public SerialTransmitter comPortPrompt() {
        SerialTransmitter st = new SerialTransmitter();
        SerialPort[] allPorts = SerialTransmitter.getPortNames();
        String[] allPortNames = new String[allPorts.length];
        for (int i = 0; i < allPorts.length; i++) {
            allPortNames[i] = allPorts[i].getDescriptivePortName();
        }
        String port = (String) JOptionPane.showInputDialog(null, "Choose",
                "Menu", JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon(PathConverter.pathConverter("icon.ico", true)),
                allPortNames, allPortNames[0]);
        st.setSerialPort(port);
        return st;
    }

    public void runServer(String path, boolean lightBoard) {
        int port = 8080;
        try {
            File f;
            // If a project is loaded, generate the packets from the project and write them to a temp file in project directory.
            // delete file after server is stopped.
            if(archivePath == null) { //if no project open
                if (path.equals("")) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select Packets (.pkt) file");
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Emrick Designer Packets File (*.pkt)", "pkt"));
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.showOpenDialog(null);
                    f = fileChooser.getSelectedFile();
                } else {
                    f = new File(path);
                }
            }
            else{ //there is a project open
                if (path.equals("")) {
                    f = new File(PathConverter.pathConverter("tempPkt.pkt", false));
                    exportPackets(f);
                } else {
                    f = new File(path);
                }
            }
            JTextField ssidField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            Object[] inputs = {
                    new JLabel("WiFi SSID:"), ssidField,
                    new JLabel("WiFi Password:"), passwordField
            };

            int option = JOptionPane.showConfirmDialog(null, inputs, "Enter WiFi Credentials", JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) {
                return;
            }
            ssid = ssidField.getText();
            char[] passwordChar = passwordField.getPassword();
            password = new String(passwordChar);

            serialTransmitter = comPortPrompt();

            Unzip.unzip(f.getAbsolutePath(), PathConverter.pathConverter("tmp/", false));

            server = HttpServer.create(new InetSocketAddress(port), 250);
            writeSysMsg("server started at " + port);
            requestIDs = new ArrayList<>();

            server.createContext("/", new GetHandler(PathConverter.pathConverter("tmp/", false), this));
            server.setExecutor(new ServerExecutor());
            server.start();
            currentID = Math.min(MAX_CONNECTIONS, footballFieldPanel.drill.ledStrips.size());
            verificationColor = JColorChooser.showDialog(this, "Select verification color", Color.WHITE);

            String input = JOptionPane.showInputDialog(null, "Enter verification token (leave blank for new token)\n\nDon't use this feature to program more than 200 units");

            if (input.isEmpty()) {
                Random r = new Random();
                token = r.nextInt(0, Integer.MAX_VALUE);
                JOptionPane.showMessageDialog(null, new JTextArea("The token for this show is: " + token + "\n Save this token in case some boards are not programmed"));
            } else {
                token = Integer.parseInt(input);
                currentID = footballFieldPanel.drill.performers.size();
            }
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
                    server.stop(0);
                    runWebServer.setEnabled(true);
                    runLightBoardWebServer.setEnabled(true);
                    stopWebServer.setEnabled(false);
                    server = null;
                    requestIDs = null;
                    noRequestTimer.stop();

                    super.windowClosing(e);
                }
            });
            webServerFrame.setVisible(true);
            lightBoardMode = lightBoard;

            if (serialTransmitter != null) {
                serialTransmitter.enterProgMode(ssid, password, currentID, token, verificationColor, lightBoardMode);
            }
            noRequestTimer.start();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void loadProject(File path) {
        try {

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
            ledStripViewGUI = new LEDStripViewGUI(new ArrayList<>(), effectManager);
            footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(0));
            ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(0));
//            rebuildPageTabCounts();
//            scrubBarGUI.setReady(true);
            footballFieldPanel.repaint();

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
        // TODO: select stuff
//        footballFieldPanel.selectedPerformers
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

    private void exportCsvFileForPerformerDeviceIDs(File selectedFile) {
        try (FileWriter fileWriter = new FileWriter(selectedFile)) {
            fileWriter.write("Performer Label,LED ID,LED Label,LED Count,Height,Width,Horizontal Offset,VerticalOffset");
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
            LEDConfig c2 = new LEDConfig();
            c2.setLabel("R");
            c2.sethOffset(1);
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

    private void parseCsvFileForPerformerDeviceIDs(File inputFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line = reader.readLine();
            if (line != null) {
                line = reader.readLine();
            }
            Performer currPerformer = null;
            ArrayList<Performer> newPerformerList = new ArrayList<>();
            ArrayList<LEDStrip> newLedStripList = new ArrayList<>();
            int currStripID = 0;
            int currPerformerID = 0;

            // Very strange buffered reader bug occurs for large csv files
            // The current code works so don't touch it unless major changes need to happen
            while (line != null) {
                System.out.println(line);
                if (!line.startsWith(",")) {
                    String[] tmp = line.split(",");
                    try {
                        if (footballFieldPanel.drill.performers.size() == 0) {
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
                    String label = tmp[2];
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

    private void saveProjectDialog() {
        if (archivePath == null) {
            System.out.println("Nothing to save.");
            writeSysMsg("Nothing to save!");
            return;
        }

        writeSysMsg("Saving project...");
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
        if (csvFile != null) {
            parseCsvFileForPerformerDeviceIDs(csvFile);
        } else {
            applyDefaultLEDConfiguration();
        }
        footballFieldPanel.addSetToField(footballFieldPanel.drill.sets.get(0));
        count2RFTrigger = new HashMap<>();
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);
        updateEffectViewPanel(selectedEffectType);
        updateTimelinePanel();
        rebuildPageTabCounts();


        ledConfigurationGUI = new LEDConfigurationGUI(footballFieldPanel.drill, this);

        mainContentPanel.remove(footballField);
        mainContentPanel.add(ledConfigurationGUI);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
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
        writeSysMsg("Got Synced Times");

        this.timeSync = times;
        this.startDelay = startDelay;

        scrubBarGUI.setTimeSync(timeSync);
        count2RFTrigger = new HashMap<>();
        footballFieldPanel.setCount2RFTrigger(count2RFTrigger);

        setupEffectView(null);
    }

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
        ledStripViewGUI.setCurrentMS(time);
    }

    @Override
    public void onSetChange(int setIndex) {
        footballFieldPanel.setCurrentSet(footballFieldPanel.drill.sets.get(setIndex));
        ledStripViewGUI.setCurrentSet(footballFieldPanel.drill.sets.get(setIndex));
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
    public void onUpdateEffectPanel(Effect effect, boolean isNew) {
        this.effectViewPanel.remove(effectGUI.getEffectPanel());
        effectGUI = new EffectGUI(effect, effect.getStartTimeMSec(), this, effect.getEffectType(), isNew);
        this.effectViewPanel.add(effectGUI.getEffectPanel());
        this.effectViewPanel.revalidate();
        this.effectViewPanel.repaint();
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
            if (l.getEffects().size() != 0) {
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

    ////////////////////////// Effect Listeners //////////////////////////

    @Override
    public void onResizeBackground() {
        footballFieldPanel.setFieldHeight(footballFieldBackground.getFieldHeight());
        footballFieldPanel.setFieldWidth(footballFieldBackground.getFieldWidth() * 5.0/6.0);
        footballFieldPanel.setFrontSideline50(footballFieldBackground.getFrontSideline50());
        footballFieldPanel.repaint();
    }

    @Override
    public void onFinishRepaint() {
        if (scrubBarGUI.isPlaying() && scrubBarGUI.isUseFps()) {

            try {
                double currTime = scrubBarGUI.getTime() * 1000;
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

    @Override
    public double getFrameRate() {
        return scrubBarGUI.getFps();
    }

    @Override
    public boolean isPlaying() {
        return scrubBarGUI.isPlaying();
    }

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
                }
            }
            effectGUI = new EffectGUI(currentEffect, currentMSec, this, selectedEffectType, false);
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

    // Don't delete, just unused for now because I don't want my disk space being eaten up
    private void autosaveProject() {
        // we don't have a project open, nothing to save
        if (archivePath == null) {
            return;
        }

        long time = System.currentTimeMillis() / 1000L;
        Path dir = Paths.get(userHome.toString(), String.valueOf(time));
        Path jsonDir = Paths.get(dir.toString(), "backup.json");
        Path archiveDir = Paths.get(dir.toString(), archivePath.getName());
        File backupDir = new File(dir.toUri());
        if (!backupDir.mkdirs()) {
            // TODO: handle error from the backup failing
            return;
        }

        try {
            Files.copy(archivePath.toPath(), archiveDir);
        } catch (IOException e) {
            // TODO: handle error from the backup failing
            System.out.println("MediaEditorGUI autosaveProject(): " + e.getMessage());
            return;
        }

        saveProject(jsonDir.toFile(), archiveDir.toFile());
        writeSysMsg("Autosaved project to `" + jsonDir + "`.");
    }

    public void saveProject(File path, File archivePath) {
        ProjectFile pf;

        ArrayList<SelectionGroupGUI.SelectionGroup> groupsList = new ArrayList<>();
        for(SelectionGroupGUI.SelectionGroup group: groupsGUI.getGroups()){
            SelectionGroupGUI.SelectionGroup toAdd = group.clone();
            toAdd.setTitleButton(null);
            groupsList.add(toAdd);
        }

        String aPath = archivePath.getName();
        for (LEDStrip ledStrip : footballFieldPanel.drill.ledStrips) {
            ledStrip.setPerformer(null);
        }
        if (this.effectManager != null) {
            pf = new ProjectFile(footballFieldPanel.drill, aPath, timeSync, startDelay, count2RFTrigger, effectManager.getIds(), groupsList);
        } else {
            pf = new ProjectFile(footballFieldPanel.drill, aPath, timeSync, startDelay, count2RFTrigger, null, groupsList);
        }
        String g = gson.toJson(pf);

        writeSysMsg("saving to `" + path + "`");
//        System.out.println(g);

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
        } catch (IOException e) {
            writeSysMsg("Failed to save to `" + path + "`.");
            throw new RuntimeException(e);
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
            dir.delete();
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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

    @Override
    public void onRFSignal(int i) {

        if (serialTransmitter != null) {

            serialTransmitter.writeSet(i, isLightBoardMode);
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
        }

        int highestID = footballFieldPanel.drill.ledStrips.size() - 1;
        if (currentID < highestID) {
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
            serialTransmitter.enterProgMode(ssid, password, currentID, token, verificationColor, lightBoardMode);
            noRequestTimer.setDelay(25000);
            noRequestTimer.start();
        } else {
            server.stop(0);
            runWebServer.setEnabled(true);
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

    private class ProgrammingTracker extends JPanel {
        private ArrayList<LEDStrip> allStrips;
        private ArrayList<Integer> completedStrips;
        private ArrayList<ProgrammableItem> items;

        public ProgrammingTracker(ArrayList<LEDStrip> allStrips, ArrayList<Integer> completedStrips) {
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

        public ArrayList<Integer> getCompletedStrips() {
            return completedStrips;
        }

        public void setCompletedStrips(ArrayList<Integer> completedStrips) {
            this.completedStrips = completedStrips;
        }

        public void addCompletedStrip(Integer ledStrip) {
            completedStrips.add(ledStrip);
            setItemCompleted(ledStrip);
        }

        private void setItemCompleted(Integer ledStrip) {
            for (ProgrammableItem item : items) {
                if (item.getLedStrip().getId() == ledStrip) {
                    item.setProgrammed(true);
                }
            }
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
                    g.setColor(Color.GREEN);
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
                                    out += "Size: 0, Strip_id: " + l.getPerformerID() + ", Set_id: " + getEffectTriggerIndex(e, timesMS)
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
                            out += "Strip_id: " + l.getPerformerID() + ", ";
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
                        bfw.close();
                        out = "";
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
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