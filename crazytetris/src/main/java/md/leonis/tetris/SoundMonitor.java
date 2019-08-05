package md.leonis.tetris;

/*
 * WAV звуковые эффекты отрабатываются "родными" средствами Java
 */

import md.leonis.tetris.engine.event.Event;
import md.leonis.tetris.engine.event.GameEventListener;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/*
 * Класс, отвечающий за звуковой канал
 * использует "родную" технологию воспроизведения звука
 * отлично работает с wav файлами.
 */
class AudioChannel {
    private Sound snd;
    private float gain = 0.85f, tmpGain;
    private float pan = 0.0f;
    private boolean loop;
    private boolean fade;
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
        if (pan > 1) {
            pan = 1;
        }
        if (pan < 1) {
            pan = -1;
        }
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

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isFade() {
        return fade;
    }
}

/*
 * Монитор звуковых каналов
 */
public class SoundMonitor implements GameEventListener {
    private List<AudioChannel> channels = new ArrayList<>();

    public void addSound(File file) {
        channels.add(new AudioChannel(file));
    }

    public void addSound(InputStream inputStream) {
        channels.add(new AudioChannel(inputStream));
    }

    public void play(int channel) {
        channels.get(channel).play();
    }

    public void loop(int channel) {
        channels.get(channel).setLoop(true);
        channels.get(channel).play();
    }

    public void stop(int channel) {
        channels.get(channel).stop();
    }

    public void fade(int channel) {
        channels.get(channel).fade();
    }

    public void setGain(int channel, float gain) {
        channels.get(channel).setGain(gain);
    }

    public void setPan(int channel, float pan) {
        channels.get(channel).setPan(pan);
    }

    public boolean isPlaying(int channel) {
        return channels.get(channel).isPlaying();
    }

    public boolean isLooping(int channel) {
        return channels.get(channel).isLoop();
    }

    public boolean isFading(int channel) {
        return channels.get(channel).isFade();
    }

    public void supportLoopingSounds() {
        channels.forEach(channel -> {
            if (channel.isLoop() && !channel.isPlaying()) {
                channel.play();
            }
        });
    }

    @Override
    public void notify(Event event, String message) {

        int channel = (message == null) ? -1 : Integer.parseInt(message);

        switch (event) {
            case PLAY_SOUND:
                play(channel);
                break;
            case START_LOOPING_SOUND:
                if (!isLooping(channel)) {
                    loop(channel);
                }
                break;
            case STOP_LOOPING_SOUND:
                if (isLooping(channel)) {
                    stop(channel);
                }
                break;
            case FADE_LOOPING_SOUND:
                if (isLooping(channel) && !isFading(channel)) {
                    fade(channel);
                }
                break;
            case SUPPORT_LOOPING_SOUNDS:
                supportLoopingSounds();
                break;
        }
    }
}
