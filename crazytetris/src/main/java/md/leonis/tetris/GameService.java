package md.leonis.tetris;

import md.leonis.tetris.engine.*;
import md.leonis.tetris.engine.config.Config;
import md.leonis.tetris.engine.event.GameEventListener;
import md.leonis.tetris.sound.MusicChannel;
import md.leonis.tetris.sound.SoundMonitor;

import java.util.Arrays;

import static md.leonis.tetris.FileSystemStorage.getResourceAsStream;
import static md.leonis.tetris.engine.event.GameEvent.*;
import static md.leonis.tetris.engine.model.SoundId.*;

public class GameService extends AbstractGameService implements GameEventListener {

    private final MusicChannel musicChannel;
    private final SoundMonitor soundMonitor;

    GameService(Config config, LanguageProvider languageProvider, StorageInterface storage) {
        setConfig(config);
        setLanguageProvider(languageProvider);
        setStorage(storage);

        musicChannel = new MusicChannel(getResourceAsStream("audio/music.mp3", config.isDebug()));

        soundMonitor = new SoundMonitor();
        soundMonitor.addSoundWithGain(FALLEN, getResourceAsStream("audio/fallen.wav", config.isDebug()), 0.9f);
        soundMonitor.addSoundWithGain(ROTATE, getResourceAsStream("audio/rotate.wav", config.isDebug()), 0.9f);
        soundMonitor.addSoundWithGain(CLICK, getResourceAsStream("audio/click.wav", config.isDebug()), 1.0f);
        soundMonitor.addSoundWithGain(HEARTBEAT_A, getResourceAsStream("audio/heartbeat-a.wav", config.isDebug()), 0.8f);
        soundMonitor.addSoundWithGain(HEARTBEAT_B, getResourceAsStream("audio/heartbeat-b.wav", config.isDebug()), 0.9f);
    }

    @Override
    public void startGame(boolean crazy) {
        setCrazy(crazy);
        setGameRecords(null);
        setTetris(new Tetris(getConfig(), crazy));
        Arrays.asList(REPAINT, GAME_OVER).forEach(e -> getTetris().addListener(e, this));

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
            musicChannel.play();
            processEvent(ENABLE_SOUND);
        }
    }

    @Override
    public void fadeMusic() { // not implemented yet
        stopMusic();
    }

    @Override
    public void stopMusic() {
        if (getConfig().soundOn) {
            musicChannel.stop();
            processEvent(MUTE_SOUND);
        }
    }
}
