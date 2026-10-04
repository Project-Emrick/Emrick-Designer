package org.emrick.project.dev;

import com.fazecast.jSerialComm.SerialPort;
import org.emrick.project.dev.DevUI.Card;
import org.emrick.project.dev.DevUI.Pill;
import org.emrick.project.dev.DevUI.Tone;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Flashes the latest Heltec Receiver firmware (published from the firmware repo's main branch) to one or more
 * plugged-in receivers at once. Only receivers are listed. Each unit's board ID, label, and show are kept; the
 * unit is read before and after so any change is caught. A unit that fails is blinked red (when it can still
 * respond) so it can be found among the others.
 */
class FlashPanel extends JPanel {
    private static final String OWNER = "Flash";

    /** Remembered for the rest of the session once the user confirms the version to flash. */
    private static String confirmedCommit = null;

    private enum Outcome { NONE, RUNNING, OK, FAILED }

    /** One plugged-in receiver. Only touched on the EDT. */
    private static class Row {
        final String port;
        boolean selected = true;
        UnitInfo before;
        String status = "Ready";
        int progress = -1;
        Outcome outcome = Outcome.NONE;

        Row(String port) {
            this.port = port;
        }

        boolean flashable() {
            return outcome != Outcome.RUNNING;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final FlashTableModel model = new FlashTableModel();
    private final JTable table = new JTable(model);
    private final JLabel releaseVersion = new JLabel("Checking...");
    private final JTextArea releaseDetail = DevUI.wrappingCaption(" ");
    private final Pill releasePill = new Pill("", Tone.NEUTRAL);
    private final JButton scanButton = DevUI.secondary("Scan for Receivers");
    private final JButton flashButton = DevUI.primary("Flash Selected");
    private final Pill countPill = new Pill("", Tone.NEUTRAL);
    private final CardLayout listCards = new CardLayout();
    private final JPanel listArea = DevUI.clear(listCards);
    private final JLabel emptyHeadline = new JLabel("", SwingConstants.CENTER);
    private final JTextArea logArea = new JTextArea(6, 80);
    private final ExecutorService scanner = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Flash port scan");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger activeFlashes = new AtomicInteger();
    private final AtomicInteger pendingScans = new AtomicInteger();
    private boolean scannedOnce = false;

    FlashPanel() {
        super(new BorderLayout(0, DevUI.GAP));
        setOpaque(false);

        // --- Firmware release ---
        Card release = new Card(new BorderLayout(DevUI.GAP, 0));
        JPanel releaseText = DevUI.clear(new BorderLayout(0, 4));
        JLabel releaseTitle = DevUI.section("Receiver firmware");
        releaseText.add(releaseTitle, BorderLayout.NORTH);
        JPanel versionRow = DevUI.clear(new DevUI.WrapLayout(FlowLayout.LEFT, 10, 0));
        releaseVersion.setFont(DevUI.heading());
        versionRow.add(releaseVersion);
        versionRow.add(releasePill);
        releaseText.add(versionRow, BorderLayout.CENTER);
        releaseText.add(releaseDetail, BorderLayout.SOUTH);
        release.add(releaseText, BorderLayout.CENTER);
        JButton checkButton = DevUI.secondary("Check for Update");
        checkButton.addActionListener(e -> checkForUpdate(true));
        JPanel checkHolder = DevUI.clear(new GridBagLayout());
        checkHolder.add(checkButton);
        release.add(checkHolder, BorderLayout.EAST);
        add(release, BorderLayout.NORTH);

        // --- Receivers ---
        Card list = new Card(new BorderLayout(0, 10));
        JPanel listHeader = DevUI.clear(new BorderLayout());
        JPanel listTitle = DevUI.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        listTitle.add(DevUI.title("Receivers"));
        listTitle.add(countPill);
        listHeader.add(listTitle, BorderLayout.WEST);
        JPanel actions = DevUI.clear(new DevUI.WrapLayout(FlowLayout.RIGHT, 8, 4));
        actions.add(DevUI.caption("Board ID, label, and show data are kept."));
        scanButton.addActionListener(e -> scan());
        actions.add(scanButton);
        flashButton.addActionListener(e -> flashSelected());
        actions.add(flashButton);
        listHeader.add(actions, BorderLayout.CENTER);
        list.add(listHeader, BorderLayout.NORTH);

        DevUI.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(44);
        table.getColumnModel().getColumn(6).setCellRenderer(new ProgressRenderer());
        table.getColumnModel().getColumn(6).setPreferredWidth(120);
        // Give the text columns enough room for their headings before Status takes the rest
        int[] minWidths = {0, 70, 75, 100, 50, 125};
        for (int i = 1; i < minWidths.length; i++) {
            table.getColumnModel().getColumn(i).setMinWidth(minWidths[i]);
        }
        table.getColumnModel().getColumn(7).setCellRenderer(new DevUI.PillRenderer(
                v -> ((Row) v).status, v -> outcomeTone((Row) v)));
        table.getColumnModel().getColumn(7).setPreferredWidth(220);
        listArea.add(DevUI.borderless(table), "table");
        JPanel empty = DevUI.clear(new GridBagLayout());
        JPanel emptyStack = DevUI.clear(new GridLayout(0, 1, 0, 4));
        emptyHeadline.setFont(DevUI.subheading());
        emptyStack.add(emptyHeadline);
        JLabel emptyDetail = DevUI.caption("Plug receivers in with USB cables, then press Scan for Receivers.");
        emptyDetail.setHorizontalAlignment(SwingConstants.CENTER);
        emptyStack.add(emptyDetail);
        empty.add(emptyStack);
        listArea.add(empty, "empty");
        list.add(listArea, BorderLayout.CENTER);

        // --- Log ---
        Card logCard = new Card(new BorderLayout(0, 8));
        JLabel logTitle = DevUI.section("Flash log");
        logCard.add(logTitle, BorderLayout.NORTH);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setOpaque(false);
        JScrollPane logScroll = DevUI.borderless(logArea);
        logCard.add(logScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, list, logCard);
        split.setResizeWeight(0.7);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false);
        split.setDividerSize(DevUI.GAP);
        add(split, BorderLayout.CENTER);

        showList();
        refreshReleaseCard();
    }

    void onShown() {
        if (!scannedOnce) {
            scannedOnce = true;
            scan();
        }
    }

    boolean isFlashing() {
        return activeFlashes.get() > 0;
    }

    void shutdown() {
        // Flashes in progress keep running in the background so a unit is never left half-written
    }

    // ---------------------------------------------------------------- release

    void refreshReleaseCard() {
        FirmwareRelease release = FirmwareRelease.latestKnown();
        if (release == null) {
            releaseVersion.setText("Not available");
            releasePill.set("Not downloaded", Tone.DANGER);
            releaseDetail.setText(FirmwareRelease.lastProblem() == null
                    ? "The latest version is downloaded automatically when you flash."
                    : FirmwareRelease.lastProblem());
        } else if (release.isFromCacheOnly()) {
            releaseVersion.setText(release.version());
            releasePill.set("Previously downloaded", Tone.WARNING);
            releaseDetail.setText("Couldn't check for a newer version: " + FirmwareRelease.lastProblem());
        } else {
            releaseVersion.setText(release.version());
            releasePill.set("Latest from main", Tone.SUCCESS);
            releaseDetail.setText("Published automatically from the firmware repo's main branch.");
        }
    }

    private void checkForUpdate(boolean showProblems) {
        releaseVersion.setText("Checking...");
        releasePill.set("", Tone.NEUTRAL);
        new Thread(() -> {
            String error = null;
            try {
                FirmwareRelease.fetchLatest();
            } catch (Exception e) {
                error = e.getMessage();
            }
            String finalError = error;
            SwingUtilities.invokeLater(() -> {
                refreshReleaseCard();
                if (finalError != null && showProblems) {
                    JOptionPane.showMessageDialog(this, finalError, "Receiver Firmware", JOptionPane.WARNING_MESSAGE);
                }
            });
        }, "Firmware check").start();
    }

    // ---------------------------------------------------------------- scanning

    private void scan() {
        if (isFlashing()) {
            return;
        }
        rows.clear();
        model.fireTableDataChanged();
        List<SerialPort> ports = UnitPort.emrickPorts();
        if (ports.isEmpty()) {
            showList();
            return;
        }
        scanButton.setEnabled(false);
        pendingScans.set(ports.size());
        showList();
        for (SerialPort port : ports) {
            String name = port.getSystemPortName();
            scanner.submit(() -> {
                String type;
                UnitInfo info = null;
                String owner = UnitPort.ownerOf(name);
                String heldBy = null;
                if (owner != null || !UnitPort.claim(name, OWNER)) {
                    // Flashing takes priority, so a receiver in use elsewhere (e.g. the terminal) is still listed
                    type = UnitPort.RECEIVER.equals(UnitPort.knownType(name)) ? UnitPort.RECEIVER : null;
                    heldBy = owner == null ? "another feature" : owner;
                    if (type == null) {
                        log(name + ": skipped, in use by " + heldBy);
                    }
                } else {
                    try {
                        SerialPort sp = UnitPort.find(name);
                        if (sp == null || !UnitPort.open(sp)) {
                            type = null;
                            log(name + ": skipped, port busy (close other programs using it)");
                        } else {
                            try {
                                type = UnitPort.probeType(sp);
                                if (UnitPort.RECEIVER.equals(type)) {
                                    info = UnitPort.queryInfo(sp, 5000);
                                } else {
                                    log(name + ": skipped, " + (UnitPort.TRANSMITTER.equals(type) ? "transmitter" : "not a receiver"));
                                }
                            } finally {
                                sp.closePort();
                            }
                        }
                    } finally {
                        UnitPort.release(name, OWNER);
                    }
                }
                String finalType = type;
                UnitInfo finalInfo = info;
                String finalHeldBy = heldBy;
                SwingUtilities.invokeLater(() -> {
                    if (UnitPort.RECEIVER.equals(finalType)) {
                        Row row = new Row(name);
                        row.before = finalInfo;
                        if (finalHeldBy != null) {
                            row.status = "In use by " + finalHeldBy + " (disconnected when flashing)";
                        }
                        rows.add(row);
                        rows.sort((a, b) -> a.port.compareTo(b.port));
                        model.fireTableDataChanged();
                    }
                    if (pendingScans.decrementAndGet() == 0) {
                        scanButton.setEnabled(true);
                    }
                    showList();
                });
            });
        }
    }

    private void showList() {
        boolean scanning = pendingScans.get() > 0;
        if (rows.isEmpty()) {
            emptyHeadline.setText(scanning ? "Looking for receivers..." : "No receivers found");
            listCards.show(listArea, "empty");
        } else {
            listCards.show(listArea, "table");
        }
        countPill.set(scanning ? "Scanning..." : rows.size() == 1 ? "1 receiver" : rows.size() + " receivers",
                rows.isEmpty() ? Tone.NEUTRAL : Tone.ACCENT);
        long selected = rows.stream().filter(r -> r.selected && r.flashable()).count();
        flashButton.setText(selected == 0 ? "Flash Selected" : "Flash " + selected + (selected == 1 ? " Receiver" : " Receivers"));
        flashButton.setEnabled(selected > 0 && !isFlashing());
    }

    // ---------------------------------------------------------------- flashing

    private void flashSelected() {
        List<Row> targets = new ArrayList<>();
        for (Row row : rows) {
            if (row.selected && row.flashable()) {
                targets.add(row);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        flashButton.setEnabled(false);
        scanButton.setEnabled(false);
        releaseVersion.setText("Getting firmware...");
        new Thread(() -> {
            FirmwareRelease release;
            try {
                release = FirmwareRelease.fetchLatest();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    refreshReleaseCard();
                    scanButton.setEnabled(true);
                    showList();
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Receiver Firmware", JOptionPane.WARNING_MESSAGE);
                });
                return;
            }
            SwingUtilities.invokeLater(() -> {
                refreshReleaseCard();
                if (confirmRelease(release)) {
                    startFlashing(targets, release);
                } else {
                    scanButton.setEnabled(true);
                    showList();
                }
            });
        }, "Firmware download").start();
    }

    /** Asks once per session (and again if a different version would be flashed). */
    private boolean confirmRelease(FirmwareRelease release) {
        if (release.commit().equals(confirmedCommit)) {
            return true;
        }
        String message = "Flash receiver firmware " + release.version() + "?";
        if (release.isFromCacheOnly()) {
            message += "\n\nThis is the newest version downloaded on this computer. A newer one may exist:\n"
                    + FirmwareRelease.lastProblem();
        }
        message += "\n\nYou won't be asked again for this version until Emrick Designer is restarted.";
        int choice = JOptionPane.showConfirmDialog(this, message, "Confirm Firmware Version",
                JOptionPane.OK_CANCEL_OPTION, release.isFromCacheOnly() ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            confirmedCommit = release.commit();
            return true;
        }
        return false;
    }

    private void startFlashing(List<Row> targets, FirmwareRelease release) {
        ExecutorService pool = Executors.newFixedThreadPool(targets.size(), r -> {
            Thread t = new Thread(r, "Flasher");
            t.setDaemon(false); // never cut a flash off midway
            return t;
        });
        AtomicInteger remaining = new AtomicInteger(targets.size());
        for (Row row : targets) {
            row.outcome = Outcome.RUNNING;
            row.progress = 0;
            row.status = "Waiting...";
            activeFlashes.incrementAndGet();
            pool.submit(() -> {
                try {
                    flashOne(row, release);
                } finally {
                    activeFlashes.decrementAndGet();
                    if (remaining.decrementAndGet() == 0) {
                        pool.shutdown();
                        SwingUtilities.invokeLater(this::onAllFinished);
                    }
                }
            });
        }
        refreshRows();
        showList();
    }

    private void flashOne(Row row, FirmwareRelease release) {
        String port = row.port;
        String holder = UnitPort.ownerOf(port);
        if (holder != null && !holder.equals(OWNER)) {
            log(port + ": taking over from " + holder);
        }
        if (!UnitPort.takeOver(port, OWNER, 10000)) {
            finish(row, false, "Couldn't get the port from " + UnitPort.ownerOf(port), false);
            return;
        }
        try {
            if (row.before == null) {
                status(row, "Reading unit...", 0);
                SerialPort before = UnitPort.find(port);
                row.before = before == null ? null : UnitPort.queryInfo(before, 4000);
            }
            status(row, "Preparing flashing tool...", 0);
            boolean ok;
            try {
                ok = Esptool.flash(port, release,
                        pct -> status(row, "Flashing " + pct + "%", pct),
                        line -> log(port + ": " + line));
            } catch (Exception e) {
                log(port + ": " + e.getMessage());
                finish(row, false, "Failed: " + e.getMessage(), true);
                return;
            }
            if (!ok) {
                finish(row, false, "Failed: check the cable and try again", true);
                return;
            }

            // Read the unit back to confirm the new version is running and its identity was kept
            status(row, "Checking unit...", 100);
            UnitPort.sleep(2500);
            SerialPort sp = UnitPort.find(port);
            UnitInfo after = sp == null ? null : UnitPort.queryInfo(sp, 8000);
            if (after == null) {
                finish(row, false, "Flashed, but the unit didn't respond afterwards", true);
            } else if (!release.commit().equals(after.fwCommit())) {
                finish(row, false, "Flashed, but the unit reports " + after.fw + " instead of " + release.commit(), true);
            } else if (row.before != null && (row.before.id != after.id || row.before.leds != after.leds
                    || !String.valueOf(row.before.label).equals(String.valueOf(after.label)))) {
                finish(row, false, "Updated, but its stored settings changed (was ID " + row.before.id + " "
                        + row.before.label + "). Re-enter them with Hardware > Modify Board", true);
            } else {
                row.before = after;
                finish(row, true, "Updated to " + after.fw, false);
            }
        } finally {
            UnitPort.release(port, OWNER);
        }
    }

    /** @param blink blink the unit's status LED red so it can be found among the others */
    private void finish(Row row, boolean ok, String message, boolean blink) {
        boolean blinked = false;
        if (!ok && blink) {
            SerialPort sp = UnitPort.find(row.port);
            blinked = sp != null && UnitPort.blink(sp, true);
        }
        String text = message + (!ok && blink ? (blinked ? " (status LED blinking red)" : " (unit not responding)") : "");
        log(row.port + ": " + text);
        SwingUtilities.invokeLater(() -> {
            row.outcome = ok ? Outcome.OK : Outcome.FAILED;
            row.status = text;
            row.selected = !ok; // failed units stay selected for a retry
            refreshRows();
            showList();
        });
    }

    private void status(Row row, String text, int progress) {
        SwingUtilities.invokeLater(() -> {
            row.status = text;
            row.progress = progress;
            refreshRows();
        });
    }

    private void onAllFinished() {
        scanButton.setEnabled(true);
        showList();
        long failed = rows.stream().filter(r -> r.outcome == Outcome.FAILED).count();
        long ok = rows.stream().filter(r -> r.outcome == Outcome.OK).count();
        if (failed > 0) {
            JOptionPane.showMessageDialog(this,
                    failed + " receiver(s) failed (shown in red). Failed units that still respond are blinking their status LED red.\n"
                            + ok + " receiver(s) updated successfully.",
                    "Flash Finished With Errors", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, ok + " receiver(s) updated successfully.", "Flash Finished",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void log(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /** Repaints rows in place; unlike fireTableDataChanged this keeps the selection. */
    private void refreshRows() {
        if (!rows.isEmpty()) {
            model.fireTableRowsUpdated(0, rows.size() - 1);
        }
    }

    private static Tone outcomeTone(Row row) {
        return switch (row.outcome) {
            case OK -> Tone.SUCCESS;
            case FAILED -> Tone.DANGER;
            case RUNNING -> Tone.INFO;
            default -> {
                FirmwareRelease latest = FirmwareRelease.latestKnown();
                boolean current = latest != null && row.before != null && latest.commit().equals(row.before.fwCommit());
                yield current ? Tone.SUCCESS : Tone.NEUTRAL;
            }
        };
    }

    // ---------------------------------------------------------------- table

    private class FlashTableModel extends AbstractTableModel {
        private final String[] columns = {"", "Port", "Board ID", "Board Label", "LEDs", "Current Firmware", "Progress", "Status"};

        @Override
        public int getRowCount() {
            return rows.size();
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
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 && rows.get(rowIndex).flashable() && !isFlashing();
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            rows.get(rowIndex).selected = (Boolean) value;
            showList();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Row row = rows.get(rowIndex);
            UnitInfo info = row.before;
            return switch (columnIndex) {
                case 0 -> row.selected;
                case 1 -> row.port;
                case 2 -> info == null ? "-" : String.valueOf(info.id);
                case 3 -> info == null || info.label == null || info.label.isEmpty() ? "-" : info.label;
                case 4 -> info == null ? "-" : String.valueOf(info.leds);
                case 5 -> info == null ? "Unknown" : info.fw;
                case 6 -> row.progress;
                default -> row;
            };
        }
    }

    /** A progress bar while a unit is being flashed; an empty cell otherwise. */
    private static class ProgressRenderer extends JProgressBar implements javax.swing.table.TableCellRenderer {
        private final DefaultTableCellRenderer blank = new DefaultTableCellRenderer();
        private final JPanel holder = new JPanel(new BorderLayout());

        ProgressRenderer() {
            super(0, 100);
            setStringPainted(true);
            holder.add(this, BorderLayout.CENTER);
            holder.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            int pct = value instanceof Integer ? (Integer) value : -1;
            if (pct < 0) {
                return blank.getTableCellRendererComponent(table, "", isSelected, false, row, column);
            }
            holder.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setValue(pct);
            setString(pct + "%");
            return holder;
        }
    }
}
