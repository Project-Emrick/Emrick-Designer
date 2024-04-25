package org.emrick.project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class UserAuthGUI implements ActionListener {
    private JTextField usernameField;
    private JTextField passwordField;
    private JButton submitButton;
    private JButton cancelButton;

    private final JDialog dialogWindow;

    private final UserAuthListener userAuthListener;

    public UserAuthGUI(JFrame parent, UserAuthListener listener) {
        this.userAuthListener = listener;
        dialogWindow = new JDialog(parent, true);
        dialogWindow.setTitle("Account");
        dialogWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialogWindow.setSize(400, 300);
        dialogWindow.setLocationRelativeTo(null);
        dialogWindow.setResizable(false);

        JLabel titleLabel = new JLabel("Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.PAGE_AXIS));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(20);

        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);

        JPanel usernamePanel = new JPanel();
        usernamePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);

        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        passwordPanel.add(passwordLabel);
        passwordPanel.add(passwordField);

        loginPanel.add(usernameLabel);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(usernamePanel);
        usernamePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginPanel.add(passwordLabel);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.add(passwordPanel);
        passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.submitButton = new JButton("Submit");
        this.cancelButton = new JButton("Cancel");

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(submitButton);

        dialogWindow.add(titleLabel, BorderLayout.NORTH);
        dialogWindow.add(loginPanel, BorderLayout.CENTER);
        dialogWindow.add(buttonPane, BorderLayout.SOUTH);

        submitButton.addActionListener(this);
        cancelButton.addActionListener(this);

        dialogWindow.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == submitButton) {
            // TODO: Add login handling here
            System.out.println("Login clicked");
            System.out.println("Username: "+ usernameField.getText() +"\nPassword: "+ passwordField.getText());

            String username = usernameField.getText();
            String password = passwordField.getText();

            // Check if username or password fields are empty
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialogWindow,
                        "Username and password fields cannot be empty.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
                return; // Return early to prevent further processing
            }
            Boolean valids = false;
            String enteredCredentials = "("+username+"."+password+")";
            System.out.println(enteredCredentials);
            try {
                // String credentials = "./src/main/resources/credentials.txt";
                String credentialString = new String(Files.readAllBytes(Paths.get("./src/main/resources/credentials.txt")));
                System.out.println(credentialString);
                valids = credentialString.contains(enteredCredentials);
            } catch (IOException IoE) {
                System.err.println("Error reading the credentials file: " + IoE.getMessage());
            }

            if (valids) {
                System.out.println("SUCCESS!");
                if (userAuthListener != null) {
                    userAuthListener.onUserLoggedIn(username);
                }
                dialogWindow.dispose();
            } else {
                JOptionPane.showMessageDialog(dialogWindow,
                        "Username or Password Incorrect.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == cancelButton) {
            dialogWindow.dispose();
        }
    }

//    public static void main(String[] args) {
//        // Assuming there's a frame to attach to
//        new UserAuthGUI(new JFrame("Parent Frame"));
//    }
}

