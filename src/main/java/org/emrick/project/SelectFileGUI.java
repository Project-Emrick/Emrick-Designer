package org.emrick.project;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Optional;

/**
 * This class constructs a Swing GUI window for importing Coordinates PDF and Pyware Archive file (.3dz)
 * when starting new projects. Coordinates PDF provides necessary performer data, and Pyware Archive (.3dz)
 * provides additional Pyware drill components (e.g., floorCover, ground, surface, audio).
 */
public class SelectFileGUI implements ActionListener {

    // Parent frame
    private JFrame frame;

    // Paths to selected files
    private String coordsFilePath;
    private String archiveFilePath;

    // Upload File Buttons
    private JButton ulCoordsButton;
    private JButton ulArchiveButton;

    // Filename Display Labels
    private JLabel ulCoordsFilename;
    private JLabel ulArchiveFilename;

    // Import / Cancel Buttons
    private JButton cancelButton;
    private JButton importButton;

    // Import Archive Service
    private ImportArchive importArchive;

    /**
     * Prepare ImportArchive service object.
     * @param importListener Passed down from a class that overrides ImportListener methods.
     *                       Provides callback functionality (e.g., to repaint field after importing
     *                       floorCover or surface images).
     */
    public SelectFileGUI(ImportListener importListener) {
        importArchive = new ImportArchive(importListener);

        coordsFilePath = null;
        archiveFilePath = null;

        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("New Project - Import");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null); // center on screen
        frame.setResizable(false); // resize window option

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

        // Build UI structure
        ulPanel.add(ulCoordsLabel);
        ulCoordsLabel.setAlignmentX(Component.LEFT_ALIGNMENT); // Need all components left-aligned
        ulPanel.add(ulCoordsPanel);
        ulCoordsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ulPanel.add(Box.createRigidArea(new Dimension(0,10)));
        ulPanel.add(ulArchiveLabel);
        ulArchiveLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ulPanel.add(ulArchivePanel);
        ulArchivePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        frame.add(titleLabel, BorderLayout.NORTH);
        frame.add(ulPanel, BorderLayout.CENTER);
        frame.add(buttonPane, BorderLayout.SOUTH);
        // frame.pack(); // Constricts window size with just enough room for components


        // Action Listeners

        ulCoordsButton.addActionListener(this);
        ulArchiveButton.addActionListener(this);
        importButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    public void show() {
        frame.setVisible(true);
    }

    /**
     * Obtain the file extension programmatically in Java
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
                    coordsFilePath = selectedFile.getAbsoluteFile().toString();
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
                    archiveFilePath = selectedFile.getAbsolutePath();
                }
            }

            // Cancel
            else if (sourceButton.equals(cancelButton)) {
                frame.dispose();
            }

            // Import selected files
            else if (sourceButton.equals(importButton)) {
                if (coordsFilePath == null) {
                    JOptionPane.showMessageDialog(frame,
                            "Please select the coordinates (.pdf) file.",
                            "Upload Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                else if (archiveFilePath == null) {
                    JOptionPane.showMessageDialog(frame,
                            "Please select the Pyware archive (.3dz) file.",
                            "Upload Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                System.out.println("begin import...");

                // TODO: Import Coordinates Pdf and Pyware Archive

                importArchive.fullImport(archiveFilePath, coordsFilePath);

                frame.dispose();
            }

        }
    }

    private static JFileChooser getFileChooser(String x, String suffix) {
        JFileChooser fileChooser = new JFileChooser();

        // Filter for archive files (e.g., pyware_archive.3dz)
        fileChooser.setFileFilter(new FileFilter() {

            public String getDescription() {
                return x;
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String filename = f.getName().toLowerCase();
                    return filename.endsWith(suffix);
                }
            }
        });

        return fileChooser;
    }

    // For testing
    public static void main(String[] args) {

        // Run Swing programs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ImportListener importListener = new ImportListener() {
                    @Override
                    public void onImport() {
                        System.out.println("onImport called.");
                    }

                    @Override
                    public void onFloorCoverImport(Image image) {
                        System.out.println("onFloorCoverImport called.");
                    }

                    @Override
                    public void onSurfaceImport(Image image) {
                        System.out.println("onSurfaceImport called.");
                    }

                    @Override
                    public void onAudioImport(File audioFile) {
                        System.out.println("onAudioImport called.");
                    }

                    @Override
                    public void onDrillImport(String drill) {
                        System.out.println("onDrillImport called.");
                    }
                };

                SelectFileGUI selectFileGUI = new SelectFileGUI(importListener);
                selectFileGUI.show();
            }
        });
    }
}
