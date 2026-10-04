package org.emrick.project.dev;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Shared look for Developer Mode: rounded cards, status pills, gold primary buttons, and table styling.
 * Colors are read from the current FlatLaf theme at paint time, so everything follows dark/light mode.
 */
final class DevUI {
    static final int GAP = 12;

    /** Meaning of a status pill or message. */
    enum Tone { SUCCESS, WARNING, DANGER, INFO, NEUTRAL, ACCENT }

    private DevUI() {
    }

    // ---------------------------------------------------------------- colors

    static boolean dark() {
        return FlatLaf.isLafDark();
    }

    static Color accent() {
        Color c = UIManager.getColor("Component.accentColor");
        return c != null ? c : new Color(0xd6b400);
    }

    static Color background() {
        return UIManager.getColor("Panel.background");
    }

    static Color cardBackground() {
        Color bg = background();
        return dark() ? blend(bg, Color.WHITE, 0.05f) : Color.WHITE;
    }

    static Color cardBorder() {
        Color bg = background();
        return dark() ? blend(bg, Color.WHITE, 0.12f) : blend(bg, Color.BLACK, 0.10f);
    }

    static Color muted() {
        Color c = UIManager.getColor("Label.disabledForeground");
        return c != null ? c : Color.GRAY;
    }

    static Color tone(Tone tone) {
        boolean d = dark();
        return switch (tone) {
            case SUCCESS -> d ? new Color(0x4cc77b) : new Color(0x1e8a4a);
            case WARNING -> d ? new Color(0xf0b43c) : new Color(0xb36b00);
            case DANGER -> d ? new Color(0xff6b6b) : new Color(0xc62828);
            case INFO -> d ? new Color(0x6aa8ff) : new Color(0x1f63c6);
            case NEUTRAL -> muted();
            case ACCENT -> accent();
        };
    }

    static Color blend(Color a, Color b, float amount) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
    }

    // ---------------------------------------------------------------- type
    // Same scale as the rest of Emrick Designer: the normal body font everywhere, bold 16 for page headings
    // (like the welcome screen), and plain titled-border text for section names (like "Effect View").

    static Font body() {
        return UIManager.getFont("Label.font");
    }

    static Font heading() {
        return body().deriveFont(Font.BOLD, 16f);
    }

    static Font subheading() {
        return body().deriveFont(Font.BOLD, 14f);
    }

    /** Section name inside a card, styled like the app's titled borders. */
    static JLabel section(String text) {
        JLabel l = new JLabel(text) {
            @Override
            public Color getForeground() {
                Color c = UIManager.getColor("TitledBorder.titleColor");
                return c != null ? c : UIManager.getColor("Label.foreground");
            }
        };
        Font f = UIManager.getFont("TitledBorder.font");
        l.setFont(f != null ? f : body());
        return l;
    }

    // ---------------------------------------------------------------- components

    /** Rounded panel with a subtle border, the basic building block of every tab. */
    static class Card extends JPanel {
        Card(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cardBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(cardBorder());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small rounded label with a tinted background, e.g. "Ready", "Outdated", "Failed". */
    static class Pill extends JLabel {
        private Tone tone;

        Pill(String text, Tone tone) {
            super(text);
            this.tone = tone;
            setBorder(new EmptyBorder(3, 10, 3, 10));
            setFont(body().deriveFont(Font.BOLD));
            setOpaque(false);
        }

        void set(String text, Tone tone) {
            setText(text);
            this.tone = tone;
            repaint();
        }

        @Override
        public Color getForeground() {
            return tone == null ? super.getForeground() : tone(tone);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (tone != null && getText() != null && !getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = tone(tone);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), dark() ? 46 : 34));
                Dimension size = getPreferredSize();
                int w = Math.min(getWidth(), size.width);
                int h = Math.min(getHeight(), size.height);
                int y = (getHeight() - h) / 2;
                int x = getHorizontalAlignment() == SwingConstants.CENTER ? (getWidth() - w) / 2 : 0;
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Table cell renderer that draws the value as a {@link Pill}. */
    static class PillRenderer implements javax.swing.table.TableCellRenderer {
        // GridBagLayout centers the pill vertically in the row at any row height or display scaling
        private final JPanel holder = new JPanel(new GridBagLayout());
        private final Pill pill = new Pill("", Tone.NEUTRAL);
        private final java.util.function.Function<Object, Tone> toneOf;
        private final java.util.function.Function<Object, String> textOf;

        PillRenderer(java.util.function.Function<Object, String> textOf, java.util.function.Function<Object, Tone> toneOf) {
            this.textOf = textOf;
            this.toneOf = toneOf;
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.LINE_START;
            c.weightx = 1;
            c.insets = new Insets(0, 6, 0, 6);
            holder.add(pill, c);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            holder.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            pill.set(textOf.apply(value), toneOf.apply(value));
            holder.setToolTipText(textOf.apply(value));
            return holder;
        }
    }

    /** Firmware dates are stored as YYYY-MM-DD; people see MM-DD-YYYY. Anything else is shown unchanged. */
    static String date(String isoDate) {
        if (isoDate != null && isoDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return isoDate.substring(5, 7) + "-" + isoDate.substring(8, 10) + "-" + isoDate.substring(0, 4);
        }
        return isoDate;
    }

    /** Muted text that wraps to the available width instead of being cut off with "...". */
    static JTextArea wrappingCaption(String text) {
        JTextArea area = new JTextArea(text) {
            @Override
            public Color getForeground() {
                return muted();
            }
        };
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(body());
        return area;
    }

    /**
     * FlowLayout that wraps onto extra lines when the row is too narrow, and reports the wrapped height so the
     * surrounding layout makes room for it (plain FlowLayout just lets components spill off the edge).
     */
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        private int requestedHeight = -1;

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public void layoutContainer(Container target) {
            super.layoutContainer(target);
            // The height needed depends on the final width, which is only known now. If it differs from what the
            // parent made room for, re-validate once so there's no leftover gap or clipped row.
            int needed = layoutSize(target, true).height;
            if (needed != target.getHeight() && needed != requestedHeight) {
                requestedHeight = needed;
                SwingUtilities.invokeLater(target::revalidate);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = layoutSize(target, false);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                // Use the width actually available; before the first layout, look up to the nearest sized parent
                Container sized = target;
                while (sized.getSize().width == 0 && sized.getParent() != null) {
                    sized = sized.getParent();
                }
                int targetWidth = sized.getSize().width;
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;
                for (Component m : target.getComponents()) {
                    if (!m.isVisible()) {
                        continue;
                    }
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + getVgap();
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) {
                        rowWidth += getHgap();
                    }
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight;
                dim.width += insets.left + insets.right + getHgap() * 2;
                dim.height += insets.top + insets.bottom + getVgap() * 2;
                return dim;
            }
        }
    }

    /**
     * Grid of cards whose column count follows the available width (up to {@code maxColumns}), so cards
     * stack instead of squeezing when the window is narrow. With {@code fillHeight}, a single row stretches
     * to the available height.
     */
    static class ResponsiveGrid implements LayoutManager {
        private final int minCellWidth;
        private final int maxColumns;
        private final int gap;
        private final boolean fillHeight;

        ResponsiveGrid(int minCellWidth, int maxColumns, int gap, boolean fillHeight) {
            this.minCellWidth = minCellWidth;
            this.maxColumns = maxColumns;
            this.gap = gap;
            this.fillHeight = fillHeight;
        }

        private int columns(Container parent, int width) {
            int n = (int) java.util.Arrays.stream(parent.getComponents()).filter(Component::isVisible).count();
            int fit = Math.max(1, (width + gap) / (minCellWidth + gap));
            return Math.max(1, Math.min(Math.min(maxColumns, fit), Math.max(1, n)));
        }

        private int[] rowHeights(Component[] comps, int cols) {
            int rows = (comps.length + cols - 1) / cols;
            int[] heights = new int[rows];
            for (int i = 0; i < comps.length; i++) {
                heights[i / cols] = Math.max(heights[i / cols], comps[i].getPreferredSize().height);
            }
            return heights;
        }

        private Component[] visible(Container parent) {
            return java.util.Arrays.stream(parent.getComponents()).filter(Component::isVisible).toArray(Component[]::new);
        }

        private int availableWidth(Container parent) {
            Container sized = parent;
            while (sized.getWidth() == 0 && sized.getParent() != null) {
                sized = sized.getParent();
            }
            Insets in = parent.getInsets();
            return Math.max(minCellWidth, sized.getWidth() - in.left - in.right);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Component[] comps = visible(parent);
            int cols = columns(parent, availableWidth(parent));
            int[] heights = rowHeights(comps, cols);
            int h = java.util.Arrays.stream(heights).sum() + gap * Math.max(0, heights.length - 1);
            Insets in = parent.getInsets();
            return new Dimension(minCellWidth + in.left + in.right, h + in.top + in.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Component[] comps = visible(parent);
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int cols = columns(parent, width);
            int[] heights = rowHeights(comps, cols);
            if (fillHeight && heights.length == 1) {
                heights[0] = Math.max(heights[0], parent.getHeight() - in.top - in.bottom);
            }
            int cellWidth = (width - gap * (cols - 1)) / cols;
            int y = in.top;
            for (int r = 0; r < heights.length; r++) {
                for (int c = 0; c < cols; c++) {
                    int i = r * cols + c;
                    if (i < comps.length) {
                        comps[i].setBounds(in.left + c * (cellWidth + gap), y, cellWidth, heights[r]);
                    }
                }
                y += heights[r] + gap;
            }
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }
    }

    /**
     * Panel for scroll panes that follows the viewport's width (so content reflows) and only scrolls vertically
     * when the window is too short for it.
     */
    static class ReflowPanel extends JPanel implements Scrollable {
        ReflowPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // Fill the viewport when there's room; scroll when there isn't
            return getParent() instanceof JViewport vp && vp.getHeight() > getPreferredSize().height;
        }
    }

    /** Wraps content so it reflows to the window width and scrolls vertically only when it doesn't fit. */
    static JScrollPane reflowScroll(JComponent content) {
        ReflowPanel panel = new ReflowPanel(new BorderLayout());
        panel.add(content, BorderLayout.CENTER);
        JScrollPane sp = borderless(panel);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /**
     * Shows ports in a dropdown by what's plugged in (board label, else "Board 12", else unit type) with the COM
     * port in gray after it. The dropdown's values stay the port names. Other entries are shown as-is.
     */
    static ListCellRenderer<Object> unitNameRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof String port) {
                    String name = UnitPort.displayName(port);
                    if (!name.equals(port)) {
                        Color gray = muted();
                        setText("<html>" + escape(name) + "&nbsp;&nbsp;<span style='color:rgb(" + gray.getRed() + ","
                                + gray.getGreen() + "," + gray.getBlue() + ")'>" + escape(port) + "</span></html>");
                    }
                }
                return this;
            }
        };
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(subheading());
        return l;
    }

    static JLabel caption(String text) {
        JLabel l = new JLabel(text) {
            @Override
            public Color getForeground() {
                return muted();
            }
        };
        return l;
    }

    private static final String PRIMARY_STYLE = "background: $Component.accentColor; foreground: #1d1d1d; font: bold;"
            + " hoverBackground: darken($Component.accentColor,6%); pressedBackground: darken($Component.accentColor,12%);"
            + " borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; margin: 6,16,6,16";
    private static final String PLAIN_BOLD_STYLE = "font: bold; margin: 6,16,6,16";
    private static final String PRIMARY_KEY = "DevUI.primary";

    /** Gold call-to-action button. Looks like a normal button while disabled so it doesn't read as clickable. */
    static JButton primary(String text) {
        JButton b = new JButton(text);
        b.addPropertyChangeListener("enabled", e -> applyPrimaryStyle(b));
        setPrimary(b, true);
        return b;
    }

    /** Switches a {@link #primary} button between gold and plain (e.g. Connect is gold, Disconnect is plain). */
    static void setPrimary(JButton b, boolean primary) {
        b.putClientProperty(PRIMARY_KEY, primary);
        applyPrimaryStyle(b);
    }

    private static void applyPrimaryStyle(JButton b) {
        boolean gold = Boolean.TRUE.equals(b.getClientProperty(PRIMARY_KEY)) && b.isEnabled();
        b.putClientProperty(FlatClientProperties.STYLE, gold ? PRIMARY_STYLE : PLAIN_BOLD_STYLE);
    }

    /** Neutral button with comfortable padding. */
    static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.STYLE, "margin: 6,14,6,14");
        return b;
    }

    /** Red-outlined button for actions that stop or turn things off. */
    static JButton danger(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.STYLE, "margin: 6,14,6,14; foreground: "
                + (dark() ? "#ff6b6b" : "#c62828"));
        return b;
    }

    static JTextField field(int columns, String placeholder) {
        JTextField f = new JTextField(columns);
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.putClientProperty(FlatClientProperties.STYLE, "margin: 6,10,6,10");
        return f;
    }

    /** Keyboard key hint, e.g. "Space". */
    static JLabel keycap(String key) {
        JLabel l = new JLabel(key) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardBorder());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return muted();
            }
        };
        l.setBorder(new EmptyBorder(1, 6, 1, 6));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /** Clean, roomy table look: no vertical grid lines, taller rows, bold header. */
    static void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        // Keep the dark gold selection even while the table has focus (the bright accent was hard to read)
        table.putClientProperty(FlatClientProperties.STYLE,
                "selectionBackground: $Table.selectionInactiveBackground; selectionForeground: $Table.selectionInactiveForeground");
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.putClientProperty(FlatClientProperties.STYLE, "height: 32; separatorColor: $Table.background; bottomSeparatorColor: $Component.borderColor");
        // Left-align headings so they line up with the cell text
        javax.swing.table.TableCellRenderer headerRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((t, v, s, f, r, c) -> {
            Component comp = headerRenderer.getTableCellRendererComponent(t, v, s, f, r, c);
            if (comp instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.LEADING);
            }
            return comp;
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, false, r, c);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }

    /** Scroll pane without its own border, for tables inside a card. */
    static JScrollPane borderless(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return sp;
    }

    /** Big centered message for empty lists, e.g. "Plug in a unit". */
    static JPanel emptyState(String headline, String detail) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel stack = new JPanel(new GridLayout(0, 1, 0, 4));
        stack.setOpaque(false);
        JLabel h = new JLabel(headline, SwingConstants.CENTER);
        h.setFont(subheading());
        stack.add(h);
        JLabel d = caption(detail);
        d.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(d);
        p.add(stack);
        return p;
    }

    /** Transparent panel, handy for layout wrappers inside cards. */
    static JPanel clear(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }
}
