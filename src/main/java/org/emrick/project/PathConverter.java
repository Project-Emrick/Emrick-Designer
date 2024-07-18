package org.emrick.project;

public class PathConverter {

    public static String pathConverter(String path) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return System.getProperty("user.home") + "/AppData/Local/Emrick Designer/" + path;
        }
        else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            return "/Desktop/Emrick-Designer/" + path;
        }
        else {
            return "/Applications/Emrick Designer.app/" + path;
        }
    }
}
