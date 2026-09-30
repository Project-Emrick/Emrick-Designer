package org.emrick.project.effect;

import org.emrick.project.LEDStrip;
import org.emrick.project.TimeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.function.Function;

/**
 * Outlook-style "Preview Effect" window: a fixed list of effect types on the left, and the
 * selected effect's edit panel (with the same timeline box used elsewhere in the app above it)
 * filling the rest. Reuses the normal EffectGUI panel for parameter editing (including unlimited
 * chase colors/grid shapes), but is fully self-contained: nothing it does touches the main
 * window's Create Effect panel, timeline selection, or scrub position, except explicitly pushing
 * to hardware.
 */
public class EffectPreviewDialog extends JDialog {

    private static final EffectList[] TYPES = {
            EffectList.GENERATED_FADE, EffectList.STATIC_COLOR, EffectList.WAVE, EffectList.ALTERNATING_COLOR,
            EffectList.RIPPLE, EffectList.CIRCLE_CHASE, EffectList.CHASE, EffectList.NOISE
    };
    private static final String[] LABELS = {
            "Fade", "Static Color", "Wave", "Alternating Color", "Ripple", "Circle Chase", "Chase", "Random Noise"
    };

    private final EffectListener realEffectListener;
    private final EffectListener sandboxedEffectListener = new SandboxedEffectListener();
    private final Function<EffectList, Effect> effectFactory;
    private final JPanel centerContainer = new JPanel(new BorderLayout());
    private final JPanel timelinePreviewContainer = new JPanel(new BorderLayout());
    private final Timer refreshTimer;
    private EffectGUI activeEffectGUI;

    public EffectPreviewDialog(Frame owner, EffectListener effectListener, Function<EffectList, Effect> effectFactory) {
        super(owner, "Preview Effect", false);
        this.realEffectListener = effectListener;
        this.effectFactory = effectFactory;

        setLayout(new BorderLayout());
        setSize(900, 700);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        DefaultListModel<EffectList> listModel = new DefaultListModel<>();
        for (EffectList type : TYPES) listModel.addElement(type);
        JList<EffectList> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                           boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                setText(LABELS[index]);
                setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return this;
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(200, 0));
        listScroll.setBorder(BorderFactory.createTitledBorder("Effects"));

        timelinePreviewContainer.setPreferredSize(new Dimension(400, 100));
        timelinePreviewContainer.setBorder(BorderFactory.createTitledBorder("Timeline Preview"));

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(timelinePreviewContainer, BorderLayout.NORTH);
        centerWrapper.add(centerContainer, BorderLayout.CENTER);

        add(listScroll, BorderLayout.WEST);
        add(centerWrapper, BorderLayout.CENTER);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                selectEffectType(list.getSelectedValue());
            }
        });

        // Timeline box doesn't animate; just periodically re-syncs from the GUI's current fields.
        refreshTimer = new Timer(400, e -> refreshTimelinePreview());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                refreshTimer.stop();
                realEffectListener.onStopHardwarePreview();
            }
        });

        list.setSelectedIndex(0);
        refreshTimer.start();
    }

    private void selectEffectType(EffectList type) {
        Effect effect = effectFactory.apply(type);
        showEffectInPanel(effect, true, -1);
    }

    private void showEffectInPanel(Effect effect, boolean isNew, int index) {
        centerContainer.removeAll();
        activeEffectGUI = new EffectGUI(effect, effect.getStartTimeMSec(), sandboxedEffectListener,
                effect.getEffectType(), isNew, index, true);
        centerContainer.add(activeEffectGUI.getEffectPanel(), BorderLayout.CENTER);
        centerContainer.revalidate();
        centerContainer.repaint();
        refreshTimelinePreview();
    }

    private void refreshTimelinePreview() {
        if (activeEffectGUI == null) return;
        Effect snapshot = activeEffectGUI.getLivePreviewEffect();
        timelinePreviewContainer.removeAll();
        timelinePreviewContainer.add(snapshot.getTimelineWidget(), BorderLayout.CENTER);
        timelinePreviewContainer.revalidate();
        timelinePreviewContainer.repaint();
    }

    /**
     * Delegates only hardware preview and time-sync lookups to the real app; everything else
     * (create/update/delete, panel rebuilds for e.g. chase's "add color", selection, scrubbing)
     * is handled locally so this window never leaks into or out of the main Create Effect panel.
     */
    private class SandboxedEffectListener implements EffectListener {
        @Override
        public void onCreateEffect(Effect effect) {
            // Not reachable: standalone preview hides the Create/Update button.
        }

        @Override
        public void onUpdateEffect(Effect oldEffect, Effect newEffect) {
            // Not reachable: standalone preview hides the Create/Update button.
        }

        @Override
        public void onDeleteEffect(Effect effect) {
            // Not reachable: standalone preview hides the Delete button.
        }

        @Override
        public void onUpdateEffectPanel(Effect effect, boolean isNew, int index) {
            // e.g. Chase's "add another color" / Grid's "add shape" rebuild the panel in place.
            showEffectInPanel(effect, isNew, index);
        }

        @Override
        public void onChangeSelectionMode(boolean isInnerSelect, HashSet<LEDStrip> strips) {
            // No football field selection concept in this standalone window.
        }

        @Override
        public HashSet<LEDStrip> onInnerSelectionRequired() {
            return new HashSet<>();
        }

        @Override
        public HashSet<LEDStrip> onSelectionRequired() {
            return new HashSet<>();
        }

        @Override
        public TimeManager onTimeRequired() {
            return realEffectListener.onTimeRequired();
        }

        @Override
        public void onPressEffect(Effect effect) {
            // Don't scrub/select in the main window from this standalone preview.
        }

        @Override
        public void onPreviewOnHardware(Effect effect) {
            realEffectListener.onPreviewOnHardware(effect);
        }

        @Override
        public void onStopHardwarePreview() {
            realEffectListener.onStopHardwarePreview();
        }

        @Override
        public void onPreviewColorOnHardware(Color color) {
            realEffectListener.onPreviewColorOnHardware(color);
        }
    }
}
