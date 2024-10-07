package org.emrick.project.audio;

import org.emrick.project.PathConverter;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;

public class AudioPlayer extends Thread {

    // ! NOTE ! -- In Pyware, audio files can be WAV, MIDI, or OGG
    private File audioFile;
    private Sequencer sequencer; // MIDI - is different from waveform audio: use a Sequencer instead of Clip.
    private Clip clip; // WAV and OGG

    public AudioPlayer(File audioFile) {
        this.audioFile = audioFile;
        initialize();
    }

    public void setAudioFile(File audioFile) {
        this.audioFile = audioFile;
    }

    public void initialize() {
        sequencer = null;
        clip = null;

        initializeAudio();

        // (Optional?) Listener to close the clip when playback is complete -- Might not want this
//        if (clip != null) {
//            clip.addLineListener(event -> {
//                if (event.getType() == LineEvent.Type.STOP) {
//                    clip.close();
//                }
//            });
//        }
    }

    // TODO: Test with WAV and MIDI (not tested)

    private void initializeAudio() {
        if (audioFile.getName().endsWith(".mid") || audioFile.getName().endsWith(".midi")) {
            // MIDI file
            try {
                sequencer = MidiSystem.getSequencer();
                sequencer.setSequence(MidiSystem.getSequence(audioFile));
                sequencer.open();
                // sequencer.start();
            } catch (IOException | MidiUnavailableException | InvalidMidiDataException e) {
                System.err.println("MIDI Audio Error " + e.getMessage());
            }
        }
        else if (audioFile.getName().endsWith(".wav")) {
            // WAV file
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                // clip.start();
            } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
                System.err.println("WAV Audio Error " + e.getMessage());
            }
        }
        else if (audioFile.getName().endsWith(".ogg")) {
            // OGG file
            try (InputStream inputStream = new FileInputStream(audioFile)) {
                OggDecoder oggDecoder = new OggDecoder();
                OggData oggData = oggDecoder.getData(inputStream);

                byte[] audioBytes;
                if (oggData.data.hasArray()) {
                    audioBytes = oggData.data.array();
                } else {
                    audioBytes = new byte[oggData.data.remaining()];
                    oggData.data.get(audioBytes);
                }

                AudioFormat format = new AudioFormat(oggData.rate, 16, oggData.channels, true, false);
                ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
                AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize());

                clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                // clip.start();
            } catch (Exception e) {
                System.err.println("OGG Audio Error " + e.getMessage());
            }
        }
    }

    // Play / Pause Logic

    public void playAudio() {
        if (sequencer != null) {
            sequencer.start();
        }
        else if (clip != null) {
            clip.start();
        }
    }

    public void playAudio(long timestampMillis) {
        System.out.println("AudioPlayer: timestampMillis = " + timestampMillis);
        if (clip != null) {
            try {
                long timestampMicros = timestampMillis * 1000;
                // Set the clip position
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    clip.setMicrosecondPosition(timestampMicros);
                }
                // Start playing the clip from the set position
                clip.start();
            } catch (IllegalArgumentException e) {
                System.err.println("The provided timestamp is out of the clip's range.");
            } catch (Exception e) {
                System.out.println("AudioPlayer: " + e.getMessage());
            }
        } else if (sequencer != null) {
            // TODO: Handle navigation for sequencer, less common
        }
    }

    public void pauseAudio() {
        if (sequencer != null) {
            sequencer.stop();
        }
        else if (clip != null) {
            clip.stop();
        }
    }
    public long getAudioLength() {
        if (sequencer != null) {
            return sequencer.getMicrosecondLength() / 1000;
        }
        else if (clip != null){
            return clip.getMicrosecondLength() / 1000;
        }
        else {
            return 0;
        }
    }

    @Override
    public void run() {
        // Suppress warning "Instantiating a 'AudioPlayer' with default 'run()' method "
    }
}
