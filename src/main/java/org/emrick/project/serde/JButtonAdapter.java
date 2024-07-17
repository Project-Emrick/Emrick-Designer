package org.emrick.project.serde;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class JButtonAdapter extends TypeAdapter<JButton> {
    @Override
    public JButton read(JsonReader reader) throws IOException {
        reader.nextNull();
        return null;
    }

    @Override
    public void write(JsonWriter writer, JButton jbutton) throws IOException {
        if (jbutton == null) {
            // color isn't defined so we'll ignore
            writer.nullValue();
            return;
        }
    }
}

