package org.emrick.project;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * A hardware status indicator that shows the status of connected Emrick Receivers and Transmitters.
 * This replaces the need to prompt users every time they need to use hardware.
 */
public class HardwareStatusIndicator extends JPanel {

    // Hardware detection data
    private Map<String, SerialTransmitter> availableTransmitters = new HashMap<>();
    private Map<String, SerialTransmitter> availableReceivers = new HashMap<>();
    private SerialTransmitter selectedTransmitter = null;
    private SerialTransmitter selectedReceiver = null;

    // UI Components
    private JLabel transmitterLabel;
    private JLabel receiverLabel;
    private Timer scanTimer;
    private MediaEditorGUI parent;

    // New: spinner and scanning state
    private JLabel spinnerLabel;
    private Timer spinnerTimer;
    private volatile boolean scanning = false;
    private final String[] SPINNER_CHARS = new String[] { "|", "/", "-", "\\" };
    private int spinnerIndex = 0;

    // Status colors
    private static final Color CONNECTED_COLOR = new Color(34, 139, 34); // Green
    private static final Color DISCONNECTED_COLOR = new Color(220, 20, 60); // Crimson
    private static final Color MULTIPLE_COLOR = new Color(255, 140, 0); // Dark Orange
    private static final Color SCANNING_COLOR = new Color(255, 200, 0); // yellow for scanning

    public HardwareStatusIndicator(MediaEditorGUI parent) {
        this.parent = parent;
        initializeUI();

        // Set fixed size to prevent stretching in MenuBar
        setPreferredSize(new Dimension(140, 32));
        setMinimumSize(new Dimension(140, 32));
        setMaximumSize(new Dimension(140, 32));

        startHardwareScanning();
    }

    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        setOpaque(false);

        // Create a compact panel for the status indicators using BoxLayout for better control
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setOpaque(false);

        // Transmitter row
        JPanel txRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        txRow.setOpaque(false);

        JLabel txIcon = new JLabel("<html>&#x1F4E1;</html>");
        txIcon.setToolTipText("Transmitter Status");
        txIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        // Fix icon size to prevent growing
        txIcon.setPreferredSize(new Dimension(16, 12));
        txIcon.setMinimumSize(new Dimension(16, 12));
        txIcon.setMaximumSize(new Dimension(16, 12));
        txRow.add(txIcon);

        transmitterLabel = new JLabel("No TX");
        transmitterLabel.setFont(transmitterLabel.getFont().deriveFont(Font.BOLD, 10f));
        transmitterLabel.setForeground(DISCONNECTED_COLOR);
        transmitterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        transmitterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleTransmitterClick();
            }
        });
        txRow.add(transmitterLabel);
        statusPanel.add(txRow);

        // Receiver row
        JPanel rxRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rxRow.setOpaque(false);

        JLabel rxIcon = new JLabel("<html>&#x1F4FB;</html>");
        rxIcon.setToolTipText("Receiver Status");
        rxIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        // Fix icon size to prevent growing
        rxIcon.setPreferredSize(new Dimension(16, 12));
        rxIcon.setMinimumSize(new Dimension(16, 12));
        rxIcon.setMaximumSize(new Dimension(16, 12));
        rxRow.add(rxIcon);

        receiverLabel = new JLabel("No RX");
        receiverLabel.setFont(receiverLabel.getFont().deriveFont(Font.BOLD, 10f));
        receiverLabel.setForeground(DISCONNECTED_COLOR);
        receiverLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        receiverLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleReceiverClick();
            }
        });
        rxRow.add(receiverLabel);
        statusPanel.add(rxRow);

        add(statusPanel);

        // Spinner label (small) to indicate scanning animation
        spinnerLabel = new JLabel("");
        spinnerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        spinnerLabel.setPreferredSize(new Dimension(14, 16));
        spinnerLabel.setMinimumSize(new Dimension(14, 16));
        spinnerLabel.setMaximumSize(new Dimension(14, 16));
        spinnerLabel.setOpaque(false);
        add(spinnerLabel);

        // Refresh button on the right side - make it non-focusable
        JButton refreshButton = new JButton("<html>&#x1F504;</html>");
        refreshButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        refreshButton.setToolTipText("Refresh Hardware Detection");
        refreshButton.setPreferredSize(new Dimension(20, 20));
        refreshButton.setMargin(new Insets(0, 0, 0, 0));
        refreshButton.setFocusable(false); // Prevent focus stealing
        refreshButton.addActionListener(e -> scanForHardware());
        add(refreshButton);
    }

    private void startHardwareScanning() {
        // Don't start automatic scanning - only scan on demand
        // scanTimer = new Timer(15000, e -> scanForHardware());
        // scanTimer.start();

        // Do one initial scan after a delay
        Timer initialScanTimer = new Timer(3000, e -> scanForHardware());
        initialScanTimer.setRepeats(false);
        initialScanTimer.start();
    }

    /**
     * Start a simple spinner animation and color change to indicate scanning.
     * Safe to call from any thread.
     */
    public void startScanAnimation() {
        if (SwingUtilities.isEventDispatchThread()) {
            doStartScanAnimation();
        } else {
            SwingUtilities.invokeLater(this::doStartScanAnimation);
        }
    }

    private void doStartScanAnimation() {
        if (scanning) return;
        scanning = true;
        spinnerIndex = 0;
        spinnerLabel.setText(SPINNER_CHARS[spinnerIndex]);
        transmitterLabel.setForeground(SCANNING_COLOR);
        receiverLabel.setForeground(SCANNING_COLOR);

        spinnerTimer = new Timer(150, e -> {
            spinnerIndex = (spinnerIndex + 1) % SPINNER_CHARS.length;
            spinnerLabel.setText(SPINNER_CHARS[spinnerIndex]);
        });
        spinnerTimer.setRepeats(true);
        spinnerTimer.start();
    }

    /**
     * Stop the spinner animation and restore the status display.
     * Safe to call from any thread.
     */
    public void stopScanAnimation() {
        if (SwingUtilities.isEventDispatchThread()) {
            doStopScanAnimation();
        } else {
            SwingUtilities.invokeLater(this::doStopScanAnimation);
        }
    }

    private void doStopScanAnimation() {
        scanning = false;
        if (spinnerTimer != null) {
            spinnerTimer.stop();
            spinnerTimer = null;
        }
        spinnerLabel.setText("");
        // Refresh the real status colors/text
        updateStatusDisplay();
    }

    private void scanForHardware() {
        // Run the hardware detection asynchronously to avoid freezing the UI
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(this::doStartScanAnimation);

                ScanResult result = performPortScan();

                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    availableTransmitters = result.transmitters;
                    availableReceivers = result.receivers;

                    if (selectedTransmitter != null && !availableTransmitters.containsValue(selectedTransmitter)) {
                        selectedTransmitter = null;
                    }
                    if (selectedReceiver != null && !availableReceivers.containsValue(selectedReceiver)) {
                        selectedReceiver = null;
                    }

                    if (availableTransmitters.size() == 1 && selectedTransmitter == null) {
                        selectedTransmitter = availableTransmitters.values().iterator().next();
                    }
                    if (availableReceivers.size() == 1 && selectedReceiver == null) {
                        selectedReceiver = availableReceivers.values().iterator().next();
                    }

                    updateStatusDisplay();
                });

            } catch (Exception ex) {
                System.err.println("Error during hardware scanning: " + ex.getMessage());
            } finally {
                // Ensure animation stops even on error
                SwingUtilities.invokeLater(this::doStopScanAnimation);
            }
        }, "Hardware Scanner").start();
    }

    // Helper container for scan results
    private static class ScanResult {
        Map<String, SerialTransmitter> transmitters;
        Map<String, SerialTransmitter> receivers;

        ScanResult(Map<String, SerialTransmitter> t, Map<String, SerialTransmitter> r) {
            this.transmitters = t;
            this.receivers = r;
        }
    }

    // Perform a synchronous port probe and return discovered transmitters/receivers. Does not touch UI.
    private ScanResult performPortScan() {
        Map<String, SerialTransmitter> newTransmitters = new HashMap<>();
        Map<String, SerialTransmitter> newReceivers = new HashMap<>();

        SerialPort[] allPorts = SerialTransmitter.getPortNames();
        System.out.println("Hardware scan found " + allPorts.length + " total ports");

        // Show ALL ports for debugging
        for (SerialPort port : allPorts) {
            String portName = port.getDescriptivePortName();
            System.out.println("Found port: " + portName + " (System: " + port.getSystemPortName() + ")");
        }

        for (SerialPort port : allPorts) {
            String portName = port.getDescriptivePortName();
            if (!portName.toLowerCase().contains("cp210x")) {
                System.out.println("Skipping port " + portName + " (non-Emrick device)");
                continue;
            }

            try {
                String deviceType = "";

                // Always probe the device to avoid stale cache entries when hardware at a port changes
                SerialTransmitter testST = new SerialTransmitter();
                String probed = testST.getBoardType(portName);
                if (probed != null && !probed.isEmpty()) {
                    deviceType = probed;
                    System.out.println("Detected " + deviceType + " on " + portName);
                }

                if ("Transmitter".equals(deviceType)) {
                    SerialTransmitter st = new SerialTransmitter();
                    if (st.setSerialPort(portName)) {
                        newTransmitters.put(portName, st);
                    }
                } else if ("Receiver".equals(deviceType)) {
                    SerialTransmitter st = new SerialTransmitter();
                    if (st.setSerialPort(portName)) {
                        newReceivers.put(portName, st);
                    }
                }
            } catch (Exception ex) {
                // Skip this port if there's an error
                System.err.println("Error scanning port " + portName + ": " + ex.getMessage());
            }
        }


        return new ScanResult(newTransmitters, newReceivers);
    }

    /**
     * Blocking version of scanForHardware that runs on the calling thread.
     * Use this for short synchronous rescans from event handlers when caller wants an immediate result.
     * This method will still update the UI (animation start/stop) on EDT.
     */
    public void scanForHardwareBlocking() {
        // Simplify: reuse the non-blocking scanForHardware logic, but provide a modal overlay when called from EDT
        if (SwingUtilities.isEventDispatchThread()) {
            final Window owner = SwingUtilities.getWindowAncestor(this);
            final JDialog dialog = new JDialog(owner, "Hardware Scan", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            JLabel label = new JLabel("Scanning for hardware...");
            p.add(label);
            dialog.getContentPane().add(p);
            dialog.pack();
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(owner);

            // Start animation and run the existing async scan; close dialog when done
            doStartScanAnimation();
            // Run the same scan logic but on a worker thread; when complete, dispose dialog
            new Thread(() -> {
                scanForHardware();
                // scanForHardware starts its own thread and will stop animation when finished. Wait a short time
                // for the worker to complete; this keeps modal dialog visible while scan runs. We poll the scanning flag.
                while (scanning) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
                SwingUtilities.invokeLater(dialog::dispose);
            }, "Hardware Scanner (blocking EDT wrapper)").start();

            dialog.setVisible(true);
            return;
        }

    // Non-EDT callers: simply call scanForHardware() and block until scanning completes
    scanForHardware();
        // Wait for background scan to finish (polling) - avoid busy loop
        while (scanning) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void updateStatusDisplay() {
        // Update transmitter status
        if (availableTransmitters.isEmpty()) {
            transmitterLabel.setText("No TX");
            transmitterLabel.setForeground(DISCONNECTED_COLOR);
            transmitterLabel.setToolTipText("No transmitters connected");
        } else if (availableTransmitters.size() == 1) {
            String portName = getShortPortName(availableTransmitters.keySet().iterator().next());
            transmitterLabel.setText("TX: " + portName);
            transmitterLabel.setForeground(CONNECTED_COLOR);
            transmitterLabel.setToolTipText("Transmitter connected on " + portName);
        } else {
            transmitterLabel.setText("TX: " + availableTransmitters.size());
            transmitterLabel.setForeground(MULTIPLE_COLOR);
            transmitterLabel.setToolTipText("Multiple transmitters available - click to select");
        }

        // Update receiver status
        if (availableReceivers.isEmpty()) {
            receiverLabel.setText("No RX");
            receiverLabel.setForeground(DISCONNECTED_COLOR);
            receiverLabel.setToolTipText("No receivers connected");
        } else if (availableReceivers.size() == 1) {
            String portName = getShortPortName(availableReceivers.keySet().iterator().next());
            receiverLabel.setText("RX: " + portName);
            receiverLabel.setForeground(CONNECTED_COLOR);
            receiverLabel.setToolTipText("Receiver connected on " + portName);
        } else {
            receiverLabel.setText("RX: " + availableReceivers.size());
            receiverLabel.setForeground(MULTIPLE_COLOR);
            receiverLabel.setToolTipText("Multiple receivers available - click to select");
        }
    }

    private String getShortPortName(String fullPortName) {
        // Extract COM port number from full port name like "Silicon Labs CP210x USB to UART Bridge (COM3)"
        int start = fullPortName.lastIndexOf("(COM");
        int end = fullPortName.lastIndexOf(")");
        if (start != -1 && end != -1) {
            return fullPortName.substring(start + 1, end);
        }
        return "PORT";
    }

    private void handleTransmitterClick() {
        if (availableTransmitters.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No transmitters detected. Please connect an Emrick transmitter.",
                "No Transmitters",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (availableTransmitters.size() == 1) {
            selectedTransmitter = availableTransmitters.values().iterator().next();
            return;
        }

        // Multiple transmitters - show selection dialog
        String[] portNames = availableTransmitters.keySet().toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Select a transmitter:",
            "Choose Transmitter",
            JOptionPane.QUESTION_MESSAGE,
            null,
            portNames,
            selectedTransmitter != null ? getPortNameForTransmitter(selectedTransmitter) : portNames[0]
        );

        if (selected != null) {
            selectedTransmitter = availableTransmitters.get(selected);
        }
    }

    private void handleReceiverClick() {
        if (availableReceivers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No receivers detected. Please connect an Emrick receiver.",
                "No Receivers",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (availableReceivers.size() == 1) {
            selectedReceiver = availableReceivers.values().iterator().next();
            return;
        }

        // Multiple receivers - show selection dialog
        String[] portNames = availableReceivers.keySet().toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Select a receiver:",
            "Choose Receiver",
            JOptionPane.QUESTION_MESSAGE,
            null,
            portNames,
            selectedReceiver != null ? getPortNameForReceiver(selectedReceiver) : portNames[0]
        );

        if (selected != null) {
            selectedReceiver = availableReceivers.get(selected);
        }
    }

    private String getPortNameForTransmitter(SerialTransmitter st) {
        for (Map.Entry<String, SerialTransmitter> entry : availableTransmitters.entrySet()) {
            if (entry.getValue() == st) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String getPortNameForReceiver(SerialTransmitter st) {
        for (Map.Entry<String, SerialTransmitter> entry : availableReceivers.entrySet()) {
            if (entry.getValue() == st) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get the currently selected or available transmitter
     * @return SerialTransmitter object or null if none available
     */
    public SerialTransmitter getTransmitter() {
        // Trigger a fresh scan if no transmitters found
        if (availableTransmitters.isEmpty()) {
            scanForHardwareBlocking();
        }

        if (selectedTransmitter != null) {
            if (isValidTransmitter(selectedTransmitter)) return selectedTransmitter;
            // selected transmitter failed sanity check, clear it and continue
            availableTransmitters.values().removeIf(st -> st == selectedTransmitter);
            selectedTransmitter = null;
            return getTransmitter();
        }

        if (availableTransmitters.size() == 1) {
            SerialTransmitter st = availableTransmitters.values().iterator().next();
            if (isValidTransmitter(st)) {
                selectedTransmitter = st;
                return selectedTransmitter;
            }
            // invalid transmitter found
            return null;
        }

        if (availableTransmitters.size() > 1) {
            // Multiple available - trigger selection
            handleTransmitterClick();
            return selectedTransmitter;
        }

        return null; // None available
    }

    /**
     * Get the currently selected or available receiver
     * @return SerialTransmitter object or null if none available
     */
    public SerialTransmitter getReceiver() {
        // Trigger a fresh scan if no receivers found
        if (availableReceivers.isEmpty()) {
            scanForHardwareBlocking();
        }

        if (selectedReceiver != null) {
            if (isValidReceiver(selectedReceiver)) return selectedReceiver;
            // selected receiver failed sanity check, clear it and continue
            availableReceivers.values().removeIf(st -> st == selectedReceiver);
            selectedReceiver = null;
            return getReceiver();
        }

        if (availableReceivers.size() == 1) {
            SerialTransmitter st = availableReceivers.values().iterator().next();
            if (isValidReceiver(st)) {
                selectedReceiver = st;
                return selectedReceiver;
            }
            // invalid receiver found
            return null;
        }

        if (availableReceivers.size() > 1) {
            // Multiple available - trigger selection
            handleReceiverClick();
            return selectedReceiver;
        }

        return null; // None available
    }

    // Sanity checks: ensure the SerialTransmitter has a backing SerialPort and a plausible type
    private boolean isValidTransmitter(SerialTransmitter st) {
        if (st == null) return false;
        try {
            if (st.getSerialPort() == null) return false;
            // Try to find the port name associated with this transmitter
            String portName = getPortNameForTransmitter(st);
            if (portName == null) return false;

            // Actively query the device to confirm type. getBoardType will attempt to open the port.
            String deviceType = st.getBoardType(portName);
            if (deviceType != null && deviceType.equalsIgnoreCase("Transmitter")) {
                return true;
            }

            // If deviceType is empty, port may be busy/disconnected; remove it from available maps and cache
            if (deviceType == null || deviceType.isEmpty()) {
                availableTransmitters.remove(portName);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidReceiver(SerialTransmitter st) {
        if (st == null) return false;
        try {
            if (st.getSerialPort() == null) return false;
            String portName = getPortNameForReceiver(st);
            if (portName == null) return false;

            String deviceType = st.getBoardType(portName);
            if (deviceType != null && deviceType.equalsIgnoreCase("Receiver")) {
                return true;
            }

            if (deviceType == null || deviceType.isEmpty()) {
                availableReceivers.remove(portName);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a transmitter is available
     * @return true if at least one transmitter is connected
     */
    public boolean isTransmitterAvailable() {
        return !availableTransmitters.isEmpty();
    }

    /**
     * Check if a receiver is available
     * @return true if at least one receiver is connected
     */
    public boolean isReceiverAvailable() {
        return !availableReceivers.isEmpty();
    }

    /**
     * Stop the hardware scanning timer
     */
    public void stopScanning() {
        if (scanTimer != null) {
            scanTimer.stop();
        }
        // also stop spinner if active
        stopScanAnimation();
    }
}