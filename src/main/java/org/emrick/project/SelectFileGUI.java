package org.emrick.project;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * This class constructs a Swing GUI window for importing Coordinates PDF and Pyware Archive file (.3dz)
 * when starting new projects. Coordinates PDF provides necessary performer data, and Pyware Archive (.3dz)
 * provides additional Pyware drill components (e.g., floorCover, ground, surface, audio).
 */
public class SelectFileGUI implements ActionListener {

    // Temporary
    private static final boolean IS_DEBUG = true;

    private final ImportListener importListener;
    // Parent frame
    private final JDialog dialogWindow;
    private final JButton ulCsvButton;
    private final JLabel ulCsvFilename;
    // Upload File Buttons
    private final JButton ulCoordsButton;
    private final JButton ulArchiveButton;
    // Filename Display Labels
    private final JLabel ulCoordsFilename;
    private final JLabel ulArchiveFilename;
    // Import / Cancel Buttons
    private final JButton cancelButton;
    private final JButton importButton;
    // Import Archive Service
    private final ImportArchive importArchive;
    // Paths to selected files
    private File coordsFile;
    private File archiveFile;
    private File csvFile;

    /**
     * Prepare ImportArchive service object.
     *
     * @param importListener Passed down from a class that overrides ImportListener methods.
     *                       Provides callback functionality (e.g., to repaint field after importing
     *                       floorCover or surface images).
     */
    public SelectFileGUI(JFrame parent, ImportListener importListener) {
        this.importListener = importListener;
        importArchive = new ImportArchive(importListener);

        coordsFile = null;
        archiveFile = null;

        dialogWindow = new JDialog(parent, true);
        dialogWindow.setTitle("New Project - Import");
        dialogWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialogWindow.setSize(400, 400);
        dialogWindow.setLocationRelativeTo(null); // center on screen
        dialogWindow.setResizable(false); // resize window option

        JLabel titleLabel = new JLabel("New Project - Import");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Here, "ul" is short for "upload"
        JPanel ulPanel = new JPanel();
        ulPanel.setLayout(new BoxLayout(ulPanel, BoxLayout.PAGE_AXIS));
        ulPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Upload coordinates
        JLabel ulCoordsLabel = new JLabel("Coordinates (.pdf)");
        this.ulCoordsButton = new JButton("Select File");
        this.ulCoordsFilename = new JLabel("No File Selected");

        JPanel ulCoordsPanel = new JPanel();
        ulCoordsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        ulCoordsPanel.add(ulCoordsButton);
        ulCoordsPanel.add(ulCoordsFilename);

        // Upload pyware archive (3dz)
        JLabel ulArchiveLabel = new JLabel("Pyware Archive (.3dz)");
        this.ulArchiveButton = new JButton("Select File");
        this.ulArchiveFilename = new JLabel("No File Selected");

        JPanel ulArchivePanel = new JPanel();
        ulArchivePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        ulArchivePanel.add(ulArchiveButton);
        ulArchivePanel.add(ulArchiveFilename);

        // Upload CSV (csv)
        JLabel ulCsvLabel = new JLabel("Device ID Comma Separated Values (.csv)");
        this.ulCsvButton = new JButton("Select File");
        this.ulCsvFilename = new JLabel("No File Selected");

        JPanel ulCsvPanel = new JPanel();
        ulCsvPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        ulCsvPanel.add(ulCsvButton);
        ulCsvPanel.add(ulCsvFilename);

        // Build UI structure
        ulPanel.add(ulCoordsLabel);
        ulCoordsLabel.setAlignmentX(Component.LEFT_ALIGNMENT); // Need all components left-aligned
        ulPanel.add(ulCoordsPanel);
        ulCoordsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ulPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ulPanel.add(ulArchiveLabel);
        ulArchiveLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ulPanel.add(ulArchivePanel);
        ulArchivePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ulPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ulPanel.add(ulCsvLabel);
        ulCsvLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ulPanel.add(ulCsvPanel);
        ulCsvPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Cancel/Import buttons
        this.cancelButton = new JButton("Cancel");
        this.importButton = new JButton("Import");

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(importButton);

        dialogWindow.add(titleLabel, BorderLayout.NORTH);
        dialogWindow.add(ulPanel, BorderLayout.CENTER);
        dialogWindow.add(buttonPane, BorderLayout.SOUTH);
        // frame.pack(); // Constricts window size with just enough room for components

        // Action Listeners

        ulCoordsButton.addActionListener(this);
        ulArchiveButton.addActionListener(this);
        ulCsvButton.addActionListener(this);
        importButton.addActionListener(this);
        cancelButton.addActionListener(this);

        // Fast Import for Debugging
        if (IS_DEBUG) {

            // FIXME: Turn IS_DEBUG to false or replace these paths with your paths
            autoImport("D:\\DrillExample_NoMergeSubsetCounts.pdf", "D:\\Purdue23-1-1aint_no_mountain_high_enough.3dz");
        } else dialogWindow.setVisible(true);
    }

    private void autoImport(String coordsFilePath, String archiveFilePath) {
        this.coordsFile = new File(coordsFilePath);
        this.archiveFile = new File(archiveFilePath);
        this.importButton.doClick();
    }

    /**
     * Obtain the file extension programmatically in Java
     *
     * @param filename - We want the extension of this file
     * @return A string representing the extension, if the extension exists
     */
    public Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                       .filter(f -> f.contains("."))
                       .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton sourceButton = (JButton) e.getSource();

            // Select coordinates file (.pdf)
            if (sourceButton.equals(ulCoordsButton)) {
                JFileChooser fileChooser = getFileChooser("PDF Documents (*.pdf)", ".pdf");

                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    System.out.println("Coordinates | Selected file: " + selectedFile.getAbsoluteFile());
                    ulCoordsFilename.setText(selectedFile.getName());
                    coordsFile = selectedFile;
                }
            }

            // Select archive file (.3dz)
            else if (sourceButton.equals(ulArchiveButton)) {
                JFileChooser fileChooser = getFileChooser("Pyware Drill Archive (*.3dz)", ".3dz");

                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    System.out.println("Archive     | Selected file: " + selectedFile.getAbsoluteFile());
                    ulArchiveFilename.setText(selectedFile.getName());
                    archiveFile = selectedFile;
                }
            }

            // Select csv file (.csv)
            else if (sourceButton.equals(ulCsvButton)) {
                JFileChooser fileChooser = getFileChooser("Device ID Comma Separated Values (*.csv)", ".csv");

                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    System.out.println("CSV     | Selected file: " + selectedFile.getAbsoluteFile());
                    ulCsvFilename.setText(selectedFile.getName());
                    csvFile = selectedFile;
                }
            }

            // Cancel
            else if (sourceButton.equals(cancelButton)) {
                dialogWindow.dispose();
            }

            // Import selected files
            else if (sourceButton.equals(importButton)) {
                if (coordsFile == null) {
                    JOptionPane.showMessageDialog(dialogWindow,
                                                  "Please select the coordinates (.pdf) file.",
                                                  "Upload Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (archiveFile == null) {
                    JOptionPane.showMessageDialog(dialogWindow,
                                                  "Please select the Pyware archive (.3dz) file.",
                                                  "Upload Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }/*else if (csvFile == null) {
                    JOptionPane.showMessageDialog(dialogWindow,
                                                  "Please select the Comma Separated Values (.csv) file.",
                                                  "Upload Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }*/
                // TODO: add status of import here?
                System.out.println("begin import...");

                // TODO: Import Coordinates Pdf and Pyware Archive

                importListener.onFileSelect(archiveFile, coordsFile, csvFile);
                importArchive.fullImport(archiveFile.getAbsolutePath(), coordsFile.getAbsolutePath());

                dialogWindow.dispose();
            }

        }
    }

    static JFileChooser getFileChooser(String x, String suffix) {
        JFileChooser fileChooser = new JFileChooser();

        // Filter for archive files (e.g., pyware_archive.3dz)
        fileChooser.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String filename = f.getName().toLowerCase();
                    return filename.endsWith(suffix);
                }
            }

            public String getDescription() {
                return x;
            }
        });

        return fileChooser;
    }

    // For testing
//    public static void main(String[] args) {
//
//        // Run Swing programs on the Event Dispatch Thread (EDT)
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                ImportListener importListener = new ImportListener() {
//                    @Override
//                    public void onImport() {
//                        System.out.println("onImport called.");
//                    }
//
//                    @Override
//                    public void onFloorCoverImport(Image image) {
//                        System.out.println("onFloorCoverImport called.");
//                    }
//
//                    @Override
//                    public void onSurfaceImport(Image image) {
//                        System.out.println("onSurfaceImport called.");
//                    }
//
//                    @Override
//                    public void onAudioImport(File audioFile) {
//                        System.out.println("onAudioImport called.");
//                    }
//
//                    @Override
//                    public void onDrillImport(String drill) {
//                        System.out.println("onDrillImport called.");
//                    }
//                };
//
//                SelectFileGUI selectFileGUI = new SelectFileGUI(importListener);
//                selectFileGUI.show();
//            }
//        });
//    }
}