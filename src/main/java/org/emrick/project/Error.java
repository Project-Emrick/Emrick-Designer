package org.emrick.project;

import java.util.Map;
import java.util.TreeMap;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import javax.swing.JOptionPane;

/**
 * Class for handling errors within Emrick Designer
 *
 * @version Mar 23, 2025
 */
public class Error {

    // severity codes
    public static final int INFORMATION = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    // list of all possible errors (imported from file)
    private static Map<Integer, Error> errors = new TreeMap<>();

    private int code;
    private int severity;

    private String category;
    private String message;

    /**
     * Loads all possible errors for Emrick Designer from a .csv file.
     * The given file should contain comma-separated values for error
     * code, category, severity, and message. Populates the errors
     * tree map with the provided information.
     *
     * @param filename name of file to read from
     */
    public static void load(String filename) {
        try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
            String line = in.readLine();
            int lineNumber = 1;

            while (line != null) {
                // skip the first line (column headings)
                if (lineNumber == 1) {
                    line = in.readLine();
                    lineNumber++;
                    continue;
                }

                try {
                    String[] args = line.split(",");

                    int code = Integer.parseInt(args[0]);
                    int severity = switch (args[1]) {
                        case "Information" -> Error.INFORMATION;
                        case "Warning" -> Error.WARNING;
                        case "Error" -> Error.ERROR;
                        default -> throw new Exception("Invalid severity label.");
                    };

                    String category = args[2];
                    String message = args[3];

                    errors.put(code, new Error(code, severity, category, message));
                } catch (Exception e) {
                    System.out.print("Could not read line " + lineNumber + ": ");
                    System.out.println(e.getMessage());
                }

                line = in.readLine();
                lineNumber++;
            }
        } catch (IOException e) {
            System.out.println("There was a problem opening the error file.");
        }
    }

    /**
     * Notifies the user that an error with the given error code has
     * occurred using the message dialog. The message dialog contains
     * the error code, category, and message/summary.
     *
     * @param errorCode 3-digit code representing the error
     */
    public static void alert(int errorCode) {
        Error e = errors.get(errorCode);

        // calling an invalid error code or
        // error file was not read in properly
        if (e == null) {
            System.out.println("No such error!");
            return;
        }

        String dialogTitle = String.format("Error Code %d", e.code);
        String dialogMessage = String.format("Category: %s\n%s", e.category, e.message);

        int icon = switch (e.severity) {
            case Error.WARNING -> JOptionPane.WARNING_MESSAGE;
            case Error.ERROR -> JOptionPane.ERROR_MESSAGE;
            default -> JOptionPane.INFORMATION_MESSAGE;
        };

        JOptionPane.showMessageDialog(null, dialogMessage, dialogTitle, icon);
    }

    /**
     * Constructor for Error
     *
     * @param code 3-digit code representing the error
     * @param severity 0=INFORMATION;
     *                 1=WARNING;
     *                 2=ERROR
     * @param category file I/O, hardware, user error, etc.
     * @param message text to show to user when error occurs
     */
    public Error(int code, int severity, String category, String message) {
        this.code = code;
        this.severity = severity;
        this.category = category;
        this.message = message;
    }

}
