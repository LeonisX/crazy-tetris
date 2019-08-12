package md.leonis.tetris;

import com.vaadin.server.ThemeResource;
import md.leonis.tetris.audio.GameAudio;
import md.leonis.tetris.audio.SoundMonitor;
import md.leonis.tetris.engine.*;
import md.leonis.tetris.engine.config.Config;
import md.leonis.tetris.engine.event.GameEventListener;

import java.util.Arrays;

import static md.leonis.tetris.engine.event.GameEvent.*;
import static md.leonis.tetris.engine.model.SoundId.*;

public class GameService extends AbstractGameService implements GameEventListener {

    private static final double MUSIC_VOLUME = 0.1;

    private final GameAudio music;
    private double musicVolume;

    private final SoundMonitor soundMonitor;

    GameService(Config config) {
        setConfig(config);
        setLanguageProvider(new LanguageProvider());
        setStorage(new InMemoryStorage());

        musicVolume = MUSIC_VOLUME;

        music = new GameAudio(new ThemeResource("audio/music.mp3"));
        music.setShowControls(false);
        music.setVolume(musicVolume);
        music.setLoop(true);

        soundMonitor = new SoundMonitor();            // создаём монитор звуковых эффектов
        soundMonitor.addSoundWithGain(FALLEN, new GameAudio(new ThemeResource("audio/fallen.wav")), 0.9, false);
        soundMonitor.addSoundWithGain(ROTATE, new GameAudio(new ThemeResource("audio/rotate.wav")), 0.9, false);
        soundMonitor.addSoundWithGain(CLICK, new GameAudio(new ThemeResource("audio/click.wav")), 1.0, false);
        soundMonitor.addSoundWithGain(HEARTBEAT_A, new GameAudio(new ThemeResource("audio/heartbeat-a.wav")), 0.8, true);
        soundMonitor.addSoundWithGain(HEARTBEAT_B, new GameAudio(new ThemeResource("audio/heartbeat-b.wav")), 0.9, true);
    }

    @Override
    public void startGame(boolean crazy) {
        setCrazy(crazy);
        setGameRecords(null);
        setTetris(new Tetris(getConfig(), crazy));
        Arrays.asList(REPAINT, UPDATE_SCORE, GAME_OVER).forEach(e -> getTetris().addListener(e, this));

        Arrays.asList(PLAY_SOUND, START_LOOPING_SOUND, STOP_LOOPING_SOUND, FADE_LOOPING_SOUND, SUPPORT_LOOPING_SOUNDS)
                .forEach(e -> getTetris().addListener(e, soundMonitor));

        getTetris().start();
    }

    @Override
    public Records initializeRecords() {
        String fileName = isCrazy() ? "crazy.res" : "tet.res";
        getStorage().setRecordsStorageName(fileName);
        setGameRecords(new Records(getStorage(), getGameScore().getScore()));
        return getGameRecords();
    }

    @Override
    public void playMusic() {
        if (getConfig().soundOn) {
            music.setVolume(musicVolume);
            music.play();
            processEvent(ENABLE_SOUND);
        }
    }

    @Override
    public void fadeMusic() {
        if (getConfig().soundOn) {
            musicVolume = music.getVolume();
            music.fade();
            processEvent(MUTE_SOUND);
        }
    }

    @Override
    public void stopMusic() {
        if (getConfig().soundOn) {
            music.stop();
            processEvent(MUTE_SOUND);
        }
    }

    GameAudio getMusic() {
        return music;
    }

    SoundMonitor getSoundMonitor() {
        return soundMonitor;
    }
}
