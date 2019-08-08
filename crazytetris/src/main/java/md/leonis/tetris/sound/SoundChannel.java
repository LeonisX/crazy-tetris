package md.leonis.tetris.sound;

import java.io.File;
import java.io.InputStream;

/*
 * Класс, отвечающий за звуковой канал использует "родную" технологию воспроизведения звука
 * Отлично работает с wav файлами.
 * All Sound FX.
 */
class SoundChannel {

    private static final int FADE_COUNT = 5;

    private Sound sound;
    private float gain = 0.85f, tmpGain;
    private float pan = 0.0f;
    private boolean loop;
    private boolean fade;

    private float fadeGrade;

    SoundChannel(File file) {
        sound = new Sound(file);
        loop = false;
        fade = false;
    }

    SoundChannel(InputStream inputStream) {
        sound = new Sound(inputStream);
        loop = false;
        fade = false;
    }

    void play() {
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
        sound.setVolume(gain);
        sound.setBalance(pan);
        sound.play();
    }

    void setGain(float g) {
        gain = g;
    }

    void setPan(float p) {
        pan = p;
        if (pan > 1) {
            pan = 1;
        }
        if (pan < 1) {
            pan = -1;
        }
    }

    void stop() {
        loop = false;
        sound.stop();
    }

    void fade() {
        fade = true;
        tmpGain = gain;
        fadeGrade = gain / FADE_COUNT;
        sound.stop();
    }

    boolean isPlaying() {
        return sound.isPlaying();
    }

    boolean isLoop() {
        return loop;
    }

    void setLoop(boolean loop) {
        this.loop = loop;
    }

    boolean isFade() {
        return fade;
    }
}