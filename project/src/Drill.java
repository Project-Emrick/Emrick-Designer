import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class Drill {
    public ArrayList<Performer> performers;
    public ArrayList<Coordinate> coordinates;
    public Drill() {
        performers = new ArrayList<>();
        coordinates = new ArrayList<>();
    }

    public void saveDrill(String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename + "Performers.json");
            deconstructPerformers();
            Type perfType = new TypeToken<ArrayList<Performer>>(){}.getType();
            fos.write(new Gson().toJson(performers, perfType).getBytes());
            fos.flush();
            fos.close();
            fos = new FileOutputStream(filename + "Coordinates.json");
            Type coordType = new TypeToken<ArrayList<Coordinate>>(){}.getType();
            fos.write(new Gson().toJson(coordinates, coordType).getBytes());
            fos.flush();
            fos.close();
        }
        catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void deconstructPerformers() {
        for (Performer p : performers) {
            p.deconstructCoordinates();
        }
    }

    public void loadAllPerformers() {
        for (Performer p : performers) {
            p.loadCoordinates(coordinates);
        }
    }

    public void addSet(Coordinate coordinate) {
        coordinates.add(coordinate);
    }

    public void addPerformer(Performer performer) {
        performers.add(performer);
    }

    public String toString() {
        String out = "";
        for (Performer p : performers) {
            out += p;
        }
        return out;
    }
}
