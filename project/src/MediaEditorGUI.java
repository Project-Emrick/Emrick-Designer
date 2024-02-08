import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MediaEditorGUI {

    static JLabel sysMsg = new JLabel("");
    static Timer clearSysMsg = new Timer(5000, e -> {
        sysMsg.setText("");
    });

    public static void main(String[] args) {
        // setup sysmsg
        clearSysMsg.setRepeats(false);

        // test autosave stuff
        Timer t = new Timer(1 * 60 * 60 * 1000, e -> {
            System.out.println("autosaving...");
            clearSysMsg.stop();
            sysMsg.setText("Autosaving...");
            clearSysMsg.start();

            // TODO: actual saving here
        });
        t.setRepeats(true);
        t.start();

        // set up main view
        SwingUtilities.invokeLater(MediaEditorGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // Create the main window
        JFrame frame = new JFrame("Main View");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        // Create the top panel with buttons for File and Help
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton fileButton = new JButton("File");
        JButton helpButton = new JButton("Help");
        topPanel.add(fileButton);
        topPanel.add(helpButton);

        // Add action listeners to buttons
        fileButton.addActionListener(e -> showFileOptions(frame));
        helpButton.addActionListener(e -> showHelpOptions(frame));

        // Add top panel to the main frame
        frame.add(topPanel, BorderLayout.NORTH);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton effectButton = new JButton("Effect view");
        rightPanel.add(effectButton);

        frame.add(rightPanel, BorderLayout.EAST);

        frame.add(sysMsg, BorderLayout.SOUTH);

        // Display the window
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showFileOptions(Frame parent) {
        // file button
        JPanel panel = new JPanel(new GridLayout(1, 4)); // 1 row, 4 cols
        String[] labels = {"Import Pyware Object", "Open Emerick Object", "Save Emerick Project", "Export Emerick Packets File"};
        for (String label : labels) {
            JButton button = new JButton(label);
            panel.add(button);
            button.addActionListener((ActionEvent e) -> {
                JOptionPane.showMessageDialog(parent, "You clicked: " + label);
            });
        }
        displayOptionsPanel(parent, panel, "File Options");
    }

    private static void showHelpOptions(Frame parent) {
        // help button
        JPanel panel = new JPanel(new GridLayout(1, 2));
        String[] labels = {"View document(open Github Wiki Page)", "Submit an Issue(open Github Issues page)"};
        for (String label : labels) {
            JButton button = new JButton(label);
            panel.add(button);
            button.addActionListener((ActionEvent e) -> {
                JOptionPane.showMessageDialog(parent, "You clicked: " + label);
            });
        }
        displayOptionsPanel(parent, panel, "Help Options");
    }

    private static void displayOptionsPanel(Frame parent, JPanel panel, String title) {
        // Show the panel in a dialog
        JDialog dialog = new JDialog(parent, title, true);
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}