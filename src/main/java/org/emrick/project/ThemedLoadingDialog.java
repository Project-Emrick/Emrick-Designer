package org.emrick.project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

public final class ThemedLoadingDialog extends JDialog {
    private JLabel titleLabel;
    private JLabel detailLabel;
    private JLabel footerLabel;

    private JProgressBar progressBar;
    private JLabel spinnerLabel;
    private JLabel dotsLabel;
    private JLabel segment1;
    private JLabel segment2;
    private JLabel segment3;
    private JLabel segment4;

    private Timer animationTimer;
    private int animationTick = 0;
    private Runnable cancelAction = () -> {
    };
    private boolean cancellationRequested;

    private final VisualStyle style;
    private final StyleSpec spec;

    private static final String[] SPINNER_FRAMES = new String[] {"|", "/", "-", "\\"};

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
        this.style = style;
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        this.spec = buildBrandSpec();
        JPanel content = buildBrandLayout(badgeText, initialTitle, initialDetail, footerText);

        setContentPane(content);
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
                if (!cancellationRequested) {
                    cancellationRequested = true;
                    cancelAction.run();
                }
                dispose();
            }
        });

        startAnimation();
    }

    public void setCancelAction(Runnable cancelAction) {
        this.cancelAction = Objects.requireNonNull(cancelAction);
    }

    public void update(String title, String detail) {
        titleLabel.setText(Objects.requireNonNullElse(title, "Working..."));
        detailLabel.setText(Objects.requireNonNullElse(detail, ""));

        if (footerLabel != null && style == VisualStyle.MONOLITH) {
            footerLabel.setText("Live update: " + detail);
        }

        if (style == VisualStyle.SUNSET) {
            int stage = inferStageIndex(title + " " + detail);
            highlightSegment(stage);
        }
    }

    public record StatusUpdate(String title, String detail) {
    }

    private JPanel buildBrandLayout(String badgeText,
                                    String initialTitle,
                                    String initialDetail,
                                    String footerText) {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(new EmptyBorder(spec.outerPadding, spec.outerPadding, spec.outerPadding, spec.outerPadding));
        content.setBackground(spec.panelBackground);

        JPanel headlinePanel = new JPanel(new BorderLayout(0, spec.headerGap));
        headlinePanel.setBackground(spec.cardBackground);
        headlinePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(spec.borderColor, spec.borderWidth, true),
                new EmptyBorder(spec.cardPadding, spec.cardPadding, spec.cardPadding, spec.cardPadding)
        ));

        JLabel badgeLabel = new JLabel(badgeText);
        badgeLabel.setOpaque(true);
        badgeLabel.setBackground(spec.badgeBackground);
        badgeLabel.setForeground(spec.badgeForeground);
        badgeLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
        badgeLabel.setFont(badgeLabel.getFont().deriveFont(Font.BOLD, 11f));

        JPanel badgeRow = new JPanel(new BorderLayout());
        badgeRow.setOpaque(false);
        badgeRow.add(badgeLabel, BorderLayout.WEST);
        headlinePanel.add(badgeRow, BorderLayout.NORTH);

        JPanel headlineTextPanel = new JPanel(new BorderLayout(0, spec.titleGap));
        headlineTextPanel.setOpaque(false);

        titleLabel = new JLabel(initialTitle);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(spec.labelForeground);
        headlineTextPanel.add(titleLabel, BorderLayout.NORTH);

        detailLabel = new JLabel(initialDetail);
        detailLabel.setFont(detailLabel.getFont().deriveFont(Font.PLAIN, 13f));
        detailLabel.setForeground(spec.labelForeground);
        headlineTextPanel.add(detailLabel, BorderLayout.CENTER);

        headlinePanel.add(headlineTextPanel, BorderLayout.CENTER);
        content.add(headlinePanel, BorderLayout.NORTH);

        JPanel progressPanel = new JPanel(new BorderLayout(0, 10));
        progressPanel.setOpaque(false);
        progressPanel.add(buildIndicatorPanel(), BorderLayout.NORTH);

        footerLabel = new JLabel(footerText);
        footerLabel.setFont(footerLabel.getFont().deriveFont(Font.PLAIN, 12f));
        footerLabel.setForeground(spec.footerForeground);
        progressPanel.add(footerLabel, BorderLayout.CENTER);

        content.add(progressPanel, BorderLayout.CENTER);
        return content;
    }

    private JComponent buildIndicatorPanel() {
        switch (style) {
            case MIDNIGHT:
                return buildSpinnerIndicator();
            case MONOLITH:
                return buildDotsIndicator();
            case SUNSET:
                return buildSegmentIndicator();
            case AURORA:
            default:
                return buildBarIndicator();
        }
    }

    private JComponent buildBarIndicator() {
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(420, spec.progressHeight));
        progressBar.setBorderPainted(false);
        progressBar.setForeground(spec.progressForeground);
        progressBar.setBackground(spec.progressTrack);
        return progressBar;
    }

    private JComponent buildSpinnerIndicator() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);

        spinnerLabel = new JLabel(SPINNER_FRAMES[0], SwingConstants.CENTER);
        spinnerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        spinnerLabel.setForeground(spec.progressForeground);
        spinnerLabel.setPreferredSize(new Dimension(22, spec.progressHeight + 8));

        JLabel caption = new JLabel("Rotating Spinner");
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 12f));
        caption.setForeground(spec.labelForeground);

        JPanel rail = new JPanel(new BorderLayout());
        rail.setOpaque(true);
        rail.setBackground(spec.progressTrack);
        rail.setBorder(new EmptyBorder(6, 10, 6, 10));
        rail.add(caption, BorderLayout.CENTER);

        panel.add(spinnerLabel, BorderLayout.WEST);
        panel.add(rail, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(420, spec.progressHeight + 8));
        return panel;
    }

    private JComponent buildDotsIndicator() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);

        dotsLabel = new JLabel("Loading", SwingConstants.LEFT);
        dotsLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        dotsLabel.setForeground(spec.progressForeground);

        JPanel rail = new JPanel(new BorderLayout());
        rail.setOpaque(true);
        rail.setBackground(spec.progressTrack);
        rail.setBorder(new EmptyBorder(6, 10, 6, 10));
        rail.add(dotsLabel, BorderLayout.CENTER);

        panel.add(rail, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(420, spec.progressHeight + 8));
        return panel;
    }

    private JComponent buildSegmentIndicator() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 6, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(420, spec.progressHeight + 8));

        segment1 = buildSegmentCell();
        segment2 = buildSegmentCell();
        segment3 = buildSegmentCell();
        segment4 = buildSegmentCell();

        panel.add(segment1);
        panel.add(segment2);
        panel.add(segment3);
        panel.add(segment4);

        highlightSegment(0);
        return panel;
    }

    private JLabel buildSegmentCell() {
        JLabel cell = new JLabel();
        cell.setOpaque(true);
        cell.setBorder(BorderFactory.createLineBorder(spec.borderColor, 1, true));
        return cell;
    }

    private void startAnimation() {
        animationTimer = new Timer(260, e -> {
            animationTick++;

            if (style == VisualStyle.MIDNIGHT && spinnerLabel != null) {
                spinnerLabel.setText(SPINNER_FRAMES[animationTick % SPINNER_FRAMES.length]);
            }

            if (style == VisualStyle.MONOLITH && dotsLabel != null) {
                int dots = animationTick % 4;
                dotsLabel.setText("Loading" + ".".repeat(dots));
            }

            if (style == VisualStyle.SUNSET) {
                highlightSegment(animationTick % 4);
            }
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    private void highlightSegment(int idx) {
        if (segment1 == null) {
            return;
        }

        Color inactive = blend(spec.panelBackground, spec.labelForeground, 0.14f);
        Color active = spec.progressForeground;

        segment1.setBackground(idx >= 0 ? active : inactive);
        segment2.setBackground(idx >= 1 ? active : inactive);
        segment3.setBackground(idx >= 2 ? active : inactive);
        segment4.setBackground(idx >= 3 ? active : inactive);
    }

    private int inferStageIndex(String value) {
        String v = value.toLowerCase();
        if (v.contains("prepar") || v.contains("reset")) return 0;
        if (v.contains("unpack") || v.contains("archive") || v.contains("extract")) return 1;
        if (v.contains("read") || v.contains("parse") || v.contains("load")) return 2;
        if (v.contains("restor") || v.contains("ready") || v.contains("complete")) return 3;
        return animationTick % 4;
    }

    private StyleSpec buildBrandSpec() {
        Color panelBackground = firstColor(UIManager.getColor("Panel.background"), getBackground());
        Color labelForeground = firstColor(UIManager.getColor("Label.foreground"), getForeground(), Color.BLACK);
        Color footerForeground = firstColor(UIManager.getColor("Label.disabledForeground"), labelForeground);
        Color accentColor = firstColor(
                UIManager.getColor("ProgressBar.foreground"),
                UIManager.getColor("Component.focusColor"),
                UIManager.getColor("MenuItem.underlineSelectionColor"),
                UIManager.getColor("accentFocusColor"),
                new Color(0x2D6CDF)
        );
        Color borderColor = firstColor(
                UIManager.getColor("Component.borderColor"),
                blend(panelBackground, labelForeground, 0.18f)
        );

        Color cardBackground = blend(panelBackground, accentColor, isDark(panelBackground) ? 0.14f : 0.08f);
        Color progressTrack = blend(panelBackground, labelForeground, isDark(panelBackground) ? 0.18f : 0.08f);

        return new StyleSpec(
                panelBackground,
                cardBackground,
                borderColor,
                accentColor,
                readableTextColor(accentColor),
                accentColor,
                progressTrack,
                labelForeground,
                footerForeground,
                20,
                14,
                14,
                12,
                1,
                12
        );
    }

    private static Color firstColor(Color... colors) {
        for (Color color : colors) {
            if (color != null) {
                return color;
            }
        }
        return Color.BLUE;
    }

    private static Color blend(Color base, Color tint, float tintWeight) {
        float clampedWeight = Math.max(0f, Math.min(1f, tintWeight));
        float baseWeight = 1f - clampedWeight;
        return new Color(
                Math.round(base.getRed() * baseWeight + tint.getRed() * clampedWeight),
                Math.round(base.getGreen() * baseWeight + tint.getGreen() * clampedWeight),
                Math.round(base.getBlue() * baseWeight + tint.getBlue() * clampedWeight)
        );
    }

    private static boolean isDark(Color color) {
        double luminance = (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255.0;
        return luminance < 0.5;
    }

    private static Color readableTextColor(Color background) {
        return isDark(background) ? Color.WHITE : new Color(24, 28, 33);
    }

    private record StyleSpec(Color panelBackground,
                             Color cardBackground,
                             Color borderColor,
                             Color badgeBackground,
                             Color badgeForeground,
                             Color progressForeground,
                             Color progressTrack,
                             Color labelForeground,
                             Color footerForeground,
                             int outerPadding,
                             int cardPadding,
                             int headerGap,
                             int titleGap,
                             int borderWidth,
                             int progressHeight) {
    }
}
