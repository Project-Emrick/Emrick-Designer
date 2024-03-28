package org.emrick.project.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.awt.*;
import java.io.IOException;

// https://www.tutorialspoint.com/gson/gson_custom_adapters.htm
public class ColorAdapter extends TypeAdapter<Color> {
    @Override
    public Color read(JsonReader reader) throws IOException {
        Integer r = null, g = null, b = null, a = null;
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

            if ("r".equals(fieldname)) {
                r = reader.nextInt();
            } else if ("g".equals(fieldname)) {
                g = reader.nextInt();
            } else if ("b".equals(fieldname)) {
                b = reader.nextInt();
            } else if ("a".equals(fieldname)) {
                a = reader.nextInt();
            }
        }
        reader.endObject();

        if (r == null) {
            throw new IOException("failed to get red component for color");
        } else if (g == null) {
            throw new IOException("failed to get green component for color");
        } else if (b == null) {
            throw new IOException("failed to get blue component for color");
        } else if (a == null) {
            throw new IOException("failed to get alpha component for color");
        }

        return new Color(r, g, b, a);
    }

    @Override
    public void write(JsonWriter writer, Color color) throws IOException {
        if (color == null) {
            // color isn't defined so we'll ignore
            writer.nullValue();
            return;
        }

        writer.beginObject();
        writer.name("r");
        writer.value(color.getRed());
        writer.name("g");
        writer.value(color.getGreen());
        writer.name("b");
        writer.value(color.getBlue());
        writer.name("a");
        writer.value(color.getAlpha());
        writer.endObject();
    }
}
