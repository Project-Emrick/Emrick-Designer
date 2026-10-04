package org.emrick.project.dev;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Developer Mode: unit details, a live terminal, unit lookup / ID sweep, and receiver flashing.
 * Shown in the main window under the normal menu bar in place of the show designer view; the project
 * stays loaded underneath.
 */
public class DeveloperModeGUI extends JPanel {
    private static final AtomicBoolean releaseCheckStarted = new AtomicBoolean(false);

    private final UnitsPanel unitsPanel;
    private final TerminalPanel terminalPanel;
    private final LookupPanel lookupPanel;
    private final FlashPanel flashPanel;

    public DeveloperModeGUI(DevModeHost host) {
        super(new BorderLayout());
        setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel header = DevUI.clear(new BorderLayout(DevUI.GAP, 0));
        header.setBorder(new EmptyBorder(0, 2, 10, 2));
        JPanel titles = DevUI.clear(new GridLayout(2, 1, 0, 2));
        JLabel title = new JLabel("Developer Mode");
        title.setFont(DevUI.heading());
        titles.add(title);
        titles.add(DevUI.caption("Inspect, find, debug, and flash Emrick units."));
        header.add(titles, BorderLayout.CENTER);
        JButton back = DevUI.secondary("Back to Show Designer");
        back.addActionListener(e -> host.exitDeveloperMode());
        JPanel backHolder = DevUI.clear(new GridBagLayout());
        backHolder.add(back);
        header.add(backHolder, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        unitsPanel = new UnitsPanel(host);
        terminalPanel = new TerminalPanel(host);
        lookupPanel = new LookupPanel(host);
        flashPanel = new FlashPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.putClientProperty(FlatClientProperties.STYLE,
                "tabHeight: 40; minimumTabWidth: 130; tabInsets: 4,18,4,18; underlineColor: $Component.accentColor;"
                        + " inactiveUnderlineColor: $Component.accentColor; tabSelectionHeight: 3; tabAreaInsets: 0,0,8,0");
        tabs.addTab("Units", unitsPanel);
        tabs.addTab("Terminal", terminalPanel);
        tabs.addTab("Lookup", lookupPanel);
        tabs.addTab("Flash Firmware", flashPanel);
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected == lookupPanel) {
                lookupPanel.onShown();
            } else if (selected == flashPanel) {
                flashPanel.onShown();
            }
        });
        add(tabs, BorderLayout.CENTER);

        // Learn the latest published firmware once per session so units can be flagged as outdated
        if (releaseCheckStarted.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    FirmwareRelease.fetchLatest();
                } catch (Exception e) {
                    System.out.println("Developer Mode: couldn't check for the latest receiver firmware: " + e.getMessage());
                }
                SwingUtilities.invokeLater(() -> {
                    unitsPanel.refreshDisplay();
                    flashPanel.refreshReleaseCard();
                });
            }, "Firmware release check").start();
        }
    }

    /** Called when Developer Mode is shown. */
    public void start() {
        unitsPanel.start();
        lookupPanel.reloadProject();
    }

    /** Called after a project is opened while Developer Mode is showing. */
    public void onProjectChanged() {
        lookupPanel.reloadProject();
        unitsPanel.refreshDisplay();
    }

    /** Called when leaving Developer Mode or closing Emrick Designer: closes ports and stops background work. */
    public void shutdown() {
        unitsPanel.shutdown();
        terminalPanel.shutdown();
        lookupPanel.shutdown();
        flashPanel.shutdown();
    }

    /**
     * Shuts down so a show can run: closes the terminal, stops reading units, turns off any lookup, and waits
     * (up to {@code timeoutMs}) for those to let go of their ports, so the show gets the transmitter. Receivers
     * that are being flashed keep flashing; that never uses the transmitter.
     */
    public void shutdownForShow(long timeoutMs) {
        shutdown();
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (UnitPort.isAnyPortOwnedBy("Terminal", "Lookup", "Units") && System.currentTimeMillis() < deadline) {
            UnitPort.sleep(50);
        }
    }

    /** @return true if leaving now would interrupt a flash in progress */
    public boolean isBusy() {
        return flashPanel.isFlashing();
    }
}
