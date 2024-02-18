package org.emrick.project;

import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.VorbisFile;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioPlayer extends Thread {

    // ! NOTE ! -- In Pyware, audio files can be WAV, MIDI, or OGG
    private File audioFile;
    private Sequencer sequencer; // Midi
    private Clip clip; // Wav


    public AudioPlayer(File audioFile, boolean loop) {
        this.audioFile = audioFile;
    }

    public void setAudioFile(File audioFile) {
        this.audioFile = audioFile;
    }


    // Play / Pause Logic

    public void play() {
        playWavOrMidi();
        playOgg();
    }

    // TODO: Test with WAV and MIDI (not tested)
    private void playWavOrMidi() {
        try {
            if (audioFile.getName().endsWith(".mid") || audioFile.getName().endsWith(".midi")) {
                // MIDI file
                Sequencer sequencer = MidiSystem.getSequencer();
                sequencer.setSequence(MidiSystem.getSequence(audioFile));
                sequencer.open();
                sequencer.start();
            } else if (audioFile.getName().endsWith("wav")) {
                // WAV file
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | MidiUnavailableException |
                 InvalidMidiDataException e) {
            System.out.println("AudioPlayer > playWavOrMidi()" + e.getMessage());
        }
    }

    // .ogg audio files must be handled by external library
    public void playOgg() {

    }

//    public void exit() {
//        line.stop();
//        line.close();
//    }

    // For Testing
    public static void main(String[] args) {
        String oggPath = "./src/main/resources/unzip/Purdue23-1-1aint_no_mountain_high_enough/Aint No Mountain High Enough.ogg";
        File oggFile = new File(oggPath);
        AudioPlayer audioPlayer = new AudioPlayer(oggFile, true);
        audioPlayer.play();

    }
}
