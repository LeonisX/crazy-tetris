package md.leonis.tetris.audio;

import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.GameEventListener;
import md.leonis.tetris.engine.model.SoundId;

import java.util.HashMap;
import java.util.Map;

/*
 * Sound channels monitor
 */
public class SoundMonitor implements GameEventListener {

    private Map<SoundId, GameAudio> channels = new HashMap<>();

    public void addSoundWithGain(SoundId soundId, GameAudio audio, double volume, boolean loop) {
        channels.put(soundId, audio);
        audio.setShowControls(false);
        audio.setVolume(volume);
        audio.setLoop(loop);
        audio.stop();
    }

    public void play(SoundId soundId) {
        channels.get(soundId).play();
    }

    public void loop(SoundId soundId) {
        channels.get(soundId).setLoop(true);
        //channels.get(soundId).play();
    }

    public void stop(SoundId soundId) {
        channels.get(soundId).stop();
    }

    public void fade(SoundId soundId) {
        channels.get(soundId).fade();
    }

    public void setVolume(SoundId soundId, double volume) {
        channels.get(soundId).setVolume(volume);
    }

    public boolean isLooping(SoundId soundId) {
        return channels.get(soundId).isLoop();
    }

    private boolean isPlaying(SoundId soundId) {
        return channels.get(soundId).isPlaying();
    }

    @Override
    public void notify(GameEvent event, String message) {
        SoundId soundId = message == null ? null : SoundId.valueOf(message);

        switch (event) {
            case PLAY_SOUND:
                play(soundId);
                break;
            case START_LOOPING_SOUND:
                if (!isPlaying(soundId)) {
                    setVolume(soundId, 0.8);
                    play(soundId);
                }
                break;
            case STOP_LOOPING_SOUND:
                if (isLooping(soundId) && isPlaying(soundId)) {
                    stop(soundId);
                }
                break;
            case FADE_LOOPING_SOUND:
                if (isLooping(soundId) && isPlaying(soundId)) {
                    fade(soundId);
                }
                break;
        }
    }

    public Map<SoundId, GameAudio> getChannels() {
        return channels;
    }
}
