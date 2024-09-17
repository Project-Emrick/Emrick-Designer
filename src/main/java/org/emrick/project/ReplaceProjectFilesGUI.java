package org.emrick.project;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ReplaceProjectFilesGUI extends JDialog implements ActionListener {
    private Frame parent;
    private JButton drillButton;
    private JButton archiveButton;
    private JLabel drillLabel;
    private JLabel archiveLabel;
    private JButton importButton;
    private JButton cancelButton;
    private File drill;
    private File archive;
    private ReplaceFilesListener listener;

    public ReplaceProjectFilesGUI(Frame parent, ReplaceFilesListener listener) {
        super(parent);
        this.parent = parent;
        this.listener = listener;
        initialize();
    }

    public void initialize() {
        setSize(300, 400);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setTitle("Replace Project Files");
        setResizable(false);
        setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        drillButton = new JButton("Drill");
        drillButton.addActionListener(this);
        drillLabel = new JLabel("");
        archiveButton = new JButton("Archive");
        archiveButton.addActionListener(this);
        archiveLabel = new JLabel("");
        JPanel drillPanel = new JPanel();
        drillPanel.setPreferredSize(new Dimension(300, 150));
        drillPanel.setMinimumSize(new Dimension(300, 150));
        drillPanel.setMaximumSize(new Dimension(300, 150));
        drillPanel.setBorder(BorderFactory.createTitledBorder("Drill (.pdf)"));
        drillPanel.setLayout(new BoxLayout(drillPanel, BoxLayout.X_AXIS));
        drillPanel.add(drillButton);
        drillPanel.add(drillLabel);
        this.getContentPane().add(drillPanel);
        JPanel archivePanel = new JPanel();
        archivePanel.setPreferredSize(new Dimension(300, 150));
        archivePanel.setMinimumSize(new Dimension(300, 150));
        archivePanel.setMaximumSize(new Dimension(300, 150));
        archivePanel.setBorder(BorderFactory.createTitledBorder("Archive (.3dz)"));
        archivePanel.setLayout(new BoxLayout(archivePanel, BoxLayout.X_AXIS));
        archivePanel.add(archiveButton);
        archivePanel.add(archiveLabel);
        this.add(archivePanel);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setPreferredSize(new Dimension(300, 150));
        bottomPanel.setMinimumSize(new Dimension(300, 150));
        bottomPanel.setMaximumSize(new Dimension(300, 150));
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        bottomPanel.add(cancelButton);
        importButton = new JButton("Import");
        importButton.addActionListener(this);
        bottomPanel.add(importButton);
        this.add(bottomPanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == drillButton) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Drill Select");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                drillLabel.setText(chooser.getSelectedFile().getName());
                drill = chooser.getSelectedFile();
            }
        } else if (e.getSource() == archiveButton) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Archive Select");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileNameExtensionFilter("Pyware Archives", "3dz"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                archiveLabel.setText(chooser.getSelectedFile().getName());
                archive = chooser.getSelectedFile();
            }
        } else if (e.getSource() == importButton) {
            if (listener.onNewFileSelect(drill, archive)) {
                this.dispose();
            }
        } else if (e.getSource() == cancelButton) {
            this.dispose();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Replace Project Files");
        frame.setSize(new Dimension(800, 600));
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        JButton showButton = new JButton("Show");
        showButton.setPreferredSize(new Dimension(100, 50));
        showButton.setMaximumSize(new Dimension(100, 50));
        showButton.addActionListener(e -> {
            ReplaceProjectFilesGUI dialog = new ReplaceProjectFilesGUI(frame, (drill, archive) -> false);
            dialog.setVisible(true);
        });
        frame.add(showButton);
        frame.setVisible(true);
    }
}
