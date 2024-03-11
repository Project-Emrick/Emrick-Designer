package org.emrick.project.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.emrick.project.SyncTimeGUI;

import java.io.IOException;

public class PairAdapter extends TypeAdapter<SyncTimeGUI.Pair> {
    @Override
    public SyncTimeGUI.Pair read(JsonReader reader) throws IOException {
        String k = null;
        Integer v = null;
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

            if ("k".equals(fieldname)) {
                k = reader.nextString();
            } else if ("v".equals(fieldname)) {
                v = reader.nextInt();
            }
        }
        reader.endObject();

        if (k == null || v == null) {
            throw new IOException("failed to get component for map entry");
        }

        return new SyncTimeGUI.Pair(k, v);
    }

    @Override
    public void write(JsonWriter writer, SyncTimeGUI.Pair entry) throws IOException {
        if (entry == null) {
            // entry isn't defined so we'll ignore
            writer.nullValue();
            return;
        }

        writer.beginObject();
        writer.name("k");
        writer.value(entry.getKey());
        writer.name("v");
        writer.value(entry.getValue());
        writer.endObject();
    }
}
