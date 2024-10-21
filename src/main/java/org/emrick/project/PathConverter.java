package org.emrick.project;

public class PathConverter {

    public static String pathConverter(String path, boolean system) {
        if (!system) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                return System.getProperty("user.home") + "/AppData/Local/Emrick Designer/" + path;
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                return System.getProperty("user.home") + "/Desktop/emrick-designer/" + path;
            } else {
                return "/Applications/Emrick Designer.app/Contents/" + path;
            }
        } else {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                return System.getenv("PROGRAMFILES") + "/Emrick Designer/" + path;
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                return "/opt/emrick-designer/lib/" + path;
            }
            else {
                return "/Applications/Emrick Designer.app/Contents/" + path;
            }
        }
    }
}
