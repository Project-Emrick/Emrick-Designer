package org.emrick.project;

import org.emrick.project.dev.UnitInfo;
import org.emrick.project.dev.UnitPort;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Hardware > Modify Board: writes a receiver's board label, board ID, and LED count.
 *
 * With a show open, the fields fill each other in from the show: entering a board label fills in its board ID
 * and LED count, and entering a board ID fills in its label. Without a show, values are written as entered.
 * The unit is read back afterwards so the result can be confirmed.
 */
public class ModifyBoardDialog {
    private static final int MAX_LEDS = 60; // size of the receiver firmware's LED buffer

    private final Frame owner;
    private final SerialTransmitter st;
    private final List<LEDStrip> strips;

    private final JTextField labelField = new JTextField(12);
    private final JTextField idField = new JTextField(12);
    private final JTextField ledField = new JTextField(12);
    private final JCheckBox writeLabel = new JCheckBox("Write", true);
    private final JCheckBox writeId = new JCheckBox("Write", true);
    private final JCheckBox writeLeds = new JCheckBox("Write", true);
    private final JLabel matchLabel = new JLabel(" ");
    private final JLabel currentLabel = new JLabel("Reading the board...");
    private boolean filling = false; // true while a field is being filled in from the show

    private ModifyBoardDialog(Frame owner, SerialTransmitter st, List<LEDStrip> strips) {
        this.owner = owner;
        this.st = st;
        this.strips = strips;
    }

    /**
     * @param strips the open show's LED strips, or an empty list if no show is open
     */
    public static void show(Frame owner, SerialTransmitter st, List<LEDStrip> strips) {
        new ModifyBoardDialog(owner, st, strips).run();
    }

    private void run() {
        readCurrentValues();
        if (!strips.isEmpty()) {
            labelField.getDocument().addDocumentListener(onChange(this::fillFromLabel));
            idField.getDocument().addDocumentListener(onChange(this::fillFromId));
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        addRow(form, c, 0, "Board Label:", labelField, writeLabel, "e.g. T10L");
        addRow(form, c, 1, "Board ID:", idField, writeId, "e.g. 42");
        addRow(form, c, 2, "LED Count:", ledField, writeLeds, "1 to " + MAX_LEDS);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        form.add(matchLabel, c);
        c.gridy = 4;
        currentLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        form.add(currentLabel, c);
        c.gridy = 5;
        JLabel hint = new JLabel(strips.isEmpty()
                ? "No show is open, so values are written exactly as entered."
                : "A show is open: entering a label fills in its ID and LED count, and entering an ID fills in its label.");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        form.add(hint, c);

        while (true) {
            int option = JOptionPane.showConfirmDialog(owner, form, "Modify Board", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return;
            }
            String problem = validate();
            if (problem == null) {
                break;
            }
            JOptionPane.showMessageDialog(owner, problem, "Modify Board", JOptionPane.WARNING_MESSAGE);
        }
        write();
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String name, JTextField field, JCheckBox write,
                        String placeholder) {
        field.putClientProperty("JTextField.placeholderText", placeholder);
        c.gridy = row;
        c.gridwidth = 1;
        c.gridx = 0;
        form.add(new JLabel(name), c);
        c.gridx = 1;
        form.add(field, c);
        c.gridx = 2;
        write.setToolTipText("Uncheck to leave this value on the board unchanged");
        form.add(write, c);
    }

    // ---------------------------------------------------------------- show lookups

    private static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    private LEDStrip stripByLabel(String label) {
        for (LEDStrip strip : strips) {
            if (strip.getLabel().equalsIgnoreCase(label.trim())) {
                return strip;
            }
        }
        return null;
    }

    private LEDStrip stripById(int id) {
        for (LEDStrip strip : strips) {
            if (strip.getId() == id) {
                return strip;
            }
        }
        return null;
    }

    private void fillFromLabel() {
        if (filling) {
            return;
        }
        String label = labelField.getText().trim();
        LEDStrip strip = label.isEmpty() ? null : stripByLabel(label);
        if (strip != null) {
            fill(() -> {
                idField.setText(String.valueOf(strip.getId()));
                ledField.setText(String.valueOf(strip.getLedConfig().getLEDCount()));
            });
        }
        showMatch(strip, label.isEmpty() ? null : "No board labeled " + label.toUpperCase() + " in the open show.");
    }

    private void fillFromId() {
        if (filling) {
            return;
        }
        String text = idField.getText().trim();
        LEDStrip strip = null;
        try {
            strip = text.isEmpty() ? null : stripById(Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
        }
        LEDStrip found = strip;
        if (found != null) {
            fill(() -> {
                labelField.setText(found.getLabel());
                ledField.setText(String.valueOf(found.getLedConfig().getLEDCount()));
            });
        }
        showMatch(found, text.isEmpty() ? null : "Board ID " + text + " isn't in the open show.");
    }

    /** Fills fields without triggering the other field's lookup. Deferred because a document can't be changed from its own listener. */
    private void fill(Runnable change) {
        SwingUtilities.invokeLater(() -> {
            filling = true;
            try {
                change.run();
            } finally {
                filling = false;
            }
        });
    }

    private void showMatch(LEDStrip strip, String notFound) {
        if (strip != null) {
            matchLabel.setText("From the show: " + strip.getLabel() + " is board ID " + strip.getId()
                    + " with " + strip.getLedConfig().getLEDCount() + " LEDs.");
        } else {
            matchLabel.setText(notFound == null ? " " : notFound);
        }
    }

    // ---------------------------------------------------------------- reading and writing

    /** Shows what's on the board now (receivers on current firmware only). */
    private void readCurrentValues() {
        new Thread(() -> {
            UnitInfo info = st.getSerialPort() == null ? null : UnitPort.queryInfo(st.getSerialPort(), 3000);
            SwingUtilities.invokeLater(() -> currentLabel.setText(info == null
                    ? "Couldn't read this board's current values (it may need a firmware update)."
                    : "On this board now: " + describe(info)));
        }, "Modify Board read").start();
    }

    private static String describe(UnitInfo info) {
        return "label " + (info.label == null || info.label.isEmpty() ? "(not set)" : info.label)
                + ", board ID " + info.id + ", " + info.leds + " LEDs";
    }

    /** @return a message describing what's wrong, or null if the input is OK */
    private String validate() {
        if (!writeLabel.isSelected() && !writeId.isSelected() && !writeLeds.isSelected()) {
            return "Check at least one value to write.";
        }
        if (writeLabel.isSelected()) {
            String label = labelField.getText().trim();
            if (label.isEmpty() || label.length() > 16 || !label.matches("[A-Za-z0-9_-]+")) {
                return "Board label must be 1 to 16 letters or numbers, e.g. T10L.";
            }
        }
        if (writeId.isSelected()) {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                if (id < 0 || id > 65534) {
                    return "Board ID must be between 0 and 65534.";
                }
            } catch (NumberFormatException e) {
                return "Board ID must be a number.";
            }
        }
        if (writeLeds.isSelected()) {
            try {
                int leds = Integer.parseInt(ledField.getText().trim());
                if (leds < 1 || leds > MAX_LEDS) {
                    return "LED count must be between 1 and " + MAX_LEDS + ".";
                }
            } catch (NumberFormatException e) {
                return "LED count must be a number.";
            }
        }
        return null;
    }

    /** Left/right position: from the show if the board is in it, otherwise the label's last letter (T10L = L). */
    private String position(String label, int id) {
        LEDStrip strip = stripById(id);
        if (strip != null && strip.getLedConfig() != null && strip.getLedConfig().getLabel() != null) {
            return strip.getLedConfig().getLabel();
        }
        String upper = label.trim().toUpperCase();
        if (upper.endsWith("L") || upper.endsWith("R")) {
            return upper.substring(upper.length() - 1);
        }
        return "";
    }

    private void write() {
        String label = labelField.getText().trim().toUpperCase();
        String id = idField.getText().trim();
        String leds = ledField.getText().trim();
        boolean doLabel = writeLabel.isSelected();
        boolean doId = writeId.isSelected();
        boolean doLeds = writeLeds.isSelected();

        JDialog progress = new JDialog(owner, "Modify Board", Dialog.ModalityType.APPLICATION_MODAL);
        JLabel step = new JLabel("Writing to the board...");
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        p.add(step, BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        progress.setContentPane(p);
        progress.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        progress.pack();
        progress.setSize(Math.max(progress.getWidth(), 360), progress.getHeight());
        progress.setLocationRelativeTo(owner);

        List<String> problems = new ArrayList<>();
        UnitInfo[] after = {null};
        new Thread(() -> {
            if (doId) {
                SwingUtilities.invokeLater(() -> step.setText("Writing board ID " + id + "..."));
                st.writeBoardID(id, position(doLabel ? label : "", Integer.parseInt(id)));
                UnitPort.sleep(5000); // the board restarts after a new ID
            }
            if (doLabel) {
                SwingUtilities.invokeLater(() -> step.setText("Writing board label " + label + "..."));
                if (!st.writeLabel(label)) {
                    problems.add("The board label wasn't stored. This board's firmware is too old for labels; update it in Developer Mode > Flash Firmware.");
                }
            }
            if (doLeds) {
                SwingUtilities.invokeLater(() -> step.setText("Writing LED count " + leds + "..."));
                st.writeLEDCount(leds);
                UnitPort.sleep(4000); // the board restarts after a new LED count
            }
            SwingUtilities.invokeLater(() -> step.setText("Checking the board..."));
            after[0] = st.getSerialPort() == null ? null : UnitPort.queryInfo(st.getSerialPort(), 6000);
            SwingUtilities.invokeLater(() -> {
                progress.dispose();
                StringBuilder message = new StringBuilder();
                if (after[0] != null) {
                    message.append("This board now has ").append(describe(after[0])).append(".");
                } else {
                    message.append("Values were sent, but the board couldn't be read back to confirm them.");
                }
                for (String problem : problems) {
                    message.append("\n\n").append(problem);
                }
                JOptionPane.showMessageDialog(owner, message.toString(), "Modify Board",
                        problems.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            });
        }, "Modify Board write").start();
        progress.setVisible(true);
    }
}
