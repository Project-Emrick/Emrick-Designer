package org.emrick.project;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.*;

public class Unzip {
    public static void unzip(String archiveSrc, String archiveDest) {
        File destDir = new File(archiveDest);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (FileInputStream fis = new FileInputStream(archiveSrc);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(archiveDest + File.separator + fileName);
                new File(newFile.getParent()).mkdirs(); // Ensure parent directories exist

                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                } catch (IOException e) {
                    System.err.println("Error extracting file: " + fileName);
                    e.printStackTrace();
                }

                ze = zis.getNextEntry();
            }

        } catch (IOException e) {
            System.err.println("Error processing zip file: " + archiveSrc);
            e.printStackTrace();
        }
    }

    public static void zip(ArrayList<String> files, String dest, boolean delete) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest));
            for (String file : files) {
                File f = new File(file);
                FileInputStream fis = new FileInputStream(f);
                ZipEntry zipEntry = new ZipEntry(f.getName());
                zos.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                    zos.flush();
                }
                fis.close();
                if (delete) {
                    f.delete();
                }
            }
            zos.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}