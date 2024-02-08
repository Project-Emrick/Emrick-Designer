import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class DrillParser {
    public static String extractText(String fileName) {
        File f = new File(fileName);
        String text = "";
        try {
            PDDocument document = Loader.loadPDF(f);
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
        return text;
    }
}
