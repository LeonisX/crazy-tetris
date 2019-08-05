package md.leonis.tetris.engine;

import md.leonis.tetris.engine.event.EventManager;
import md.leonis.tetris.engine.model.CritterState;
import md.leonis.tetris.engine.model.GameState;

import java.awt.event.KeyEvent;

import static md.leonis.tetris.engine.event.Event.*;
import static md.leonis.tetris.engine.model.GameState.*;

public class Tetris extends EventManager implements PropertiesHolder {

    private final Config config;

    private int level;
    private int score;
    private int lines;

    private int width, height;

    private volatile Board board;
    private volatile Figure figure, nextFigure;
    private volatile Critter critter;

    private GameState state; // VOID
    private boolean crazy; // false

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
    }

    public void start() {
        state = VOID;
        level = config.startLevel;
        score = 0;
        lines = 0;

        board = new Board(this);
        critter = new Critter(this);

        createNextFigure();
        createFigure();
        NextMove nm = new NextMove();
        NextFrame nf = new NextFrame();
        state = RUNNING;
        critter.start();
        nm.start();
        nf.start();
        initialized = true;
    }

    public void keyPressed(int keyCode) {
        if (state == PAUSED || state == FINISHED) {
            return;
        }
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
                if (figure.moveLeft()) {
                    notify(PLAY_SOUND, "2");
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (figure.moveRight()) {
                    notify(PLAY_SOUND, "2");
                }
                break;
            case KeyEvent.VK_DOWN:
                if (figure.moveDown()) {
                    notify(PLAY_SOUND, "2");
                }
                break;
            case KeyEvent.VK_UP:
                if (figure.rotateRight()) {
                    notify(PLAY_SOUND, "1");
                }
                break;
            case KeyEvent.VK_SPACE:
                figure.fallDown();
                generateNextFigure();
                break;
            case KeyEvent.VK_F12: // Cheat
                score += 10000;
                updateStatistics();
                break;
        }
    }

    private void generateNextFigure() {
        if (state == FINISHED) {
            return;
        }
        if (state != VOID) {
            critter.setPaused(true);
            notify(PLAY_SOUND, "0");
            board.mergeFigure(figure);
            board.deleteCompletedRows();
            critter.correctYPosition(board.getCompletedRows());
            critter.setPaused(false);
            updateStatistics();
        }
        createFigure();
        if (critter.isDead() || !figure.isAllowedNewPosition()) {
            finish();
        }
    }

    private void createFigure() {
        figure = nextFigure;
        createNextFigure();
    }

    private void createNextFigure() {
        nextFigure = new Figure(this);
    }

    private void updateStatistics() {
        lines += board.getCompletedRows().size();

        score += config.scores.get(figure.getType());
        score += config.completedRowsBonus.get(board.getCompletedRows().size());

        level = score / config.nextLevel;
    }



    // Поток опускания фигуры - интервал зависит от скорости игры
    class NextMove extends Thread {
        public void run() {
            while (state != FINISHED) {
                if ((state != PAUSED) && figure.isFallen()) {
                    generateNextFigure();
                }
                try {
                    sleep(1001 - level * 100);
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
        critter.setPaused(isPaused);
    }

    public void finish() {
        critter.setStatus(CritterState.DEAD);
        notify(GAME_OVER, null);
        state = FINISHED;
    }

    // Поток рисования - интервал 33 мс (30 кадров в секунду), больше не надо
    class NextFrame extends Thread {
        public void run() {
            while (state != FINISHED) {
                try {
                    sleep(33);
                } catch (InterruptedException e) {
                    // empty
                }
                voiceCritter();

                Tetris.this.notify(REPAINT, null);

                if (critter.isDead()) {
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
                    Tetris.this.notify(FADE_LOOPING_SOUND, "3");
                    Tetris.this.notify(STOP_LOOPING_SOUND, "4");
                case 1:                 //задыхаюсь
                    Tetris.this.notify(STOP_LOOPING_SOUND, "4");
                    Tetris.this.notify(START_LOOPING_SOUND, "3");
                    break;
                case 2:                 //скоро конец
                    Tetris.this.notify(STOP_LOOPING_SOUND, "3");
                    Tetris.this.notify(START_LOOPING_SOUND, "4");
                    break;
            }
        }
    }

    public GameState getState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public int getLines() {
        return lines;
    }

    public int getLevel() {
        return level;
    }

    public Critter getCritter() {
        return critter;
    }

    public Figure getNextFigure() {
        return nextFigure;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public int getColorsCount() {
        return config.colors.length;
    }

    @Override
    public Integer getTransparentColor() {
        return config.transparentColor;
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
}
