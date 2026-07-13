package org.emrick.project;

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

public class Icons {

    private static URL findIcon(String path) {
        URL url = Icons.class.getResource(path);

        if (url == null) {
            throw new IllegalStateException("Missing icon resource: " + path);
        }

        return url;
    }

    public static ImageIcon loadImageIcon(String path) {
        return new ImageIcon(findIcon(path));
    }

    public static Image loadToolkitImage(String path) {
        return loadImageIcon(path).getImage();
    }

}
