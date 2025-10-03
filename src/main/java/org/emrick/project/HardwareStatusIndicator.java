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
    private Map<String, String> portTypeCache = new HashMap<>(); // Cache to avoid repeated hardware queries
    private SerialTransmitter selectedTransmitter = null;
    private SerialTransmitter selectedReceiver = null;
    
    // UI Components
    private JLabel transmitterLabel;
    private JLabel receiverLabel;
    private Timer scanTimer;
    private MediaEditorGUI parent;
    
    // Status colors
    private static final Color CONNECTED_COLOR = new Color(34, 139, 34); // Forest Green
    private static final Color DISCONNECTED_COLOR = new Color(220, 20, 60); // Crimson
    private static final Color MULTIPLE_COLOR = new Color(255, 140, 0); // Dark Orange
    
    public HardwareStatusIndicator(MediaEditorGUI parent) {
        this.parent = parent;
        initializeUI();
        
        // Set fixed size to prevent stretching in MenuBar
        setPreferredSize(new Dimension(120, 32));
        setMinimumSize(new Dimension(120, 32));
        setMaximumSize(new Dimension(120, 32));

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
        
        JLabel txIcon = new JLabel("📡");
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
        
        JLabel rxIcon = new JLabel("📻");
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
        
        // Refresh button on the right side - make it non-focusable
        JButton refreshButton = new JButton("🔄");
        refreshButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        refreshButton.setToolTipText("Refresh Hardware Detection");
        refreshButton.setPreferredSize(new Dimension(20, 20));
        refreshButton.setMargin(new Insets(0, 0, 0, 0));
        refreshButton.setFocusable(false); // Prevent focus stealing
        refreshButton.addActionListener(e -> {
            portTypeCache.clear(); // Clear cache to force fresh detection
            scanForHardware();
        });
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
    
    private void scanForHardware() {
        // Run the hardware detection in a background thread to avoid freezing UI
        new Thread(() -> {
            try {
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
                    
                    try {
                        String deviceType;
                        
                        // Check cache first to avoid repeated hardware queries
                        if (portTypeCache.containsKey(portName)) {
                            deviceType = portTypeCache.get(portName);
                        } else {
                            // Use the original SerialTransmitter.getBoardType() method directly
                            SerialTransmitter testST = new SerialTransmitter();
                            deviceType = testST.getBoardType(portName);
                            
                            if (deviceType != null && !deviceType.isEmpty()) {
                                portTypeCache.put(portName, deviceType);
                                System.out.println("Detected " + deviceType + " on " + portName);
                            }
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
                        // Skip this port if there's an error and remove from cache
                        portTypeCache.remove(portName);
                        System.err.println("Error scanning port " + portName + ": " + ex.getMessage());
                    }
                }
                
                // Clean up cache for ports that no longer exist
                portTypeCache.entrySet().removeIf(entry -> {
                    String cachedPortName = entry.getKey();
                    boolean portExists = false;
                    for (SerialPort port : allPorts) {
                        if (port.getDescriptivePortName().equals(cachedPortName)) {
                            portExists = true;
                            break;
                        }
                    }
                    return !portExists;
                });
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    // Update our collections
                    availableTransmitters = newTransmitters;
                    availableReceivers = newReceivers;
                    
                    // Update selected devices if they're no longer available
                    if (selectedTransmitter != null && !availableTransmitters.containsValue(selectedTransmitter)) {
                        selectedTransmitter = null;
                    }
                    if (selectedReceiver != null && !availableReceivers.containsValue(selectedReceiver)) {
                        selectedReceiver = null;
                    }
                    
                    // Auto-select if only one device of each type
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
            }
        }, "Hardware Scanner").start();
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
            scanForHardware();
            // Wait a bit for scan to complete
            /*try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }*/
        }
        
        if (selectedTransmitter != null) {
            return selectedTransmitter;
        }
        
        if (availableTransmitters.size() == 1) {
            selectedTransmitter = availableTransmitters.values().iterator().next();
            return selectedTransmitter;
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
            scanForHardware();
            // Wait a bit for scan to complete
            /*try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }*/
        }
        
        if (selectedReceiver != null) {
            return selectedReceiver;
        }
        
        if (availableReceivers.size() == 1) {
            selectedReceiver = availableReceivers.values().iterator().next();
            return selectedReceiver;
        }
        
        if (availableReceivers.size() > 1) {
            // Multiple available - trigger selection
            handleReceiverClick();
            return selectedReceiver;
        }
        
        return null; // None available
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
    }
}