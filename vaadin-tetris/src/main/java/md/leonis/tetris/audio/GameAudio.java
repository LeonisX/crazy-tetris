package md.leonis.tetris.audio;

import com.vaadin.server.Resource;
import org.vaadin.gwtav.GwtAudio;

import java.util.Timer;
import java.util.TimerTask;

// GwtAudio with audio fade possibility
public class GameAudio extends GwtAudio {

    private long fadePeriod = 1000; // in milliseconds
    private int fadeSteps = 5; // how many steps to mute the sound

    private double volumeDecrement;

    private boolean playing = false;

    public GameAudio() {
        super();
    }

    public GameAudio(Resource resource) {
        super(resource);
    }

    public GameAudio(String caption) {
        super(caption);
    }

    public GameAudio(Resource resource, String caption) {
        super(resource, caption);
    }

    public void fade() {
        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                setVolume(getVolume() - volumeDecrement);
                if (getVolume() <= 0 || isMuted()) {
                    cancel();
                    pause();
                }
            }
        };

        volumeDecrement = getVolume() / fadeSteps;
        Timer timer = new Timer("Timer");

        timer.scheduleAtFixedRate(repeatedTask, 0, fadePeriod / fadeSteps);
    }

    @Override
    public void setVolume(double volume) {
        if (getUI() != null) {
            getUI().getSession().getLockInstance().lock();
            try {
                super.setVolume(volume);
            } finally {
                getUI().getSession().getLockInstance().unlock();
            }
        } else {
            super.setVolume(volume);
        }
    }

    @Override
    public void play() {
        getUI().getSession().getLockInstance().lock();
        try {
            super.play();
        } finally {
            getUI().getSession().getLockInstance().unlock();
        }
        playing = true;
    }

    @Override
    public void pause() {
        getUI().getSession().getLockInstance().lock();
        try {
            super.pause();
        } finally {
            getUI().getSession().getLockInstance().unlock();
        }
        playing = false;
    }

    @Override
    public void stop() {
        playing = false;
        if (getUI() != null) {
            getUI().getSession().getLockInstance().lock();
            try {
                super.stop();
            } finally {
                getUI().getSession().getLockInstance().unlock();
            }
        } else {
            super.stop();
        }
    }

    public long getFadePeriod() {
        return fadePeriod;
    }

    public void setFadePeriod(long fadePeriod) {
        this.fadePeriod = fadePeriod;
    }

    public int getFadeSteps() {
        return fadeSteps;
    }

    public void setFadeSteps(int fadeSteps) {
        this.fadeSteps = fadeSteps == 0 ? 1 : fadeSteps;
    }

    public boolean isPlaying() {
        return playing;
    }
}
