package org.emrick.project.dev;

import com.fazecast.jSerialComm.SerialPort;
import org.emrick.project.dev.DevUI.Card;
import org.emrick.project.dev.DevUI.Pill;
import org.emrick.project.dev.DevUI.Tone;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows every plugged-in unit and its details (board ID, label, firmware, battery, ...). New units are read
 * automatically when plugged in; Refresh reads them all again.
 */
class UnitsPanel extends JPanel {
    private static final String OWNER = "Units";
    private static final long LIVE_REFRESH_MS = 5000;

    /** One plugged-in unit. Only touched on the EDT. */
    private static class Row {
        final String port;
        String type = "";
        UnitInfo info;
        String status = "Reading...";
        boolean blinking;
        boolean reading;
        boolean busy;     // last read couldn't get the port; retried automatically
        long lastRead;

        Row(String port) {
            this.port = port;
        }
    }

    private final DevModeHost host;
    private final Map<String, Row> rows = new LinkedHashMap<>();
    private final List<Row> rowList = new ArrayList<>();
    private final UnitTableModel model = new UnitTableModel();
    private final JTable table = new JTable(model);
    private final Pill countPill = new Pill("", Tone.NEUTRAL);
    private final JButton blinkButton = DevUI.secondary("Blink Status LED");
    private final CardLayout listCards = new CardLayout();
    private final JPanel listArea = DevUI.clear(listCards);
    private final JPanel details = DevUI.clear(new BorderLayout(0, DevUI.GAP));
    private final ExecutorService serialWorker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Developer Mode unit reader");
        t.setDaemon(true);
        return t;
    });
    private final Timer plugWatcher = new Timer(1500, e -> checkForPlugChanges());

    UnitsPanel(DevModeHost host) {
        super(new BorderLayout(0, DevUI.GAP));
        this.host = host;
        setOpaque(false);

        // --- Unit list ---
        Card listCard = new Card(new BorderLayout(0, 10));
        JPanel listHeader = DevUI.clear(new BorderLayout());
        JPanel listTitle = DevUI.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        listTitle.add(DevUI.title("Connected Units"));
        listTitle.add(countPill);
        listHeader.add(listTitle, BorderLayout.WEST);
        JPanel listActions = DevUI.clear(new DevUI.WrapLayout(FlowLayout.RIGHT, 8, 4));
        listActions.add(DevUI.caption("Updates automatically while this tab is open."));
        blinkButton.setToolTipText("Blink the selected receiver's status LED red so you can tell which unit it is");
        blinkButton.addActionListener(e -> toggleBlink());
        listActions.add(blinkButton);
        JButton refresh = DevUI.secondary("Refresh");
        refresh.setToolTipText("Read every plugged-in unit again");
        refresh.addActionListener(e -> rescanAll());
        listActions.add(refresh);
        listHeader.add(listActions, BorderLayout.CENTER);
        listCard.add(listHeader, BorderLayout.NORTH);

        DevUI.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> refreshDetails());
        table.getColumnModel().getColumn(7).setCellRenderer(new DevUI.PillRenderer(
                v -> ((Row) v).blinking ? "Blinking" : ((Row) v).status, v -> statusTone((Row) v)));
        table.getColumnModel().getColumn(7).setPreferredWidth(260);
        listArea.add(DevUI.borderless(table), "table");
        listArea.add(DevUI.emptyState("No units connected", "Plug a receiver or transmitter in with a USB cable."), "empty");
        listArea.setPreferredSize(new Dimension(800, 210));
        listCard.add(listArea, BorderLayout.CENTER);
        add(listCard, BorderLayout.NORTH);

        // --- Details of the selected unit ---
        JScrollPane detailScroll = DevUI.reflowScroll(details);
        add(detailScroll, BorderLayout.CENTER);
        rebuildRows();
    }

    void start() {
        plugWatcher.start();
        checkForPlugChanges();
    }

    void shutdown() {
        plugWatcher.stop();
        for (Row row : rowList) {
            if (row.blinking) {
                row.blinking = false;
                String port = row.port;
                serialWorker.submit(() -> withPort(port, sp -> UnitPort.blink(sp, false)));
            }
        }
    }

    /** Re-renders the table and details (e.g. after the latest firmware version becomes known). */
    void refreshDisplay() {
        refreshRows();
        refreshDetails();
    }

    // ---------------------------------------------------------------- reading units

    private void checkForPlugChanges() {
        Set<String> present = new HashSet<>();
        for (SerialPort sp : UnitPort.emrickPorts()) {
            present.add(sp.getSystemPortName());
        }
        boolean changed = rows.keySet().removeIf(port -> !present.contains(port));
        for (String port : present) {
            if (!rows.containsKey(port)) {
                rows.put(port, new Row(port));
                changed = true;
                read(port);
            }
        }
        if (changed) {
            rebuildRows();
        }
        // Retry ports that were busy (e.g. another tab or the hardware scan was using them), and keep receivers'
        // details live while this tab is on screen. Only the quick "info" command is used, which never resets a unit.
        long now = System.currentTimeMillis();
        for (Row row : rows.values()) {
            if (row.reading) {
                continue;
            }
            if (row.busy && now - row.lastRead > 3000) {
                read(row.port);
            } else if (isShowing() && row.info != null && now - row.lastRead > LIVE_REFRESH_MS) {
                refreshInfo(row.port);
            }
        }
    }

    /** Re-reads a receiver's details without probing it again. Keeps the last details if it doesn't answer. */
    private void refreshInfo(String port) {
        Row row = rows.get(port);
        if (row == null || row.reading) {
            return;
        }
        row.reading = true;
        serialWorker.submit(() -> {
            UnitInfo[] info = {null};
            String owner = UnitPort.ownerOf(port);
            if (owner == null || owner.equals(OWNER)) {
                withPort(port, sp -> info[0] = UnitPort.queryInfo(sp, 2500));
            }
            SwingUtilities.invokeLater(() -> {
                Row r = rows.get(port);
                if (r == null) {
                    return;
                }
                r.reading = false;
                r.lastRead = System.currentTimeMillis();
                if (info[0] != null) {
                    r.info = info[0];
                    refreshRows();
                    if (selectedRow() == r) {
                        refreshDetails();
                    }
                }
            });
        });
    }

    private void rescanAll() {
        for (String port : rows.keySet()) {
            read(port);
        }
        checkForPlugChanges();
    }

    private void read(String port) {
        Row row = rows.get(port);
        if (row == null || row.reading) {
            return;
        }
        row.reading = true;
        row.status = "Reading...";
        refreshRows();
        serialWorker.submit(() -> {
            String owner = UnitPort.ownerOf(port);
            if (owner != null && !owner.equals(OWNER)) {
                SwingUtilities.invokeLater(() -> update(port, null, null, "In use by " + owner, true));
                return;
            }
            String[] type = {UnitPort.UNKNOWN};
            UnitInfo[] info = {null};
            boolean ok = withPort(port, sp -> {
                type[0] = UnitPort.probeType(sp);
                if (UnitPort.RECEIVER.equals(type[0])) {
                    info[0] = UnitPort.queryInfo(sp, 5000);
                }
            });
            String status;
            if (!ok) {
                status = "Port busy";
            } else if (UnitPort.RECEIVER.equals(type[0])) {
                status = info[0] != null ? "Ready" : "Older firmware";
            } else if (UnitPort.TRANSMITTER.equals(type[0])) {
                status = "Transmitter";
            } else {
                status = "Not responding";
            }
            String finalStatus = status;
            SwingUtilities.invokeLater(() -> update(port, type[0], info[0], finalStatus, !ok));
        });
    }

    private void update(String port, String type, UnitInfo info, String status, boolean busy) {
        Row row = rows.get(port);
        if (row == null) {
            return;
        }
        row.reading = false;
        row.busy = busy;
        row.lastRead = System.currentTimeMillis();
        if (type != null) {
            row.type = type;
        }
        if (!busy) {
            row.info = info;
        }
        row.status = status;
        refreshRows();
        refreshDetails();
    }

    private void toggleBlink() {
        Row row = selectedRow();
        if (row == null || !UnitPort.RECEIVER.equals(row.type)) {
            return;
        }
        boolean on = !row.blinking;
        String port = row.port;
        serialWorker.submit(() -> {
            boolean[] ok = {false};
            withPort(port, sp -> ok[0] = UnitPort.blink(sp, on));
            SwingUtilities.invokeLater(() -> {
                Row r = rows.get(port);
                if (r != null && ok[0]) {
                    r.blinking = on;
                } else if (r != null) {
                    JOptionPane.showMessageDialog(this, "The unit on " + port + " didn't respond. It may need a firmware update.",
                            "Blink Status LED", JOptionPane.WARNING_MESSAGE);
                }
                refreshRows();
                refreshDetails();
            });
        });
    }

    private interface PortAction {
        void run(SerialPort sp);
    }

    /** Claims, opens, runs, and releases a port. @return false if the port is used elsewhere or can't open */
    private boolean withPort(String port, PortAction action) {
        if (!UnitPort.claim(port, OWNER)) {
            return false;
        }
        SerialPort sp = UnitPort.find(port);
        try {
            if (sp == null || !UnitPort.open(sp)) {
                return false;
            }
            action.run(sp);
            return true;
        } finally {
            if (sp != null) {
                sp.closePort();
            }
            UnitPort.release(port, OWNER);
        }
    }

    // ---------------------------------------------------------------- display

    private static Tone statusTone(Row row) {
        if (row.blinking) {
            return Tone.ACCENT;
        }
        return switch (row.status) {
            case "Ready" -> {
                String verdict = firmwareVerdict(row.info);
                yield verdict.startsWith("Outdated") ? Tone.WARNING : Tone.SUCCESS;
            }
            case "Transmitter" -> Tone.INFO;
            case "Reading..." -> Tone.NEUTRAL;
            case "Older firmware" -> Tone.WARNING;
            default -> Tone.DANGER;
        };
    }

    private void rebuildRows() {
        String selected = selectedRow() == null ? null : selectedRow().port;
        rowList.clear();
        rowList.addAll(rows.values());
        model.fireTableDataChanged();
        for (int i = 0; i < rowList.size(); i++) {
            if (rowList.get(i).port.equals(selected)) {
                table.setRowSelectionInterval(i, i);
            }
        }
        if (table.getSelectedRow() < 0 && !rowList.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
        listCards.show(listArea, rowList.isEmpty() ? "empty" : "table");
        countPill.set(rowList.size() == 1 ? "1 unit" : rowList.size() + " units", rowList.isEmpty() ? Tone.NEUTRAL : Tone.ACCENT);
        refreshDetails();
    }

    /** Repaints rows in place; unlike fireTableDataChanged this keeps the selection. */
    private void refreshRows() {
        if (!rowList.isEmpty()) {
            model.fireTableRowsUpdated(0, rowList.size() - 1);
        }
    }

    private Row selectedRow() {
        int i = table.getSelectedRow();
        return i >= 0 && i < rowList.size() ? rowList.get(i) : null;
    }

    private static String firmwareVerdict(UnitInfo info) {
        FirmwareRelease latest = FirmwareRelease.latestKnown();
        if (info == null) {
            return "";
        }
        if (info.fw != null && info.fw.endsWith("-dirty")) {
            return "Local development build";
        }
        if (latest == null) {
            return "Latest version unknown";
        }
        return latest.commit().equals(info.fwCommit()) ? "Up to date" : "Outdated (latest " + latest.commit() + ")";
    }

    private void refreshDetails() {
        Row row = selectedRow();
        blinkButton.setEnabled(row != null && UnitPort.RECEIVER.equals(row.type) && row.info != null);
        blinkButton.setText(row != null && row.blinking ? "Stop Blinking" : "Blink Status LED");

        details.removeAll();
        if (row == null) {
            details.revalidate();
            details.repaint();
            return;
        }

        // Header: port, type, and status
        JPanel head = DevUI.clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel name = DevUI.title(row.port + "  \u00b7  " + (row.type.isEmpty() ? "Unit" : row.type));
        head.add(name);
        head.add(new Pill(row.blinking ? "Blinking" : row.status, statusTone(row)));
        details.add(head, BorderLayout.NORTH);

        UnitInfo info = row.info;
        JPanel grid = DevUI.clear(new DevUI.ResponsiveGrid(300, 3, DevUI.GAP, false));
        if (info == null) {
            String message = switch (row.status) {
                case "Transmitter" -> "Transmitters don't report details. Use the Lookup tab to find receivers through it.";
                case "Older firmware" -> "This receiver's firmware is too old to report its details. Update it on the Flash Firmware tab.";
                case "Reading..." -> "Reading the unit...";
                default -> "The unit didn't respond. Check the cable, close other programs using " + row.port + ", and press Refresh.";
            };
            Card card = new Card(new BorderLayout());
            card.add(DevUI.caption(message), BorderLayout.CENTER);
            details.add(card, BorderLayout.CENTER);
            details.revalidate();
            details.repaint();
            return;
        }

        Card identity = section("Identity");
        bigValue(identity, "Board ID", String.valueOf(info.id), "Board Label",
                info.label == null || info.label.isEmpty() ? "Not set" : info.label);
        value(identity, "Position", info.position);
        value(identity, "LED count", String.valueOf(info.leds));
        grid.add(identity);

        Card firmware = section("Firmware");
        String verdict = firmwareVerdict(info);
        value(firmware, "Version", info.fw);
        value(firmware, "Built", DevUI.date(info.fwDate));
        pillValue(firmware, "Status", verdict, verdict.startsWith("Outdated") ? Tone.WARNING
                : verdict.equals("Up to date") ? Tone.SUCCESS : Tone.NEUTRAL);
        grid.add(firmware);

        Card show = section("Show Data");
        value(show, "Show token", String.valueOf(info.token));
        value(show, "Packets stored", String.valueOf(info.packets));
        colorValue(show, "Verification color", info.verificationColor);
        grid.add(show);

        Card power = section("Power & State");
        batteryValue(power, info.batteryVolts, info.batteryPercent);
        value(power, "State", capitalize(info.state));
        value(power, "Debug output", info.debug ? "On" : "Off");
        grid.add(power);

        Card hardware = section("Hardware");
        value(hardware, "Port", row.port);
        value(hardware, "MAC address", info.mac);
        value(hardware, "Type", row.type);
        grid.add(hardware);

        Card project = section("Project Check");
        String check = ProjectUnits.check(host.getLedStrips(), info);
        if (check.isEmpty()) {
            pillValue(project, "Result", "No project open", Tone.NEUTRAL);
            project.add(wrapCaption("Open a project to compare this unit's ID, label, and LED count."));
            JButton open = DevUI.secondary("Open Project...");
            open.addActionListener(e -> host.openProjectForDevMode());
            JPanel openRow = DevUI.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
            openRow.setAlignmentX(LEFT_ALIGNMENT);
            openRow.setBorder(new EmptyBorder(8, 0, 0, 0));
            openRow.add(open);
            openRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, openRow.getPreferredSize().height));
            project.add(openRow);
        } else {
            boolean ok = check.startsWith("Matches");
            pillValue(project, "Result", ok ? "Matches" : "Mismatch", ok ? Tone.SUCCESS : Tone.DANGER);
            project.add(wrapCaption(ok ? check : check.substring("Mismatch: ".length())));
        }
        grid.add(project);

        JPanel gridHolder = DevUI.clear(new BorderLayout());
        gridHolder.add(grid, BorderLayout.NORTH);
        details.add(gridHolder, BorderLayout.CENTER);
        details.revalidate();
        details.repaint();
    }

    // ---------------------------------------------------------------- detail card helpers

    private static Card section(String title) {
        Card card = new Card(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel t = DevUI.section(title);
        t.setAlignmentX(LEFT_ALIGNMENT);
        t.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(t);
        return card;
    }

    private static JPanel line(String key, JComponent value) {
        JPanel p = DevUI.clear(new BorderLayout(10, 0));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(3, 0, 3, 0));
        p.add(DevUI.caption(key), BorderLayout.WEST);
        JPanel right = DevUI.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(value);
        p.add(right, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private static void value(Card card, String key, String value) {
        JLabel v = new JLabel(value == null || value.isEmpty() ? "-" : value);
        card.add(line(key, v));
    }

    private static void pillValue(Card card, String key, String text, Tone tone) {
        card.add(line(key, new Pill(text, tone)));
    }

    /** The two most important values, shown large side by side. */
    private static void bigValue(Card card, String k1, String v1, String k2, String v2) {
        JPanel row = DevUI.clear(new GridLayout(1, 2, 10, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(0, 0, 6, 0));
        for (String[] kv : new String[][]{{k1, v1}, {k2, v2}}) {
            JPanel cell = DevUI.clear(new BorderLayout());
            cell.add(DevUI.caption(kv[0]), BorderLayout.NORTH);
            JLabel big = new JLabel(kv[1]);
            big.setFont(DevUI.heading());
            cell.add(big, BorderLayout.CENTER);
            row.add(cell);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        card.add(row);
    }

    private static void colorValue(Card card, String key, String rgb) {
        JLabel v = new JLabel(rgb == null ? "-" : rgb);
        try {
            String[] parts = rgb.split(",");
            Color color = new Color(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
            v.setIcon(new SwatchIcon(color));
            v.setIconTextGap(8);
        } catch (RuntimeException ignored) {
            // Not a color; show the text only
        }
        card.add(line(key, v));
    }

    private static void batteryValue(Card card, double volts, double percent) {
        JPanel p = DevUI.clear(new BorderLayout(8, 0));
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) Math.round(percent));
        bar.setPreferredSize(new Dimension(90, 10));
        bar.setForeground(DevUI.tone(percent < 20 ? Tone.DANGER : percent < 50 ? Tone.WARNING : Tone.SUCCESS));
        JPanel barHolder = DevUI.clear(new GridBagLayout());
        barHolder.add(bar);
        p.add(barHolder, BorderLayout.WEST);
        JLabel v = new JLabel(String.format("%.0f%%  (%.2f V)", percent, volts));
        p.add(v, BorderLayout.CENTER);
        card.add(line("Battery", p));
    }

    private static JComponent wrapCaption(String text) {
        JTextArea l = DevUI.wrappingCaption(text);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(6, 0, 0, 0));
        return l;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "-";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).replace('_', ' ');
    }

    /** Small rounded color square. */
    private record SwatchIcon(Color color) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, 14, 14, 5, 5);
            g2.setColor(DevUI.cardBorder());
            g2.drawRoundRect(x, y, 14, 14, 5, 5);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 15;
        }

        @Override
        public int getIconHeight() {
            return 15;
        }
    }

    private class UnitTableModel extends AbstractTableModel {
        private final String[] columns = {"Port", "Type", "Board ID", "Board Label", "LEDs", "Firmware", "Battery", "Status"};

        @Override
        public int getRowCount() {
            return rowList.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Row row = rowList.get(rowIndex);
            UnitInfo info = row.info;
            return switch (columnIndex) {
                case 0 -> row.port;
                case 1 -> row.type;
                case 2 -> info == null ? "" : String.valueOf(info.id);
                case 3 -> info == null ? "" : info.label;
                case 4 -> info == null ? "" : String.valueOf(info.leds);
                case 5 -> info == null ? "" : info.fw;
                case 6 -> info == null ? "" : String.format("%.0f%%", info.batteryPercent);
                default -> row;
            };
        }
    }
}
