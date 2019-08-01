package md.leonis.tetris;

import java.io.File;
import java.io.InputStream;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayerException;

/*
 * Класс, отвечающий за музыкальный канал
 */
public class MusicChannel {
    private BasicPlayer player;                         // проигрыватель BasicPlayer
    private BasicController control;                    // управление проирывателем
    private double gain = 0.85;                           // громкость
    private double pan = 0.0;                             // лево-право (сейчас по центру)

    MusicChannel(InputStream inputStream) {
        player = new BasicPlayer();
        control = player;
        //		player.addBasicPlayerListener(this);
        try {
            control.open(inputStream);
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }

    MusicChannel(File file) {
        player = new BasicPlayer();
        control = player;
        //		player.addBasicPlayerListener(this);
        try {
            control.open(file);
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if ((player.getStatus() == BasicPlayer.PLAYING) || (player.getStatus() == BasicPlayer.PAUSED)) stop();
        try {
            control.play();
            control.setGain(gain);              // устанавливается только после запуска
            control.setPan(pan);                // аналогично
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            control.stop();
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        try {
            control.pause();
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }

    public int state() {
        return player.getStatus();              // состояние проигрывателя
    }

    public void setGain(double g) {
        gain = g;
    }

    public void setPan(double p) {
        pan = p;
    }

    public void resume() {                      // возобновить после паузы
        try {
            control.resume();
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }
}
