import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class DrillParserTest {
    @org.junit.jupiter.api.Test
    public void testExtractText() {
        File input = new File(".\\test\\DrillExample.pdf");
        File exp = new File(".\\test\\ExpectedPDFOutput.txt");
        String actual = DrillParser.extractText(input.getAbsolutePath());
        try {
            FileInputStream fis = new FileInputStream(exp);
            String expected = new String(fis.readAllBytes());
            fis.close();
            assertEquals(expected, actual);

        }
        catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    @org.junit.jupiter.api.Test
    public void testParseDrill() {
        File input = new File(".\\test\\ExpectedPDFOutput.txt");
        File expected = new File(".\\test\\ExpectedParsedDrillOutput.txt");
        try {
            FileInputStream fis = new FileInputStream(input);
            Drill act = DrillParser.parseWholeDrill(new String(fis.readAllBytes()));
            fis.close();
            fis = new FileInputStream(expected);
            String exp = new String(fis.readAllBytes());
            assertEquals(exp,act.toString());
        }
        catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @org.junit.jupiter.api.Test
    public void testSaveAndLoadDrill() {
        File input = new File(".\\test\\ExpectedPDFOutput.txt");
        File perf = new File(".\\test\\testSaveDrillPerformers.json");
        File coord = new File(".\\test\\testSaveDrillCoordinates.json");
        try {
            FileInputStream fis = new FileInputStream(input);
            Drill exp = DrillParser.parseWholeDrill(new String(fis.readAllBytes()));
            fis.close();
            exp.saveDrill(".\\test\\testSaveDrill");
            exp.loadAllPerformers();
            Drill act = DrillParser.importDrill(".\\test\\testSaveDrill");
            for (Performer p : exp.performers) {
                assertTrue(act.performers.contains(p));
            }
            perf.delete();
            coord.delete();
        }
        catch (FileNotFoundException fnfe) {
            perf.delete();
            coord.delete();
            fnfe.printStackTrace();
            fail();
        }
        catch (IOException ioe) {
            perf.delete();
            coord.delete();
            ioe.printStackTrace();
            fail();
        }
        catch (AssertionError ae) {
            perf.delete();
            coord.delete();
            System.out.println(ae);
            fail();
        }
    }
}
