package md.leonis.tetris.sound;

/*
 * WAV звуковые эффекты отрабатываются "родными" средствами Java
 */
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.GameEventListener;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    public void notify(GameEvent event, String message) {

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
