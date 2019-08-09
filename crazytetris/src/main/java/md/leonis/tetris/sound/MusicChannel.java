package md.leonis.tetris.sound;

import java.io.File;
import java.io.InputStream;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayerException;

// Music Channel Class (MP3)
public class MusicChannel {

    private BasicPlayer player;                         // BasicPlayer player
    private BasicController control;                    // Player control
    private double gain = 0.85;                           // Volume
    private double pan = 0.0;                             // left-right pan (now centered)

    public MusicChannel(InputStream inputStream) {
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
        if ((player.getStatus() == BasicPlayer.PLAYING) || (player.getStatus() == BasicPlayer.PAUSED)) {
            stop();
        }
        try {
            control.play();
            control.setGain(gain); // set only after launch!
            control.setPan(pan); // set only after launch!
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
        return player.getStatus(); // player status
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public void setPan(double pan) {
        this.pan = pan;
    }

    public void resume() { // resume after a pause
        try {
            control.resume();
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }
}
