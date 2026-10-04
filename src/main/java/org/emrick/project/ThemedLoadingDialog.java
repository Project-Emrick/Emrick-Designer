package org.emrick.project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

/**
 * Progress window for long tasks (opening a project, importing, installing the board driver).
 *
 * Shows a spinner, the current step, and a progress bar. For project loading the bar moves forward through the
 * known stages (unpacking, reading, assets, building the view); for other tasks it slides until done. Colors
 * come from the current theme so it matches the rest of Emrick Designer in dark and light mode.
 */
public final class ThemedLoadingDialog extends JDialog {
    private static final int DIALOG_WIDTH = 460;

    /**
     * Known stages of opening/importing a project, matched against the status title, and how far along each
     * one is. Unknown titles keep the bar where it is.
     */
    private static final String[][] STAGES = {
            {"validat", "0.06"},
            {"prepar", "0.10"},
            {"unpack", "0.25"},
            {"reading", "0.40"},
            {"loading asset", "0.55"},
            {"loading audio", "0.55"},
            {"parsing", "0.65"},
            {"finalizing data", "0.75"},
            {"finalizing view", "0.88"},
    };
    private static final int STAGE_COUNT = 5; // shown to the user as "Step n of 5"

    private final JLabel titleLabel = new JLabel();
    private final JTextArea detailText = new JTextArea();
    private final Spinner spinner = new Spinner();
    private final ProgressLine progress = new ProgressLine();
    private final JLabel stepLabel = new JLabel(" ");
    private final JButton cancelButton = new JButton("Cancel");

    private Timer animationTimer;
    private Runnable cancelAction = null;
    private boolean cancellationRequested;

    /** Kept so existing callers still compile; every style now uses the same design. */
    public enum VisualStyle {
        AURORA,
        MIDNIGHT,
        MONOLITH,
        SUNSET
    }

    public ThemedLoadingDialog(Window owner,
                               String dialogTitle,
                               String badgeText,
                               String initialTitle,
                               String initialDetail,
                               String footerText) {
        this(owner, dialogTitle, badgeText, initialTitle, initialDetail, footerText, VisualStyle.AURORA, true);
    }

    public ThemedLoadingDialog(Window owner,
                               String dialogTitle,
                               String badgeText,
                               String initialTitle,
                               String initialDetail,
                               String footerText,
                               VisualStyle style,
                               boolean modal) {
        super(owner, dialogTitle, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setContentPane(buildLayout(badgeText, initialTitle, initialDetail, footerText));
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopAnimation();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                requestCancel();
            }
        });

        startAnimation();
    }

    /** Called if the user cancels (Cancel button or closing the window). Shows the Cancel button. */
    public void setCancelAction(Runnable cancelAction) {
        this.cancelAction = Objects.requireNonNull(cancelAction);
        cancelButton.setVisible(true);
        pack();
    }

    public void update(String title, String detail) {
        titleLabel.setText(Objects.requireNonNullElse(title, "Working..."));
        detailText.setText(Objects.requireNonNullElse(detail, ""));

        String t = String.valueOf(title).toLowerCase();
        for (int i = 0; i < STAGES.length; i++) {
            if (t.contains(STAGES[i][0])) {
                float target = Float.parseFloat(STAGES[i][1]);
                progress.setTarget(target);
                int step = Math.min(STAGE_COUNT, 1 + (int) (target * STAGE_COUNT));
                stepLabel.setText("Step " + step + " of " + STAGE_COUNT);
                break;
            }
        }
        // Long details wrap onto more lines; grow the window to fit instead of cutting them off
        if (getPreferredSize().height != getHeight()) {
            pack();
        }
    }

    public record StatusUpdate(String title, String detail) {
    }

    private void requestCancel() {
        if (cancellationRequested) {
            return;
        }
        cancellationRequested = true;
        if (cancelAction != null) {
            cancelAction.run();
        }
        dispose();
    }

    // ---------------------------------------------------------------- layout

    private JPanel buildLayout(String badgeText, String initialTitle, String initialDetail, String footerText) {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(new EmptyBorder(22, 24, 18, 24));

        // Spinner beside the task name and current step
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel spinnerHolder = new JPanel(new GridBagLayout());
        spinnerHolder.setOpaque(false);
        spinnerHolder.add(spinner);
        header.add(spinnerHolder, BorderLayout.WEST);

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        JLabel badge = new MutedLabel(prettify(badgeText));
        text.add(badge, BorderLayout.NORTH);
        titleLabel.setText(initialTitle);
        titleLabel.setFont(baseFont().deriveFont(Font.BOLD, 16f));
        text.add(titleLabel, BorderLayout.CENTER);
        header.add(text, BorderLayout.CENTER);
        content.add(header, BorderLayout.NORTH);

        // Detail, progress, and footer
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        styleWrapping(detailText, false);
        detailText.setText(initialDetail);
        detailText.setColumns(1); // width comes from the dialog, not the text
        detailText.setSize(DIALOG_WIDTH - 48, Short.MAX_VALUE); // lets the text area work out its wrapped height
        body.add(detailText, BorderLayout.NORTH);

        JPanel progressRow = new JPanel(new BorderLayout(0, 6));
        progressRow.setOpaque(false);
        progressRow.add(progress, BorderLayout.NORTH);
        stepLabel.setFont(baseFont().deriveFont(baseFont().getSize2D() - 1f));
        stepLabel.setForeground(muted());
        progressRow.add(stepLabel, BorderLayout.SOUTH);
        body.add(progressRow, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        JTextArea footerArea = new JTextArea(footerText == null ? "" : footerText);
        styleWrapping(footerArea, true);
        footerArea.setSize(DIALOG_WIDTH - 140, Short.MAX_VALUE);
        footer.add(footerArea, BorderLayout.CENTER);
        cancelButton.setVisible(false);
        cancelButton.setFocusable(false); // no focus ring drawing attention to it; Escape also cancels
        cancelButton.addActionListener(e -> requestCancel());
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        content.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (cancelButton.isVisible()) {
                    requestCancel();
                }
            }
        });
        JPanel cancelHolder = new JPanel(new GridBagLayout());
        cancelHolder.setOpaque(false);
        cancelHolder.add(cancelButton);
        footer.add(cancelHolder, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        return new FixedWidthPanel(content);
    }

    /** "PROJECT LOAD" -> "Project load", so it reads as a label rather than a stamp. */
    private static String prettify(String badge) {
        if (badge == null || badge.isBlank()) {
            return " ";
        }
        String lower = badge.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void styleWrapping(JTextArea area, boolean muted) {
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(muted ? baseFont().deriveFont(baseFont().getSize2D() - 1f) : baseFont());
        if (muted) {
            area.setForeground(muted());
        }
    }

    /** Keeps the window a fixed width so wrapping text grows it downward instead of sideways. */
    private static final class FixedWidthPanel extends JPanel {
        FixedWidthPanel(JComponent content) {
            super(new BorderLayout());
            add(content, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(DIALOG_WIDTH, super.getPreferredSize().height);
        }
    }

    private static final class MutedLabel extends JLabel {
        MutedLabel(String text) {
            super(text);
            setFont(baseFont().deriveFont(baseFont().getSize2D() - 1f));
        }

        @Override
        public Color getForeground() {
            return muted();
        }
    }

    // ---------------------------------------------------------------- animation

    private void startAnimation() {
        animationTimer = new Timer(16, e -> {
            spinner.advance();
            progress.advance();
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    /** Smooth rotating arc. */
    private static final class Spinner extends JComponent {
        private static final int SIZE = 30;
        private double angle = 0;

        Spinner() {
            setPreferredSize(new Dimension(SIZE, SIZE));
        }

        void advance() {
            angle = (angle + 6) % 360;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float stroke = 3.5f;
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double inset = stroke / 2 + 1;
            double d = SIZE - inset * 2;
            g2.setColor(track());
            g2.draw(new Arc2D.Double(inset, inset, d, d, 0, 360, Arc2D.OPEN));
            g2.setColor(accent());
            g2.draw(new Arc2D.Double(inset, inset, d, d, -angle, 100, Arc2D.OPEN));
            g2.dispose();
        }
    }

    /**
     * Slim rounded bar. Eases toward the current stage's position; with no known stage yet it shows a segment
     * sliding across.
     */
    private static final class ProgressLine extends JComponent {
        private static final int HEIGHT = 6;
        private float target = -1;   // -1 = no stage known yet
        private float shown = 0;
        private float slide = 0;

        ProgressLine() {
            setPreferredSize(new Dimension(100, HEIGHT));
        }

        void setTarget(float value) {
            target = Math.max(target, value); // never move backwards
        }

        void advance() {
            if (target >= 0) {
                shown += (target - shown) * 0.08f;
            } else {
                slide = (slide + 0.012f) % 1.4f;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(track());
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            g2.setColor(accent());
            if (target >= 0) {
                float fill = Math.max(h, w * shown);
                g2.fill(new RoundRectangle2D.Float(0, 0, fill, h, h, h));
            } else {
                float segment = w * 0.3f;
                float x = (slide - 0.3f) * w;
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                g2.fill(new RoundRectangle2D.Float(x, 0, segment, h, h, h));
            }
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- theme colors

    private static Font baseFont() {
        Font f = UIManager.getFont("Label.font");
        return f != null ? f : new JLabel().getFont();
    }

    private static Color accent() {
        Color c = UIManager.getColor("Component.accentColor");
        return c != null ? c : new Color(0xd6b400);
    }

    private static Color muted() {
        Color c = UIManager.getColor("Label.disabledForeground");
        return c != null ? c : Color.GRAY;
    }

    private static Color track() {
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        if (bg == null || fg == null) {
            return new Color(128, 128, 128, 60);
        }
        return blend(bg, fg, 0.14f);
    }

    private static Color blend(Color base, Color tint, float amount) {
        return new Color(
                Math.round(base.getRed() + (tint.getRed() - base.getRed()) * amount),
                Math.round(base.getGreen() + (tint.getGreen() - base.getGreen()) * amount),
                Math.round(base.getBlue() + (tint.getBlue() - base.getBlue()) * amount));
    }
}
