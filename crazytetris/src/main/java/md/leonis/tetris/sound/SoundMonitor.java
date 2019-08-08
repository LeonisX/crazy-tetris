package md.leonis.tetris.sound;

/*
 * WAV звуковые эффекты отрабатываются "родными" средствами Java
 */
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.GameEventListener;
import md.leonis.tetris.engine.model.SoundId;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/*
 * Монитор звуковых каналов
 */
public class SoundMonitor implements GameEventListener {

    private Map<SoundId, SoundChannel> channels = new HashMap<>();

    public void addSound(SoundId soundId, File file) {
        channels.put(soundId, new SoundChannel(file));
    }

    public void addSound(SoundId soundId, InputStream inputStream) {
        channels.put(soundId, new SoundChannel(inputStream));
    }

    public void addSoundWithGain(SoundId soundId, InputStream inputStream, float gain) {
        addSound(soundId, inputStream);
        setGain(soundId, gain);
    }

    public void play(SoundId soundId) {
        channels.get(soundId).play();
    }

    public void loop(SoundId soundId) {
        channels.get(soundId).setLoop(true);
        channels.get(soundId).play();
    }

    public void stop(SoundId soundId) {
        channels.get(soundId).stop();
    }

    public void fade(SoundId soundId) {
        channels.get(soundId).fade();
        channels.get(soundId).stop();
    }

    public void setGain(SoundId soundId, float gain) {
        channels.get(soundId).setGain(gain);
    }

    public void setPan(SoundId soundId, float pan) {
        channels.get(soundId).setPan(pan);
    }

    public boolean isPlaying(SoundId soundId) {
        return channels.get(soundId).isPlaying();
    }

    public boolean isLooping(SoundId soundId) {
        return channels.get(soundId).isLoop();
    }

    public boolean isFading(SoundId soundId) {
        return channels.get(soundId).isFade();
    }

    public void supportLoopingSounds() {
        channels.forEach((key, value) -> {
            if (value.isLoop() && !value.isPlaying()) {
                value.play();
            }
        });
    }

    @Override
    public void notify(GameEvent event, String message) {
        SoundId soundId = message == null ? null : SoundId.valueOf(message);

        switch (event) {
            case PLAY_SOUND:
                play(soundId);
                break;
            case START_LOOPING_SOUND:
                if (!isLooping(soundId)) {
                    loop(soundId);
                }
                break;
            case STOP_LOOPING_SOUND:
                if (isLooping(soundId)) {
                    stop(soundId);
                }
                break;
            case FADE_LOOPING_SOUND:
                if (isLooping(soundId) && !isFading(soundId)) {
                    fade(soundId);
                }
                break;
            case SUPPORT_LOOPING_SOUNDS:
                supportLoopingSounds();
                break;
        }
    }
}
