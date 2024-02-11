package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SelectFileGUI {

    JFrame frame;

    String coordsFilePath;
    String archiveFilePath;

    public SelectFileGUI() {
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
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Here, "ul" is short for "upload"
        JPanel ulPanel = new JPanel();
        ulPanel.setLayout(new BoxLayout(ulPanel, BoxLayout.PAGE_AXIS));
        ulPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Upload coordinates
        JLabel ulCoordsLabel = new JLabel("Coordinates (.pdf)");
        JButton ulCoordsButton = new JButton("Select File");
        JLabel ulCoordsFilename = new JLabel("No File Selected");

        JPanel ulCoordsPanel = new JPanel();
        ulCoordsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        ulCoordsPanel.add(ulCoordsButton);
        ulCoordsPanel.add(ulCoordsFilename);

        // Upload pyware archive (3dz)
        JLabel ulArchiveLabel = new JLabel("Pyware Archive (.3dz)");
        JButton ulArchiveButton = new JButton("Select File");
        JLabel ulArchiveFilename = new JLabel("No File Selected");

        JPanel ulArchivePanel = new JPanel();
        ulArchivePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        ulArchivePanel.add(ulArchiveButton);
        ulArchivePanel.add(ulArchiveFilename);


        ulPanel.add(ulCoordsLabel);
        ulCoordsLabel.setAlignmentX(Component.LEFT_ALIGNMENT); // Need all components left-aligned
        ulPanel.add(ulCoordsPanel);
        ulCoordsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ulPanel.add(Box.createRigidArea(new Dimension(0,10)));
        ulPanel.add(ulArchiveLabel);
        ulArchiveLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ulPanel.add(ulArchivePanel);
        ulArchivePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelButton = new JButton("Cancel");
        JButton importButton = new JButton("Import");

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
//        frame.pack(); // Constricts window size with just enough room for components

        // Coordinates File
        ulCoordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    System.out.println("Selected file: " + selectedFile.getAbsoluteFile());
                    // TODO: check correct format/type
                    ulCoordsFilename.setText(selectedFile.getName());
                    coordsFilePath = selectedFile.getAbsoluteFile().toString();
//                    System.out.println(coordsFilePath);
                }
            }
        });

        // Pyware Archive (3dz) file
        ulArchiveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    System.out.println("Selected file: " + selectedFile.getAbsoluteFile());
                    // TODO: check correct format/type
                    ulArchiveFilename.setText(selectedFile.getName());
                    archiveFilePath = selectedFile.getAbsoluteFile().toString();
                }
            }
        });

        // Close
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        // Import
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
            }
        });
    }

    public void show() {
        frame.setVisible(true);
    }

    // For testing
    public static void main(String[] args) {

        // Run Swing programs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SelectFileGUI selectFileGUI = new SelectFileGUI();
                selectFileGUI.show();
            }
        });
    }
}
