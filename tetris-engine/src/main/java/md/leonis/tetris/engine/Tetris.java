package md.leonis.tetris.engine;

import md.leonis.tetris.engine.config.Config;
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.EventManager;
import md.leonis.tetris.engine.model.CritterState;
import md.leonis.tetris.engine.model.GameScore;
import md.leonis.tetris.engine.model.GameState;

import static md.leonis.tetris.engine.event.GameEvent.*;
import static md.leonis.tetris.engine.model.GameState.*;
import static md.leonis.tetris.engine.model.SoundId.*;

public class Tetris extends EventManager implements PropertiesHolder {

    private final Config config;

    private GameScore gameScore;

    private int width, height;

    private volatile Board board;
    private volatile Figure figure, nextFigure;
    private volatile Critter critter;

    private GameState state; // VOID
    private boolean crazy; // false

    private boolean soundOn;

    private boolean initialized;

    public Tetris(Config config, boolean crazy) {
        initializeEvents();
        this.config = config;

        this.crazy = crazy;
        if (crazy) {
            width = config.crazyWidth;
            height = config.crazyHeight;
        } else {
            width = config.standardWidth;
            height = config.standardHeight;
        }

        this.soundOn = config.soundOn;
    }

    public void start() {
        state = VOID;
        gameScore = new GameScore(config.getScoreConfig());

        board = new Board(this);
        if (config.critterEnabled) {
            critter = new Critter(this);
        }

        createNextFigure();
        createFigure();
        NextMove nm = new NextMove();
        NextFrame nf = new NextFrame();
        state = RUNNING;
        if (config.critterEnabled) {
            critter.start();
        }
        nm.start();
        nf.start();
        initialized = true;
    }

    void processEvent(GameEvent event) {
        if ((state == PAUSED || state == FINISHED)) {
            return;
        }
        switch (event) {
            case MOVE_LEFT:
                if (figure.moveLeft() && soundOn) {
                    notify(PLAY_SOUND, CLICK.name()); // click
                }
                break;
            case MOVE_RIGHT:
                if (figure.moveRight() && soundOn) {
                    notify(PLAY_SOUND, CLICK.name());
                }
                break;
            case STEP_DOWN:
                if (figure.moveDown() && soundOn) {
                    notify(PLAY_SOUND, CLICK.name());
                }
                break;
            case ROTATE_RIGHT:
                if (figure.rotateRight() && soundOn) {
                    notify(PLAY_SOUND, ROTATE.name()); // rotate
                }
                break;
            case ROTATE_LEFT:
                if (figure.rotateLeft() && soundOn) {
                    notify(PLAY_SOUND, ROTATE.name()); // rotate
                }
                break;
            case FALL_DOWN:
                figure.fallDown();
                generateNextFigure();
                break;
            case NEXT_LEVEL: // Cheat
                gameScore.levelUp();
                updateStatistics();
                break;
            case MUTE_SOUND:
                soundOn = false;
                break;
            case ENABLE_SOUND:
                soundOn = true;
                break;
            case GAME_OVER:
                state = FINISHED;
                break;
        }
    }

    private void generateNextFigure() {
        if (state == FINISHED) {
            return;
        }
        if (state != VOID) {
            if (critter != null) {
                critter.setPaused(true);
            }
            if (soundOn) {
                notify(PLAY_SOUND, FALLEN.name()); // fallen
            }
            board.mergeFigure(figure);
            board.deleteCompletedRows();
            if (critter != null) {
                critter.correctYPosition(board.getCompletedRows());
                critter.setPaused(false);
            }
            updateStatistics();
        }
        createFigure();
        if (critter != null && critter.isDead() || !figure.isAllowedNewPosition()) {
            finish();
        }
    }

    private void createFigure() {
        figure = nextFigure;
        createNextFigure();
    }

    private void createNextFigure() {
        do {
            nextFigure = new Figure(this);
        } while (figure != null && nextFigure.getColor() == figure.getColor());
    }

    private void updateStatistics() {
        gameScore.countCompletedRows(board.getCompletedRows().size());
        gameScore.countFigure(figure.getType());
        notify(UPDATE_SCORE, gameScore);
    }


    // Figure Lowering Thread - Interval Depends on Game Speed
    class NextMove extends Thread {
        public void run() {
            while (state != FINISHED) {
                if ((state != PAUSED) && figure.isFallen()) {
                    generateNextFigure();
                }
                try {
                    sleep(1001 - gameScore.getLevel() * 100);
                } catch (InterruptedException e) {
                    // empty
                }
                if ((state != PAUSED) && !figure.isFallen()) {
                    figure.moveDown();
                }
            }
        }
    }

    public void pause(boolean isPaused) {
        if (isPaused) {
            state = PAUSED;
        } else {
            state = RUNNING;
        }
        if (critter != null) {
            critter.setPaused(isPaused);
        }
    }

    private void finish() {
        if (critter != null) {
            critter.setStatus(CritterState.DEAD);
            muteCritter();
        }
        notify(GAME_OVER, null);
        state = FINISHED;
    }

    private void muteCritter() {
        if (soundOn) {
            Tetris.this.notify(STOP_LOOPING_SOUND, HEARTBEAT_A.name());
            Tetris.this.notify(STOP_LOOPING_SOUND, HEARTBEAT_B.name());
        }
    }

    // Drawing Thread - Interval 33 ms (30 frames per second), no longer needed
    class NextFrame extends Thread {
        public void run() {
            while (state != FINISHED) {
                try {
                    sleep(config.refreshAfter);
                } catch (InterruptedException e) {
                    // empty
                }
                if (critter != null && soundOn && isInitialized()) {
                    voiceCritter();
                }

                Tetris.this.notify(REPAINT, null);

                if (critter != null && critter.isDead()) {
                    finish();
                }
            }
        }

        private void voiceCritter() {
            Tetris.this.notify(SUPPORT_LOOPING_SOUNDS, null);
            int critterSound = (critter.getAir() < 50) ? 1 : 0;
            if (critter.getAir() < 10) {
                critterSound = 2;
            }
            switch (critterSound) {
                case 0:                //всё хорошо
                    Tetris.this.notify(FADE_LOOPING_SOUND, HEARTBEAT_A.name()); // heartbeat-a
                    Tetris.this.notify(STOP_LOOPING_SOUND, HEARTBEAT_B.name()); // heartbeat-b
                    break;
                case 1:                 //задыхаюсь
                    Tetris.this.notify(STOP_LOOPING_SOUND, HEARTBEAT_B.name());
                    Tetris.this.notify(START_LOOPING_SOUND, HEARTBEAT_A.name());
                    break;
                case 2:                 //скоро конец
                    Tetris.this.notify(STOP_LOOPING_SOUND, HEARTBEAT_A.name());
                    Tetris.this.notify(START_LOOPING_SOUND, HEARTBEAT_B.name());
                    break;
            }
        }
    }

    GameState getState() {
        return state;
    }

    Figure getNextFigure() {
        return nextFigure;
    }

    boolean isInitialized() {
        return initialized;
    }

    @Override
    public int getColorsCount() {
        return config.getColorConfig().colors.length;
    }

    @Override
    public Integer getTransparentColor() {
        return config.getColorConfig().transparentColor;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Board getBoard() {
        return board;
    }

    @Override
    public boolean isCrazy() {
        return crazy;
    }

    @Override
    public Board.Glass getGlass() {
        return board.getGlass();
    }

    @Override
    public Figure getFigure() {
        return figure;
    }

    @Override
    public Critter getCritter() {
        return critter;
    }

    GameScore getGameScore() {
        return gameScore;
    }
}
