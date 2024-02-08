import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DrillParserTest {
    @org.junit.jupiter.api.Test
    public void testExtractText() {
        File input = new File(".\\test\\DrillExample.pdf");
        File exp = new File(".\\test\\ExpectedPDFOutput.txt");
        String actual = DrillParser.extractText(input.getAbsolutePath());
        try {
            FileInputStream fis = new FileInputStream(exp);
            String expected = new String(fis.readAllBytes());
            assertEquals(expected, actual);
        }
        catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
}
