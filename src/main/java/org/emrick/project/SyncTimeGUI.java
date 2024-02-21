package org.emrick.project;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncTimeGUI implements ActionListener {

    // String Constants
    private static final String PATH_INSTR_IMAGE = "./src/main/resources/images/sync_time_instr.jpg";
    private static final String INSTRUCTIONS = "<html><p>" +
            "Assuming that your Pyware drill was synced to music, " +
            "in order to play each set at the appropriate rate, " +
            "please enter the corresponding timestamps for each page-tab. " +
            "<br><br>" +
            "For example, in the image above, a Count Track shows Page Tab 1A is placed at count 16 and has timestamp 0:07. " +
            "If this was your Count Track, you would enter 0:07 (exactly as shown) in the field corresponding to Page Tab 1A." +
            "<br><br>" +
            "</p></html>";

    // Page Tab / Count / Times
    private Map<String, Integer> pageTabCounts; // [pageTab]:[count] e.g., k:"2A", v:30 , From ScrubBarGUI
    private Map<String, Integer> pageTabTimes; // [pageTab]:[time] e.g., k:"2A", v:21 , [time] in seconds
    private Map<String, JTextField> pageTabTimeFields;

    private JFrame frame;
    private JButton cancelButton;
    private JButton syncButton;

    public SyncTimeGUI(Map<String, Integer> pageTabCounts) {
        this.pageTabCounts = pageTabCounts;
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Sync Time to Original Drill");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        // A main Panel for padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(mainPanel, BorderLayout.CENTER);

        // Title panel for instructions
        JPanel titlePanel = new JPanel(new BorderLayout(0, 10));
        JLabel titleLabel = new JLabel("Sync Time to Original Drill");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titlePanel.add(titleLabel, BorderLayout.NORTH);

        // Add instruction image
        try {
            BufferedImage instrImage = ImageIO.read(new File(PATH_INSTR_IMAGE));

            int newWidth = frame.getWidth() - 20; // minus 20 for padding
            int newHeight = (int) (float) newWidth * instrImage.getHeight() / instrImage.getWidth();

            Image scaledInstrImage = instrImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            JLabel instrImageLabel = new JLabel(new ImageIcon(scaledInstrImage));
            instrImageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            titlePanel.add(instrImageLabel, BorderLayout.CENTER);
        } catch (IOException e) {
            System.out.println("SyncTimeGUI: initialize() " + e.getMessage());
        }

        JLabel instrLabel = new JLabel(INSTRUCTIONS);
        titlePanel.add(instrLabel, BorderLayout.SOUTH);

        // TODO: Layout Manager fix, stop the fields from shrinking
        // Panel for User to enter page tab times
        JPanel entryPanel = new JPanel(new GridLayout(pageTabCounts.size() + 1, 3, 2, 2));
        entryPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
//        entryPanel.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));

        JScrollPane entryScrollPane = new JScrollPane(entryPanel);
        entryScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        entryPanel.add(new JLabel("Page Tab"));
        entryPanel.add(new JLabel("Timestamp"));
        entryPanel.add(new JLabel("Count"));

        List<Map.Entry<String, Integer>> ptCounts = ScrubBarGUI.sortMap(pageTabCounts);

        pageTabTimeFields = new HashMap<>();

        for (Map.Entry<String, Integer> entry : ptCounts) {
            JLabel setLabel = new JLabel(entry.getKey());
            setLabel.setFont(new Font("Arial", Font.BOLD, 16));
            setLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            entryPanel.add(setLabel); // Set e.g., "2A"

            JTextField textField = new JTextField();
            textField.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            textField.setToolTipText("Enter timestamp of Page Tab " + entry.getKey());
            pageTabTimeFields.put(entry.getKey(), textField); // Keep a reference to text fields
            entryPanel.add(textField);

            JLabel countLabel = new JLabel(entry.getValue().toString());
            countLabel.setFont(new Font("Arial", Font.BOLD, 16));
            countLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            entryPanel.add(countLabel); // Count e.g., "48"
        }

        // Cancel/Import buttons
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        syncButton = new JButton("Sync");
        syncButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(syncButton);

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(entryScrollPane, BorderLayout.CENTER);
        frame.add(buttonPane, BorderLayout.SOUTH);
    }

    public void show() {
        frame.setVisible(true);
    }

    public Map<String, Integer> getPageTabTimes() {
        return this.pageTabTimes;
    }

    public Map<String, Integer> getPageTabCounts() {
        return this.pageTabCounts;
    }

    private void setPageTabTimes() {
        for (Map.Entry<String, JTextField> ptField : pageTabTimeFields.entrySet()) {

            // Key will be the Set String, e.g., "2A". Value is the corresponding JTextField
            // TODO
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cancelButton)) {
            frame.dispose();
        }
        else if (e.getSource().equals(syncButton)) {
            // TODO: Sync Times
        }
    }

    // For testing
    public static void main(String[] args) {

        // Run Swing programs on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                // Dummy input
                Map<String, Integer> dummyData1 = new HashMap<>();
                dummyData1.put("1", 0); // Page tab 1 maps to count 0
                dummyData1.put("1A", 16); // Page tab 1A maps to count 16
                dummyData1.put("2", 32); // Page tab 2 maps to count 32
                dummyData1.put("2A", 48); // etc.
                dummyData1.put("3", 64);
                dummyData1.put("3A", 88);
                dummyData1.put("4", 96);
                dummyData1.put("4A", 112);
                dummyData1.put("4B", 128);
                dummyData1.put("5", 136);
                dummyData1.put("6", 152);
//                dummyData1.put("6A", 168);
//                dummyData1.put("7", 184);
//                dummyData1.put("7A", 200);
//                dummyData1.put("7B", 216);
//                dummyData1.put("8", 228);
//                dummyData1.put("9", 230);
//                dummyData1.put("10", 232);
//                dummyData1.put("11", 234);
//                dummyData1.put("12", 236);
//                dummyData1.put("13", 238);
//                dummyData1.put("14", 240);
//                dummyData1.put("15", 242);
//                dummyData1.put("16", 244); // Lots of data, making sure they fit on GUI

                SyncTimeGUI syncTimeGUI = new SyncTimeGUI(dummyData1);
                syncTimeGUI.show();
            }
        });
    }
}

