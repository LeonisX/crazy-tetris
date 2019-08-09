package md.leonis.tetris.sound;

/*
 * http://habrahabr.ru/post/191422/
 * 26 august 2013. 20:36
 * A few words about the support of audio file formats: forget about MP3 and remember WAV. AU and AIF are also supported.
 * (C) raid
 *
 * This library plays standard Java sounds. Actual for Java SE 7/8
 * 06.03.2015 modification and revision
 * (C) Leonis
 */
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioFormat;

class Sound {

    private boolean released;
    private Clip clip = null;
    private FloatControl volumeC = null;
    private boolean playing = false;

    Sound(File file) {
        released = false;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(
                    Clip.class,
                    stream.getFormat(),
                    ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.addLineListener(new Listener());
            volumeC = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            released = true;

        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException exc) {
            exc.printStackTrace();
            released = false;
        }
    }

    Sound(InputStream inputStream) {
        released = false;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(
                    Clip.class,
                    stream.getFormat(),
                    ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.addLineListener(new Listener());
            volumeC = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            released = true;

        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException exc) {
            exc.printStackTrace();
            released = false;
        }
    }

    // true if the sound was loaded successfully, false if an error occurred
    boolean isReleased() {
        return released;
    }

    // is the sound currently playing
    boolean isPlaying() {
        return playing;
    }

    /*
     * breakOld determines the behavior if the sound is already playing
     * If breakOld==true, o sound will be interrupted and restarted
     * Otherwise nothing will happen.
     */
    private void play(boolean breakOld) {
        if (released) {
            if (breakOld) {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            } else if (!isPlaying()) {
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            }
        }
    }

    // Same as play(true)
    void play() {
        play(true);
    }

    // Stops playback
    void stop() {
        if (playing) {
            clip.stop();
        }
    }

    // Volume setting: volume must be between 0 and 1 (from the quietest to the loudest)
    void setVolume(float volume) {
        if (volume < 0) {
            volume = 0;
        }
        if (volume > 1) {
            volume = 1;
        }
        float min = volumeC.getMinimum();
        float max = volumeC.getMaximum();
        volumeC.setValue((max - min) * volume + min);
    }

    // Returns the current volume (a number from 0 to 1)
    float getVolume() {
        float volume = volumeC.getValue();
        float min = volumeC.getMinimum();
        float max = volumeC.getMaximum();
        return (volume - min) / (max - min);
    }

    /**
     * Sound Positioning: Center (0), Left (-1.0) or Right (1.0)
     * If this function is not supported, nothing will happen.
     */
    void setBalance(float pan) {
        if (clip.isControlSupported(FloatControl.Type.PAN)) // stereo sound is needed, OTHERWISE DOES NOT WORK !!!
            try {
                FloatControl panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
                panControl.setValue(pan);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
    }

    // Waiting for the sound to finish
    void join() {
        if (!released) return;
        synchronized (clip) {
            try {
                while (playing) {
                    clip.wait();
                }
            } catch (InterruptedException exc) {
                //TODO
            }
        }
    }

    // Static method, for convenience
    static Sound playSound(String s) {
        File f = new File(s);
        Sound snd = new Sound(f);
        snd.play();
        return snd;
    }

    private class Listener implements LineListener {
        public void update(LineEvent ev) {
            if (ev.getType() == LineEvent.Type.STOP) {
                playing = false;
                synchronized (clip) {
                    clip.notify();
                }
            }
        }
    }
}
