package org.emrick.project.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;

public class DurationAdapter extends TypeAdapter<Duration> {

    @Override
    public void write(JsonWriter writer, Duration duration) throws IOException {
        if (duration == null) {
            // color isn't defined so we'll ignore
            writer.nullValue();
            return;
        }

        writer.beginObject();
        writer.name("d");
        writer.value(duration.toMillis());
        writer.endObject();
    }

    @Override
    public Duration read(JsonReader reader) throws IOException {
        Integer d = null;
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

            if ("d".equals(fieldname)) {
                d = reader.nextInt();
            }
        }
        reader.endObject();

        if (d == null) {
            throw new IOException("failed to get d component for duration");
        }

        return Duration.ofSeconds(0).plusMillis(d);
    }
}
