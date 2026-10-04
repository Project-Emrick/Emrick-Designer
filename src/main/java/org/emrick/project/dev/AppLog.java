package org.emrick.project.dev;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Keeps a copy of everything Emrick Designer prints to System.out / System.err since launch, so the
 * Developer Mode terminal can show it. Output still goes to the console as before. Kept in memory
 * only (bounded), never written to disk unless the user saves it from the terminal.
 */
public final class AppLog {

    /** One captured line of output. */
    public record Entry(long timeMillis, String text, boolean error) {
    }

    private static final int MAX_ENTRIES = 20000;
    private static final Deque<Entry> entries = new ArrayDeque<>();
    private static final List<Consumer<Entry>> listeners = new CopyOnWriteArrayList<>();
    private static boolean installed = false;

    private AppLog() {
    }

    /** Starts capturing. Call once, as early as possible in main(). */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        System.setOut(new PrintStream(new TeeStream(System.out, false), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new TeeStream(System.err, true), true, StandardCharsets.UTF_8));
    }

    /** @return every line captured so far, oldest first */
    public static synchronized List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }

    public static void addListener(Consumer<Entry> listener) {
        listeners.add(listener);
    }

    public static void removeListener(Consumer<Entry> listener) {
        listeners.remove(listener);
    }

    private static void append(String text, boolean error) {
        Entry entry = new Entry(System.currentTimeMillis(), text, error);
        synchronized (AppLog.class) {
            entries.addLast(entry);
            while (entries.size() > MAX_ENTRIES) {
                entries.removeFirst();
            }
        }
        for (Consumer<Entry> listener : listeners) {
            listener.accept(entry);
        }
    }

    /** Passes bytes through to the original stream and collects them into lines for the log. */
    private static class TeeStream extends OutputStream {
        private final PrintStream original;
        private final boolean error;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();

        TeeStream(PrintStream original, boolean error) {
            this.original = original;
            this.error = error;
        }

        @Override
        public synchronized void write(int b) {
            original.write(b);
            collect(b);
        }

        @Override
        public synchronized void write(byte[] buf, int off, int len) {
            original.write(buf, off, len);
            for (int i = off; i < off + len; i++) {
                collect(buf[i]);
            }
        }

        private void collect(int b) {
            if (b == '\n') {
                flushLine();
            } else if (b != '\r') {
                line.write(b);
            }
        }

        @Override
        public void flush() {
            original.flush();
        }

        private void flushLine() {
            String text = line.toString(StandardCharsets.UTF_8);
            line.reset();
            append(text, error);
        }
    }
}
