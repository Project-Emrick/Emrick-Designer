package org.emrick.project.dev;

import com.fazecast.jSerialComm.SerialPort;
import org.emrick.project.LEDStrip;
import org.emrick.project.dev.DevUI.Card;
import org.emrick.project.dev.DevUI.Pill;
import org.emrick.project.dev.DevUI.Tone;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Finds units over LoRa through the transmitter. Lookup lights one unit (by board label and/or ID) white until
 * stopped; the sweep steps through every board ID one at a time so a person can spot duplicate or missing IDs.
 * Only one unit is lit at a time: sending a new ID turns the previous one off.
 */
class LookupPanel extends JPanel {
    private static final String OWNER = "Lookup";

    private final DevModeHost host;
    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Lookup sender");
        t.setDaemon(true);
        return t;
    });

    // Connection strip
    private final JComboBox<String> transmitterBox = new JComboBox<>();
    private final Pill transmitterPill = new Pill("", Tone.NEUTRAL);
    private final JTextArea projectSummary = DevUI.wrappingCaption("");
    private final Pill projectPill = new Pill("", Tone.NEUTRAL);
    private final JButton openProjectButton = DevUI.secondary("Open Project...");

    // Find a unit
    private final JTextField labelField = DevUI.field(10, "e.g. T10L");
    private final JTextField idField = DevUI.field(10, "e.g. 42");
    private final JLabel litId = new JLabel("-", SwingConstants.CENTER);
    private final JLabel litLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Pill litPill = new Pill("Nothing lit", Tone.NEUTRAL);
    private final JLabel lookupMessage = DevUI.caption(" ");

    // Sweep
    private final JSpinner startSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65534, 1));
    private final JSpinner endSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65534, 1));
    private final JButton startButton = DevUI.primary("Start Sweep");
    private final JButton stopButton = DevUI.danger("Stop Sweep");
    private final JLabel sweepId = new JLabel("-", SwingConstants.CENTER);
    private final JLabel sweepExpected = DevUI.caption("Choose a range and press Start Sweep");
    private final JProgressBar sweepProgress = new JProgressBar();
    private final JButton prevButton = DevUI.secondary("\u2039  Previous");
    private final JButton resendButton = DevUI.secondary("Resend");
    private final JButton nextButton = DevUI.primary("Next  \u203a");
    private final JCheckBox autoBox = new JCheckBox("Auto-advance every");
    private final JSpinner autoSeconds = new JSpinner(new SpinnerNumberModel(3.0, 0.5, 60.0, 0.5));
    private final Timer autoTimer = new Timer(3000, e -> step(1));
    private final Card sweepCard = new Card(new BorderLayout(0, DevUI.GAP));

    private Integer currentSweepId = null; // current ID while a sweep is running
    private boolean unitLit = false;
    private boolean detectedOnce = false;

    LookupPanel(DevModeHost host) {
        super(new BorderLayout(0, DevUI.GAP));
        this.host = host;
        setOpaque(false);

        JPanel content = DevUI.clear(new BorderLayout(0, DevUI.GAP));
        content.add(buildConnectionStrip(), BorderLayout.NORTH);
        JPanel main = DevUI.clear(new DevUI.ResponsiveGrid(400, 2, DevUI.GAP, true));
        main.add(buildFindCard());
        main.add(buildSweepCard());
        content.add(main, BorderLayout.CENTER);
        add(DevUI.reflowScroll(content), BorderLayout.CENTER);

        bindKey(KeyEvent.VK_SPACE, "next", () -> step(1));
        bindKey(KeyEvent.VK_RIGHT, "next2", () -> step(1));
        bindKey(KeyEvent.VK_ENTER, "resend", () -> step(0));
        bindKey(KeyEvent.VK_BACK_SPACE, "prev", () -> step(-1));
        bindKey(KeyEvent.VK_LEFT, "prev2", () -> step(-1));
        sweepCard.setFocusable(true);

        refreshPorts(false);
        updateSweepControls();
        bigText(litId, null, "No unit lit", 26f);
        bigText(sweepId, null, "Not running", 60f);
    }

    // ---------------------------------------------------------------- layout

    private JComponent buildConnectionStrip() {
        JPanel strip = DevUI.clear(new DevUI.ResponsiveGrid(400, 2, DevUI.GAP, false));

        Card tx = new Card(new BorderLayout(DevUI.GAP, 0));
        JPanel txText = DevUI.clear(new BorderLayout(0, 2));
        txText.add(DevUI.section("Transmitter"), BorderLayout.NORTH);
        txText.add(DevUI.wrappingCaption("Sends lookups to every unit over LoRa"), BorderLayout.CENTER);
        tx.add(txText, BorderLayout.CENTER);
        JPanel txControls = DevUI.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        transmitterBox.setPrototypeDisplayValue("Transmitter  COM100");
        transmitterBox.setRenderer(DevUI.unitNameRenderer()); // "Transmitter" / board labels instead of bare COM ports
        transmitterBox.addActionListener(e -> updateTransmitterPill());
        txControls.add(transmitterPill);
        txControls.add(transmitterBox);
        JButton detect = DevUI.secondary("Detect");
        detect.setToolTipText("Find which plugged-in unit is the transmitter");
        detect.addActionListener(e -> refreshPorts(true));
        txControls.add(detect);
        tx.add(centerVertically(txControls), BorderLayout.EAST);
        strip.add(tx);

        Card project = new Card(new BorderLayout(DevUI.GAP, 0));
        JPanel projText = DevUI.clear(new BorderLayout(0, 2));
        projText.add(DevUI.section("Project"), BorderLayout.NORTH);
        projText.add(projectSummary, BorderLayout.CENTER);
        project.add(projText, BorderLayout.CENTER);
        JPanel projControls = DevUI.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        projControls.add(projectPill);
        openProjectButton.addActionListener(e -> host.openProjectForDevMode());
        projControls.add(openProjectButton);
        project.add(centerVertically(projControls), BorderLayout.EAST);
        strip.add(project);
        return strip;
    }

    private JComponent buildFindCard() {
        Card card = new Card(new BorderLayout(0, DevUI.GAP));
        JPanel head = DevUI.clear(new BorderLayout(0, 4));
        head.add(DevUI.title("Find a Unit"), BorderLayout.NORTH);
        head.add(DevUI.wrappingCaption("Lights one unit solid white until you turn it off, look up another, or press its button."),
                BorderLayout.CENTER);

        JPanel form = DevUI.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(DevUI.caption("Board label"), c);
        c.insets = new Insets(0, 0, 10, 0);
        styleBigField(labelField);
        form.add(labelField, c);
        c.insets = new Insets(0, 0, 4, 0);
        form.add(DevUI.caption("Board ID (optional if you entered a label)"), c);
        c.insets = new Insets(0, 0, 14, 0);
        styleBigField(idField);
        form.add(idField, c);

        JPanel buttons = DevUI.clear(new GridLayout(1, 2, 10, 0));
        JButton light = DevUI.primary("Light Up");
        light.addActionListener(e -> lookUp());
        buttons.add(light);
        JButton off = DevUI.danger("Turn Off");
        off.addActionListener(e -> allOff());
        buttons.add(off);
        c.insets = new Insets(0, 0, 6, 0);
        form.add(buttons, c);
        c.insets = new Insets(0, 0, 0, 0);
        form.add(lookupMessage, c);
        labelField.addActionListener(e -> lookUp());
        idField.addActionListener(e -> lookUp());

        JPanel top = DevUI.clear(new BorderLayout(0, DevUI.GAP));
        top.add(head, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        // Currently lit unit
        Tile lit = new Tile(new BorderLayout(0, 4));
        JPanel litHead = DevUI.clear(new BorderLayout());
        litHead.add(DevUI.section("Currently lit"), BorderLayout.WEST);
        litHead.add(litPill, BorderLayout.EAST);
        lit.add(litHead, BorderLayout.NORTH);
        lit.add(litId, BorderLayout.CENTER);
        litLabel.setFont(DevUI.subheading());
        lit.add(litLabel, BorderLayout.SOUTH);
        card.add(lit, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildSweepCard() {
        JPanel head = DevUI.clear(new BorderLayout(0, 4));
        head.add(DevUI.title("ID Sweep"), BorderLayout.NORTH);
        head.add(DevUI.wrappingCaption("Lights each board ID in turn. One unit should light each time: none means missing, two means a duplicate."),
                BorderLayout.CENTER);

        JPanel range = DevUI.clear(new DevUI.WrapLayout(FlowLayout.LEFT, 8, 4));
        range.add(DevUI.caption("From ID"));
        range.add(startSpinner);
        range.add(DevUI.caption("to"));
        range.add(endSpinner);
        range.add(Box.createHorizontalStrut(8));
        range.add(startButton);
        range.add(stopButton);

        JPanel top = DevUI.clear(new BorderLayout(0, DevUI.GAP));
        top.add(head, BorderLayout.NORTH);
        top.add(range, BorderLayout.CENTER);
        sweepCard.add(top, BorderLayout.NORTH);

        // Big current ID
        Tile tile = new Tile(new BorderLayout(0, 6));
        tile.add(DevUI.section("Board ID"), BorderLayout.NORTH);
        tile.add(sweepId, BorderLayout.CENTER);
        JPanel under = DevUI.clear(new BorderLayout(0, 8));
        sweepExpected.setHorizontalAlignment(SwingConstants.CENTER);
        under.add(sweepExpected, BorderLayout.NORTH);
        sweepProgress.setStringPainted(true);
        sweepProgress.setString("");
        under.add(sweepProgress, BorderLayout.SOUTH);
        tile.add(under, BorderLayout.SOUTH);
        sweepCard.add(tile, BorderLayout.CENTER);

        // Navigation with keyboard hints
        JPanel nav = DevUI.clear(new GridLayout(2, 3, 10, 4));
        for (JButton b : new JButton[]{prevButton, resendButton, nextButton}) {
            b.setFocusable(false); // keep focus on the card so the keyboard shortcuts keep working
            b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE_CLASS, "large");
            nav.add(b);
        }
        nav.add(keyHint("Backspace  or  \u2190"));
        nav.add(keyHint("Enter"));
        nav.add(keyHint("Space  or  \u2192"));
        JPanel auto = DevUI.clear(new DevUI.WrapLayout(FlowLayout.CENTER, 8, 0));
        autoBox.setOpaque(false);
        auto.add(autoBox);
        auto.add(autoSeconds);
        auto.add(DevUI.caption("seconds"));
        JPanel bottom = DevUI.clear(new BorderLayout(0, 10));
        bottom.add(nav, BorderLayout.CENTER);
        bottom.add(auto, BorderLayout.SOUTH);
        sweepCard.add(bottom, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startSweep());
        stopButton.addActionListener(e -> stopSweep());
        prevButton.addActionListener(e -> step(-1));
        resendButton.addActionListener(e -> step(0));
        nextButton.addActionListener(e -> step(1));
        autoBox.addActionListener(e -> updateAutoTimer());
        autoSeconds.addChangeListener(e -> updateAutoTimer());
        return sweepCard;
    }

    /** Rounded inset panel used for the big displays. */
    private static class Tile extends JPanel {
        Tile(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(DevUI.dark() ? DevUI.blend(DevUI.cardBackground(), Color.WHITE, 0.04f)
                    : DevUI.blend(DevUI.cardBackground(), DevUI.background(), 0.7f));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Shows a value in a large bold font, or a muted placeholder at a readable size when there is no value.
     */
    private static void bigText(JLabel label, String value, String placeholder, float grow) {
        Font base = UIManager.getFont("Label.font");
        if (value != null) {
            label.setText(value);
            label.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + grow));
            label.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            label.setText(placeholder);
            label.setFont(base);
            label.setForeground(DevUI.muted());
        }
    }

    private static JComponent keyHint(String text) {
        JPanel p = DevUI.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.add(DevUI.keycap(text));
        return p;
    }

    private static JComponent centerVertically(JComponent c) {
        JPanel p = DevUI.clear(new GridBagLayout());
        p.add(c);
        return p;
    }

    private static void styleBigField(JTextField f) {
        f.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "margin: 8,12,8,12");
    }

    private void bindKey(int key, String name, Runnable action) {
        sweepCard.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), name);
        sweepCard.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
        sweepCard.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentSweepId != null) {
                    action.run();
                }
            }
        });
    }

    // ---------------------------------------------------------------- project and ports

    /** Called when the Lookup tab is opened. */
    void onShown() {
        reloadProject();
        if (!detectedOnce) {
            detectedOnce = true;
            refreshPorts(true);
        }
    }

    /** Updates the project summary and sweep range from the open project. */
    void reloadProject() {
        List<LEDStrip> strips = host.getLedStrips();
        if (strips.isEmpty()) {
            projectSummary.setText("Needed for lookup by label and the sweep range");
            projectPill.set("No project open", Tone.WARNING);
            openProjectButton.setVisible(true);
        } else {
            int maxId = ProjectUnits.maxId(strips);
            projectSummary.setText(strips.size() + " boards, IDs 0 to " + maxId);
            projectPill.set("Loaded", Tone.SUCCESS);
            openProjectButton.setVisible(false);
            if (currentSweepId == null) {
                startSpinner.setValue(0);
                endSpinner.setValue(maxId);
            }
        }
    }

    /** @param detect find which port is the transmitter (may probe units) */
    private void refreshPorts(boolean detect) {
        Object current = transmitterBox.getSelectedItem();
        transmitterBox.removeAllItems();
        List<SerialPort> ports = UnitPort.emrickPorts();
        for (SerialPort sp : ports) {
            transmitterBox.addItem(sp.getSystemPortName());
        }
        if (current != null) {
            transmitterBox.setSelectedItem(current);
        }
        updateTransmitterPill();
        // With several units plugged in, select the transmitter: use what another tab already found, otherwise
        // probe the ports in the background
        if (detect && ports.size() > 1) {
            for (SerialPort sp : ports) {
                if (UnitPort.TRANSMITTER.equals(UnitPort.knownType(sp.getSystemPortName()))) {
                    transmitterBox.setSelectedItem(sp.getSystemPortName());
                    return;
                }
            }
            transmitterPill.set("Detecting...", Tone.NEUTRAL);
            sender.submit(() -> {
                for (SerialPort sp : ports) {
                    String name = sp.getSystemPortName();
                    if (UnitPort.knownType(name) == null && UnitPort.ownerOf(name) == null && UnitPort.claim(name, OWNER)) {
                        try {
                            if (UnitPort.TRANSMITTER.equals(UnitPort.probeType(sp))) {
                                SwingUtilities.invokeLater(() -> transmitterBox.setSelectedItem(name));
                                break;
                            }
                        } finally {
                            UnitPort.release(name, OWNER);
                        }
                    }
                }
                SwingUtilities.invokeLater(this::updateTransmitterPill);
            });
        }
    }

    private void updateTransmitterPill() {
        String port = (String) transmitterBox.getSelectedItem();
        if (port == null) {
            transmitterPill.set("Not connected", Tone.DANGER);
        } else if (UnitPort.TRANSMITTER.equals(UnitPort.knownType(port))) {
            transmitterPill.set("Transmitter", Tone.SUCCESS);
        } else if (UnitPort.RECEIVER.equals(UnitPort.knownType(port))) {
            transmitterPill.set("That's a receiver", Tone.DANGER);
        } else {
            transmitterPill.set("Selected", Tone.NEUTRAL);
        }
    }

    // ---------------------------------------------------------------- sending

    /** Sends an identify for this board ID (-1 = all off). Runs in order on a background thread. */
    private void sendIdentify(int id, Runnable onFailure) {
        String portName = (String) transmitterBox.getSelectedItem();
        if (portName == null) {
            JOptionPane.showMessageDialog(this, "Plug in the transmitter first.", "No Transmitter", JOptionPane.WARNING_MESSAGE);
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        sender.submit(() -> {
            String error = null;
            if (!UnitPort.claim(portName, OWNER)) {
                error = portName + " is being used by " + UnitPort.ownerOf(portName) + ". Disconnect it there first.";
            } else {
                try {
                    SerialPort sp = UnitPort.find(portName);
                    if (sp == null || !UnitPort.sendRaw(sp, "i" + id + "\n")) {
                        error = "Couldn't send to the transmitter on " + portName + ".";
                    }
                } finally {
                    UnitPort.release(portName, OWNER);
                }
            }
            if (error != null) {
                String message = error;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, message, "Lookup", JOptionPane.WARNING_MESSAGE);
                    if (onFailure != null) {
                        onFailure.run();
                    }
                });
            }
        });
    }

    private void showLit(Integer id) {
        if (id == null) {
            bigText(litId, null, "No unit lit", 26f);
            litLabel.setText(" ");
            litPill.set("Nothing lit", Tone.NEUTRAL);
            return;
        }
        LEDStrip strip = ProjectUnits.byId(host.getLedStrips(), id);
        bigText(litId, String.valueOf(id), null, 26f);
        litLabel.setText(strip != null ? strip.getLabel() : " ");
        litPill.set("Lit white", Tone.ACCENT);
    }

    // ---------------------------------------------------------------- single lookup

    private void lookUp() {
        String label = labelField.getText().trim();
        String idText = idField.getText().trim();
        List<LEDStrip> strips = host.getLedStrips();
        Integer id = null;

        if (!idText.isEmpty()) {
            try {
                id = Integer.parseInt(idText);
            } catch (NumberFormatException e) {
                lookupMessage.setText("Board ID must be a number.");
                return;
            }
        }
        if (!label.isEmpty()) {
            if (strips.isEmpty()) {
                lookupMessage.setText("Open a project to look up by label, or enter just the board ID.");
                return;
            }
            LEDStrip strip = ProjectUnits.byLabel(strips, label);
            if (strip == null) {
                lookupMessage.setText("No board labeled " + label.toUpperCase() + " in the open project.");
                return;
            }
            if (id != null && id != strip.getId()) {
                int choice = JOptionPane.showOptionDialog(this,
                        "The project says " + strip.getLabel() + " is board ID " + strip.getId() + ", not " + id + ".\nWhich one should light up?",
                        "Label and ID Don't Match", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        new Object[]{"ID " + strip.getId() + " (" + strip.getLabel() + ")", "ID " + id, "Cancel"}, null);
                if (choice == 0) {
                    id = strip.getId();
                } else if (choice != 1) {
                    return;
                }
            } else {
                id = strip.getId();
            }
        }
        if (id == null) {
            lookupMessage.setText("Enter a board label or board ID.");
            return;
        }
        stopSweep();
        sendIdentify(id, () -> {
            lookupMessage.setText("Lookup not sent.");
            showLit(null);
        });
        unitLit = true;
        lookupMessage.setText(" ");
        showLit(id);
    }

    private void allOff() {
        sendIdentify(-1, null);
        unitLit = false;
        showLit(null);
    }

    // ---------------------------------------------------------------- sweep

    private void startSweep() {
        int start = (Integer) startSpinner.getValue();
        int end = (Integer) endSpinner.getValue();
        if (end < start) {
            JOptionPane.showMessageDialog(this, "The end ID must be at least the start ID.", "ID Sweep", JOptionPane.WARNING_MESSAGE);
            return;
        }
        currentSweepId = start;
        showAndSend();
        updateSweepControls();
        updateAutoTimer();
        sweepCard.requestFocusInWindow();
    }

    /** @param direction -1 previous, 0 resend current, 1 next */
    private void step(int direction) {
        if (currentSweepId == null) {
            return;
        }
        int end = (Integer) endSpinner.getValue();
        int start = (Integer) startSpinner.getValue();
        int next = currentSweepId + direction;
        if (next > end) {
            autoTimer.stop();
            autoBox.setSelected(false);
            sweepExpected.setText("That was the last ID (" + end + "). Press Stop Sweep when you're done.");
            return;
        }
        currentSweepId = Math.max(start, next);
        showAndSend();
        if (autoTimer.isRunning()) {
            autoTimer.restart();
        }
        sweepCard.requestFocusInWindow();
    }

    private void showAndSend() {
        int start = (Integer) startSpinner.getValue();
        int end = (Integer) endSpinner.getValue();
        bigText(sweepId, String.valueOf(currentSweepId), null, 60f);
        LEDStrip strip = ProjectUnits.byId(host.getLedStrips(), currentSweepId);
        sweepExpected.setText(strip != null
                ? "Should be " + strip.getLabel() + "  \u00b7  " + ProjectUnits.ledCount(strip) + " LEDs"
                : "Exactly one unit should light up");
        sweepProgress.setMinimum(start);
        sweepProgress.setMaximum(Math.max(start + 1, end));
        sweepProgress.setValue(currentSweepId);
        sweepProgress.setString((currentSweepId - start + 1) + " of " + (end - start + 1));
        unitLit = true;
        showLit(currentSweepId);
        sendIdentify(currentSweepId, this::stopSweep);
    }

    private void stopSweep() {
        if (currentSweepId == null) {
            return;
        }
        currentSweepId = null;
        autoTimer.stop();
        bigText(sweepId, null, "Not running", 60f);
        sweepExpected.setText("Sweep stopped");
        sweepProgress.setValue(sweepProgress.getMinimum());
        sweepProgress.setString("");
        allOff();
        updateSweepControls();
    }

    private void updateAutoTimer() {
        autoTimer.setDelay((int) (((Double) autoSeconds.getValue()) * 1000));
        if (autoBox.isSelected() && currentSweepId != null) {
            autoTimer.restart();
        } else {
            autoTimer.stop();
        }
    }

    private void updateSweepControls() {
        boolean running = currentSweepId != null;
        startButton.setVisible(!running);
        stopButton.setVisible(running);
        startSpinner.setEnabled(!running);
        endSpinner.setEnabled(!running);
        prevButton.setEnabled(running);
        resendButton.setEnabled(running);
        nextButton.setEnabled(running);
    }

    void shutdown() {
        autoTimer.stop();
        if (unitLit) {
            currentSweepId = null;
            sendIdentify(-1, null);
            unitLit = false;
        }
    }
}
