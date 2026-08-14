package org.emrick.project;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides file and folder dialogs with direct path entry and per-user remembered locations.
 */
public final class EmrickFileChooser {

    private static final File PREFERENCES_FILE = new File(
            System.getProperty("user.home"), ".emrick-designer-file-chooser.properties");
    private static final Properties LAST_DIRECTORIES = loadLastDirectories();

    private EmrickFileChooser() {
    }

    public static File chooseOpenFile(Component parent, String key, String title,
                                      String description, String extension) {
        return choose(parent, key, title, description, extension, JFileChooser.FILES_ONLY, false);
    }

    public static File chooseSaveFile(Component parent, String key, String title,
                                      String description, String extension) {
        return choose(parent, key, title, description, extension, JFileChooser.FILES_ONLY, true);
    }

    public static File chooseDirectory(Component parent, String key, String title) {
        return choose(parent, key, title, null, null, JFileChooser.DIRECTORIES_ONLY, true);
    }

    private static File choose(Component parent, String key, String title, String description,
                               String extension, int selectionMode, boolean saveDialog) {
        JFileChooser chooser = new AddressBarFileChooser(
                getLastDirectory(key), extension, selectionMode, saveDialog);
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(selectionMode);

        if (extension != null) {
            chooser.setFileFilter(new FileNameExtensionFilter(description, extension.substring(1)));
        }

        int result = saveDialog ? chooser.showSaveDialog(parent) : chooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selectedFile = normalizeExtension(chooser.getSelectedFile(), extension, saveDialog);
        rememberDirectory(key, selectionMode == JFileChooser.DIRECTORIES_ONLY
                ? selectedFile
                : selectedFile.getParentFile());
        return selectedFile;
    }

    private static final class AddressBarFileChooser extends JFileChooser {
        private final JTextField addressBar = new JTextField();
        private final String extension;
        private final int selectionMode;
        private final boolean saveDialog;

        private AddressBarFileChooser(File initialDirectory, String extension,
                                      int selectionMode, boolean saveDialog) {
            super(initialDirectory);
            this.extension = extension;
            this.selectionMode = selectionMode;
            this.saveDialog = saveDialog;
            addressBar.setText(initialDirectory.getAbsolutePath());
            addressBar.addActionListener(event -> navigateToAddress());
            addPropertyChangeListener(DIRECTORY_CHANGED_PROPERTY, event -> {
                File currentDirectory = getCurrentDirectory();
                if (currentDirectory != null) {
                    addressBar.setText(currentDirectory.getAbsolutePath());
                }
            });
        }

        @Override
        protected JDialog createDialog(Component parent) {
            JDialog dialog = super.createDialog(parent);
            JPanel addressPanel = new JPanel(new BorderLayout(6, 0));
            addressPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
            addressPanel.add(new JLabel("Location:"), BorderLayout.WEST);
            addressPanel.add(addressBar, BorderLayout.CENTER);
            JButton goButton = new JButton("Go");
            goButton.addActionListener(event -> navigateToAddress());
            addressPanel.add(goButton, BorderLayout.EAST);
            dialog.getContentPane().add(addressPanel, BorderLayout.NORTH);
            dialog.pack();
            return dialog;
        }

        private void navigateToAddress() {
            File enteredPath = new File(stripSurroundingQuotes(addressBar.getText()));
            if (enteredPath.isDirectory()) {
                setCurrentDirectory(enteredPath);
                if (selectionMode == JFileChooser.DIRECTORIES_ONLY) {
                    setSelectedFile(enteredPath);
                }
                return;
            }

            File selectedFile = normalizeExtension(enteredPath, extension, saveDialog);
            File parentDirectory = selectedFile.getParentFile();
            boolean validFile = saveDialog
                    ? parentDirectory != null && parentDirectory.isDirectory() && parentDirectory.canWrite()
                    : selectedFile.isFile() && selectedFile.canRead();
            boolean acceptedExtension = extension == null
                    || selectedFile.getName().toLowerCase().endsWith(extension.toLowerCase());

            if (selectionMode == JFileChooser.FILES_ONLY && validFile && acceptedExtension) {
                setCurrentDirectory(parentDirectory);
                setSelectedFile(selectedFile);
                return;
            }

            String message = selectionMode == JFileChooser.DIRECTORIES_ONLY
                    ? "Enter an existing folder path."
                    : saveDialog
                    ? "Enter an existing folder path or a file path inside an existing writable folder."
                    : "Enter an existing folder path or an existing readable file path.";
            JOptionPane.showMessageDialog(this, message, "Invalid Location", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static File normalizeExtension(File file, String extension, boolean saveDialog) {
        if (!saveDialog || extension == null || extension.isEmpty()) {
            return file;
        }
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(extension.toLowerCase())
                ? file
                : new File(path + extension);
    }

    private static String stripSurroundingQuotes(String path) {
        String trimmedPath = path == null ? "" : path.trim();
        if (trimmedPath.length() >= 2 && trimmedPath.startsWith("\"") && trimmedPath.endsWith("\"")) {
            return trimmedPath.substring(1, trimmedPath.length() - 1);
        }
        return trimmedPath;
    }

    private static File getLastDirectory(String key) {
        String savedPath = LAST_DIRECTORIES.getProperty(key);
        File savedDirectory = savedPath == null ? null : new File(savedPath);
        return savedDirectory != null && savedDirectory.isDirectory()
                ? savedDirectory
                : new File(System.getProperty("user.home"));
    }

    private static synchronized void rememberDirectory(String key, File directory) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        LAST_DIRECTORIES.setProperty(key, directory.getAbsolutePath());
        try (FileOutputStream output = new FileOutputStream(PREFERENCES_FILE)) {
            LAST_DIRECTORIES.store(output, "Emrick Designer file chooser locations");
        } catch (IOException ignored) {
        }
    }

    private static Properties loadLastDirectories() {
        Properties directories = new Properties();
        if (!PREFERENCES_FILE.isFile()) {
            return directories;
        }
        try (FileInputStream input = new FileInputStream(PREFERENCES_FILE)) {
            directories.load(input);
        } catch (IOException ignored) {
        }
        return directories;
    }
}
