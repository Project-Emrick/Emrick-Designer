package org.emrick.project.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.awt.geom.Point2D;
import java.io.IOException;

public class Point2DAdapter extends TypeAdapter<Point2D> {

    @Override
    public Point2D read(JsonReader reader) throws IOException {
        Double x = null, y = null;
        String fieldname = null;
        if (reader.peek().equals(JsonToken.NULL)) {
            reader.nextNull();
            return null;
        }

        reader.beginObject();
        while (reader.hasNext()) {
            JsonToken token = reader.peek();

            if (token.equals(JsonToken.NAME)) {
                fieldname = reader.nextName();
            }

            if ("x".equals(fieldname)) {
                x = reader.nextDouble();
            } else if ("y".equals(fieldname)) {
                y = reader.nextDouble();
            }
        }
        reader.endObject();

        if (x == null || y == null) {
            throw new IOException("failed to get component for point");
        }

        return new Point2D.Double(x, y);
    }

    @Override
    public void write(JsonWriter writer, Point2D point2D) throws IOException {
        if (point2D == null) {
            // color isn't defined so we'll ignore
            writer.nullValue();
            return;
        }

        writer.beginObject();
        writer.name("x");
        writer.value(point2D.getX());
        writer.name("y");
        writer.value(point2D.getY());
        writer.endObject();
    }
}
