package md.leonis.tetris;

/*
 * WAV звуковые эффекты отрабатываются "родными" средствами Java
 */

import java.io.File;
import java.io.InputStream;

/*
 * Класс, отвечающий за звуковой канал
 * использует "родную" технологию воспроизведения звука
 * отлично работает с wav файлами.
 */
class AudioChannel {
    private Sound snd;
    private float gain = 0.85f, tmpGain;
    private float pan = 0.0f;
    boolean loop;
    boolean fade;
    private int fadeCount = 5;
    private float fadeGrade;

    AudioChannel(File file) {
        snd = new Sound(file);
        loop = false;
        fade = false;
    }

    public AudioChannel(InputStream inputStream) {
        snd = new Sound(inputStream);
        loop = false;
        fade = false;
    }

    public void play() {
        if (fade) {
            gain -= fadeGrade;
            if (gain < 0) {
                fade = false;
                gain = 0;
                loop = false;
                gain = tmpGain;
                return;
            }
        }
        snd.setVolume(gain);
        snd.setBalance(pan);
        snd.play();
    }

    public void setGain(float g) {
        gain = g;
    }

    public void setPan(float p) {
        pan = p;
        if (pan > 1) pan = 1;
        if (pan < 1) pan = -1;
    }

    public void stop() {
        loop = false;
        snd.stop();
    }

    public void fade() {
        fade = true;
        tmpGain = gain;
        fadeGrade = gain / fadeCount;
        snd.stop();
    }

    public boolean isPlaying() {
        return snd.isPlaying();
    }
}

/*
 * Монитор звуковых каналов
 */
public class SoundMonitor {
    AudioChannel[] ac = new AudioChannel[6];        // реализуем 6 каналов
    private int channelsCount = 0;

    public void addSound(File file) {
        ac[channelsCount] = new AudioChannel(file);
        channelsCount++;
    }

    public void addSound(InputStream inputStream) {
        ac[channelsCount] = new AudioChannel(inputStream);
        channelsCount++;
    }

    public void play(int k) {
        ac[k].play();
    }

    public void loop(int k) {
        ac[k].loop = true;
        ac[k].play();
    }

    public void stop(int k) {
        ac[k].stop();
    }

    public void fade(int k) {
        ac[k].fade();
    }

    public void setGain(int k, float g) {
        ac[k].setGain(g);
    }

    public void setPan(int k, float p) {
        ac[k].setPan(p);
    }

    public boolean isPlaying(int k) {
        return ac[k].isPlaying();
    }

    public boolean isLooping(int k) {
        return ac[k].loop;
    }

    public void schedule() {
        for (int i = 0; i < channelsCount; i++)
            if (ac[i].loop) if (!ac[i].isPlaying()) ac[i].play();
    }
}
