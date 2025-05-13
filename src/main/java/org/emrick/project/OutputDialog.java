package org.emrick.project;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class OutputDialog extends JDialog {
    private final JTextArea textArea;
    private final JButton closeButton;

    public OutputDialog() {
        setTitle("Upload Progress");
        setSize(600, 400);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        closeButton = new JButton("Close");
        closeButton.setEnabled(false);
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void appendLine(String line) {
        SwingUtilities.invokeLater(() -> textArea.append(line + "\n"));
    }

    public void enableCloseButton() {
        SwingUtilities.invokeLater(() -> closeButton.setEnabled(true));
    }
}