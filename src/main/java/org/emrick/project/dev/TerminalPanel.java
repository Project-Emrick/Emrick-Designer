package org.emrick.project.dev;

import com.fazecast.jSerialComm.SerialPort;
import org.emrick.project.EmrickFileChooser;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Live terminal for either Emrick Designer's own log or a unit's serial port (9600 baud).
 *
 * Connecting to a receiver turns its debug output on for the session (and off again on disconnect). Text
 * typed to a transmitter is checked first, because the transmitter treats every character as a command
 * that is broadcast to every unit (e.g. 'e' puts them all to sleep).
 */
class TerminalPanel extends JPanel {
    private static final String OWNER = "Terminal";
    private static final String APP_LOG = "Emrick Designer (app log)";
    private static final int MAX_LINES = 5000;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS");
    // The console stays dark in both themes, like a regular terminal
    private static final Color CONSOLE_BG = new Color(0x141414);
    private static final Color CONSOLE_FG = new Color(0xd6d6d6);

    /** Transmitter command characters that affect every unit and need confirmation. */
    private static final Map<Character, String> RISKY_TRANSMITTER_COMMANDS = new LinkedHashMap<>();

    static {
        RISKY_TRANSMITTER_COMMANDS.put('e', "MASS SLEEP: puts every unit to sleep");
        RISKY_TRANSMITTER_COMMANDS.put('r', "MASS RESET: restarts every unit");
        RISKY_TRANSMITTER_COMMANDS.put('p', "PROGRAMMING: starts wireless show programming");
        RISKY_TRANSMITTER_COMMANDS.put('d', "STORAGE MODE: puts every unit in storage mode");
    }

    private final DevModeHost host;
    private final JComboBox<String> sourceBox = new JComboBox<>();
    private final JButton connectButton = DevUI.primary("Connect");
    private final JButton resetButton = DevUI.secondary("Reset Unit");
    private final JCheckBox debugBox = new JCheckBox("Receiver debug output", true);
    private final JCheckBox autoScrollBox = new JCheckBox("Auto-scroll", true);
    private final JCheckBox timestampBox = new JCheckBox("Timestamps", true);
    private final JCheckBox pauseBox = new JCheckBox("Pause");
    private final JTextArea output = new JTextArea();
    private final JTextField input = DevUI.field(20, "Type a command and press Enter  (receiver: info, debug on, blink, help)");
    private final JComboBox<String> lineEndingBox = new JComboBox<>(new String[]{"LF (\\n)", "CRLF (\\r\\n)", "No line ending"});
    private final JButton sendButton = DevUI.primary("Send");
    private final DevUI.Pill statusPill = new DevUI.Pill("Not connected", DevUI.Tone.NEUTRAL);
    private final List<String> pausedLines = new ArrayList<>();

    // Connection state
    private SerialPort port;
    private String portName;
    private String unitType = "";
    private boolean debugTurnedOn = false;
    private Thread readerThread;
    private volatile boolean reading = false;
    private Consumer<AppLog.Entry> appLogListener;

    TerminalPanel(DevModeHost host) {
        super(new BorderLayout(0, DevUI.GAP));
        this.host = host;
        setOpaque(false);

        // --- Connection toolbar ---
        DevUI.Card toolbar = new DevUI.Card(new BorderLayout(DevUI.GAP, 0));
        JPanel left = DevUI.clear(new DevUI.WrapLayout(FlowLayout.LEFT, 8, 4));
        left.add(DevUI.caption("Source"));
        sourceBox.setPrototypeDisplayValue("Emrick Designer (app log)    ");
        sourceBox.setRenderer(DevUI.unitNameRenderer()); // board labels instead of bare COM ports
        left.add(sourceBox);
        JButton refreshPorts = DevUI.secondary("Refresh");
        refreshPorts.setToolTipText("Look for newly plugged-in units");
        refreshPorts.addActionListener(e -> refreshSources());
        left.add(refreshPorts);
        left.add(connectButton);
        left.add(statusPill);
        toolbar.add(left, BorderLayout.CENTER);
        JPanel right = DevUI.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        debugBox.setOpaque(false);
        debugBox.setToolTipText("Turn on the receiver's debug messages while connected (off again on disconnect)");
        right.add(debugBox);
        resetButton.setToolTipText("Restart the unit to see its boot messages");
        right.add(resetButton);
        toolbar.add(right, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // --- Console ---
        JPanel console = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CONSOLE_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(DevUI.cardBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        console.setOpaque(false);
        console.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 6));
        output.setEditable(false);
        output.setLineWrap(true); // long lines wrap instead of needing a horizontal scrollbar
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setBackground(CONSOLE_BG);
        output.setForeground(CONSOLE_FG);
        output.setCaretColor(CONSOLE_FG);
        output.setSelectionColor(new Color(0x3a4a66));
        output.setSelectedTextColor(Color.WHITE);
        ((DefaultCaret) output.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        JScrollPane outputScroll = new JScrollPane(output);
        outputScroll.setBorder(BorderFactory.createEmptyBorder());
        outputScroll.getViewport().setBackground(CONSOLE_BG);
        console.add(outputScroll, BorderLayout.CENTER);
        add(console, BorderLayout.CENTER);

        // --- Input and options ---
        DevUI.Card bottom = new DevUI.Card(new BorderLayout(0, 10));
        JPanel inputRow = DevUI.clear(new BorderLayout(8, 0));
        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        inputRow.add(input, BorderLayout.CENTER);
        JPanel sendGroup = DevUI.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sendGroup.add(lineEndingBox);
        sendGroup.add(sendButton);
        inputRow.add(sendGroup, BorderLayout.EAST);
        bottom.add(inputRow, BorderLayout.NORTH);

        JPanel options = DevUI.clear(new BorderLayout());
        JPanel toggles = DevUI.clear(new DevUI.WrapLayout(FlowLayout.LEFT, 12, 0));
        for (JCheckBox box : new JCheckBox[]{autoScrollBox, timestampBox, pauseBox}) {
            box.setOpaque(false);
            toggles.add(box);
        }
        options.add(toggles, BorderLayout.CENTER);
        JPanel logActions = DevUI.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton clear = DevUI.secondary("Clear");
        clear.addActionListener(e -> output.setText(""));
        logActions.add(clear);
        JButton save = DevUI.secondary("Save Log...");
        save.setToolTipText("Save what's shown to a text file on this computer");
        save.addActionListener(e -> saveLog());
        logActions.add(save);
        options.add(logActions, BorderLayout.EAST);
        bottom.add(options, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> {
            if (isConnected()) {
                disconnect();
            } else {
                connect();
            }
        });
        resetButton.addActionListener(e -> resetUnit());
        sendButton.addActionListener(e -> send());
        input.addActionListener(e -> send());
        pauseBox.addActionListener(e -> {
            if (!pauseBox.isSelected()) {
                pausedLines.forEach(this::appendNow);
                pausedLines.clear();
            }
        });

        refreshSources();
        updateControls();
        appendNow("Choose a source above and press Connect. Receivers: type 'help' to list commands.");
    }

    private boolean isConnected() {
        return port != null || appLogListener != null;
    }

    private void refreshSources() {
        Object current = sourceBox.getSelectedItem();
        sourceBox.removeAllItems();
        sourceBox.addItem(APP_LOG);
        for (SerialPort sp : UnitPort.emrickPorts()) {
            sourceBox.addItem(sp.getSystemPortName());
        }
        if (current != null) {
            sourceBox.setSelectedItem(current);
        }
    }

    private void updateControls() {
        boolean connected = isConnected();
        boolean serial = port != null;
        connectButton.setText(connected ? "Disconnect" : "Connect");
        DevUI.setPrimary(connectButton, !connected);
        sourceBox.setEnabled(!connected);
        resetButton.setEnabled(serial);
        input.setEnabled(serial);
        sendButton.setEnabled(serial);
        lineEndingBox.setEnabled(serial);
        debugBox.setEnabled(!connected);
    }

    // ---------------------------------------------------------------- connecting

    private void connect() {
        String source = (String) sourceBox.getSelectedItem();
        if (source == null) {
            return;
        }
        if (APP_LOG.equals(source)) {
            for (AppLog.Entry entry : AppLog.snapshot()) {
                appendLine(format(entry));
            }
            appLogListener = entry -> SwingUtilities.invokeLater(() -> appendLine(format(entry)));
            AppLog.addListener(appLogListener);
            statusPill.set("Showing app log", DevUI.Tone.ACCENT);
            updateControls();
            return;
        }

        // Flashing takes priority: if this unit gets flashed, the terminal lets go of the port and stops
        Runnable letGo = () -> SwingUtilities.invokeLater(() -> {
            if (source.equals(portName)) {
                appendLine("[Disconnected: " + source + " is being flashed]");
                if (port == null) {
                    portName = null; // still connecting; the connect thread hands the port back
                } else {
                    closePort(false);
                }
            }
        });
        portName = source;
        statusPill.set("Connecting to " + source + "...", DevUI.Tone.NEUTRAL);
        connectButton.setEnabled(false);
        sourceBox.setEnabled(false);
        boolean wantDebug = debugBox.isSelected();

        new Thread(() -> {
            // Wait for quick users (e.g. the Units tab reading this unit) instead of failing straight away
            String problem = null;
            SerialPort sp = null;
            if (!UnitPort.claimWhenFree(source, OWNER, letGo, 8000)) {
                problem = source + " is being used by " + UnitPort.ownerOf(source) + ".";
            } else {
                sp = UnitPort.find(source);
                if (sp == null || !UnitPort.openWithRetry(sp, 4)) {
                    UnitPort.release(source, OWNER);
                    problem = "Couldn't open " + source + ". Close any other program using it and try again.";
                }
            }
            if (problem != null) {
                String message = problem;
                SwingUtilities.invokeLater(() -> {
                    portName = null;
                    connectButton.setEnabled(true);
                    statusPill.set("Not connected", DevUI.Tone.NEUTRAL);
                    updateControls();
                    JOptionPane.showMessageDialog(this, message, "Port Unavailable", JOptionPane.WARNING_MESSAGE);
                });
                return;
            }
            SerialPort opened = sp;
            SwingUtilities.invokeLater(() -> {
                if (!source.equals(portName)) {
                    // Taken over (e.g. by the flasher) before the connection finished
                    opened.closePort();
                    UnitPort.release(source, OWNER);
                    connectButton.setEnabled(true);
                    statusPill.set("Not connected", DevUI.Tone.NEUTRAL);
                    updateControls();
                    return;
                }
                port = opened;
                updateControls();
            });
            String type = UnitPort.probeType(opened);
            boolean debugOn = UnitPort.RECEIVER.equals(type) && wantDebug && UnitPort.setDebug(opened, true);
            SwingUtilities.invokeLater(() -> {
                connectButton.setEnabled(true);
                if (port != opened) {
                    return; // disconnected (or flashed) while connecting
                }
                unitType = type;
                debugTurnedOn = debugOn;
                appendLine("[connected to " + source + " - " + type
                        + (debugOn ? ", debug output on" : "") + "]");
                String name = UnitPort.displayName(source);
                statusPill.set((name.equals(source) ? type : name) + "  \u00b7  " + source,
                        UnitPort.UNKNOWN.equals(type) ? DevUI.Tone.WARNING : DevUI.Tone.SUCCESS);
                startReader(opened);
            });
        }, "Terminal connect").start();
    }

    private void startReader(SerialPort sp) {
        reading = true;
        readerThread = new Thread(() -> {
            StringBuilder line = new StringBuilder();
            byte[] buf = new byte[256];
            while (reading) {
                if (!sp.isOpen()) {
                    break;
                }
                int available = sp.bytesAvailable();
                if (available < 0) {
                    break; // unplugged
                }
                if (available == 0) {
                    UnitPort.sleep(20);
                    continue;
                }
                int n = sp.readBytes(buf, Math.min(buf.length, available));
                for (int i = 0; i < n; i++) {
                    char c = (char) (buf[i] & 0xFF);
                    if (c == '\n') {
                        String text = line.toString();
                        line.setLength(0);
                        SwingUtilities.invokeLater(() -> appendLine(stamp() + text));
                    } else if (c != '\r') {
                        line.append(c >= 32 && c < 127 || c == '\t' ? c : '?');
                    }
                }
            }
            if (reading) {
                SwingUtilities.invokeLater(() -> {
                    appendLine("[" + sp.getSystemPortName() + " disconnected - unit unplugged?]");
                    closePort(false);
                });
            }
        }, "Terminal reader " + sp.getSystemPortName());
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void disconnect() {
        if (appLogListener != null) {
            AppLog.removeListener(appLogListener);
            appLogListener = null;
            appendLine("[stopped showing Emrick Designer log]");
            statusPill.set("Not connected", DevUI.Tone.NEUTRAL);
            updateControls();
            return;
        }
        closePort(true);
    }

    /** @param tidy turn the receiver's debug output back off before closing (skipped if the unit is gone) */
    private void closePort(boolean tidy) {
        if (port == null) {
            return;
        }
        reading = false;
        SerialPort sp = port;
        String name = portName;
        boolean turnDebugOff = tidy && debugTurnedOn;
        port = null;
        portName = null;
        debugTurnedOn = false;
        new Thread(() -> {
            if (readerThread != null) {
                try {
                    readerThread.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (turnDebugOff) {
                UnitPort.setDebug(sp, false);
            }
            sp.closePort();
            UnitPort.release(name, OWNER);
        }, "Terminal disconnect").start();
        appendLine("[disconnected from " + name + "]");
        statusPill.set("Not connected", DevUI.Tone.NEUTRAL);
        updateControls();
    }

    void shutdown() {
        if (isConnected()) {
            disconnect();
        }
    }

    // ---------------------------------------------------------------- sending

    private void send() {
        if (port == null) {
            return;
        }
        String text = input.getText();
        if (!UnitPort.RECEIVER.equals(unitType) && !confirmTransmitterText(text)) {
            return;
        }
        String ending = switch (lineEndingBox.getSelectedIndex()) {
            case 0 -> "\n";
            case 1 -> "\r\n";
            default -> "";
        };
        SerialPort sp = port;
        new Thread(() -> UnitPort.write(sp, text + ending), "Terminal send").start();
        appendLine(stamp() + "> " + text);
        input.setText("");
    }

    /**
     * Transmitters read every character as a broadcast command, so warn before sending anything that would
     * affect every unit. Units that couldn't be identified are treated the same way to be safe.
     */
    private boolean confirmTransmitterText(String text) {
        StringBuilder risky = new StringBuilder();
        for (Map.Entry<Character, String> cmd : RISKY_TRANSMITTER_COMMANDS.entrySet()) {
            if (text.indexOf(cmd.getKey()) >= 0) {
                risky.append("\n  '").append(cmd.getKey()).append("'  ").append(cmd.getValue());
            }
        }
        if (risky.length() == 0) {
            return true;
        }
        String who = UnitPort.TRANSMITTER.equals(unitType) ? "the transmitter" : "this unit (it could not be identified, so it is treated as a transmitter)";
        int choice = JOptionPane.showConfirmDialog(this,
                "Every character sent to " + who + " is broadcast to ALL units as a command.\n"
                        + "This text contains:" + risky + "\n\nSend it anyway?",
                "Confirm Broadcast", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void resetUnit() {
        if (port == null) {
            return;
        }
        SerialPort sp = port;
        boolean reenableDebug = debugTurnedOn;
        appendLine("[resetting unit]");
        new Thread(() -> {
            UnitPort.reset(sp);
            // Debug output is off after every boot; turn it back on once the unit is up
            if (reenableDebug) {
                UnitPort.sleep(3000);
                UnitPort.write(sp, "debug on\n");
            }
        }, "Terminal reset").start();
    }

    // ---------------------------------------------------------------- output

    private String stamp() {
        return timestampBox.isSelected() ? "[" + TIME.format(new Date()) + "] " : "";
    }

    private String format(AppLog.Entry entry) {
        String prefix = timestampBox.isSelected() ? "[" + TIME.format(new Date(entry.timeMillis())) + "] " : "";
        return prefix + (entry.error() ? "ERR " : "") + entry.text();
    }

    private void appendLine(String text) {
        if (pauseBox.isSelected()) {
            pausedLines.add(text);
            if (pausedLines.size() > MAX_LINES) {
                pausedLines.remove(0);
            }
            return;
        }
        appendNow(text);
    }

    private void appendNow(String text) {
        output.append(text + "\n");
        int excess = output.getLineCount() - MAX_LINES;
        if (excess > 0) {
            try {
                output.replaceRange("", 0, output.getLineEndOffset(excess - 1));
            } catch (BadLocationException ignored) {
            }
        }
        if (autoScrollBox.isSelected()) {
            output.setCaretPosition(output.getDocument().getLength());
        }
    }

    private void saveLog() {
        File file = EmrickFileChooser.chooseSaveFile(host.getFrame(), "TERMINAL_LOG_SAVE", "Save Terminal Log",
                "Text Files (*.txt)", ".txt");
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
        }
        try {
            Files.writeString(file.toPath(), output.getText(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Couldn't save the log: " + e.getMessage(), "Save Log", JOptionPane.ERROR_MESSAGE);
        }
    }
}
