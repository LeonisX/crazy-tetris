package md.leonis.tetris;

import md.leonis.tetris.engine.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static md.leonis.tetris.ResourceUtils.getResourceAsStream;
import static md.leonis.tetris.engine.GameState.*;

public class Tetris extends KeyAdapter implements PropertiesHolder {

    private final Config config;

    private int level;
    int score;
    private int lines;

    private int width, height;

    private Board board;
    private volatile Figure figure, nextFigure;
    private volatile Critter critter;
    private NextMove nm;
    private NextFrame nf;
    JPanel panel;
    Monitor monitor;
    private GameState state = VOID;
    private boolean crazy = false;
    private SoundMonitor soundMonitor;                    // монитор для звуковых эффектов

    private boolean initialized;

    public Tetris(Config config) {


        this.config = config;

        //TODO resources separately!!!
        soundMonitor = new SoundMonitor();            // создаём монитор звуковых эффектов
        soundMonitor.addSound(getResourceAsStream("audio/falled.wav", config.isDebug));        // 1 звук
        soundMonitor.addSound(getResourceAsStream("audio/rotate.wav", config.isDebug));        // 2 звук
        soundMonitor.addSound(getResourceAsStream("audio/click.wav", config.isDebug));         // 3 звук
        soundMonitor.addSound(getResourceAsStream("audio/heartbeat-a.wav", config.isDebug));   // 4 звук
        soundMonitor.addSound(getResourceAsStream("audio/heartbeat-b.wav", config.isDebug));   // 5 звук
        soundMonitor.setGain(0, 0.9f);               // громкость (от 0 до 1.0f)
        soundMonitor.setGain(1, 0.9f);
        soundMonitor.setGain(2, 1.0f);
        soundMonitor.setGain(3, 0.8f);
        soundMonitor.setGain(4, 0.9f);
    }

    public void setCrazy(boolean crazy) {
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
        board = new Board(this);
        critter = new Critter(this);
        level = config.startLevel;
        score = 0;
        lines = 0;
        makeNextFigure();
        next();
        nm = new NextMove();
        nf = new NextFrame();
        state = RUNNING;
        critter.start();
        nm.start();
        nf.start();
        initialized = true;
    }

    private void makeNextFigure() {
        nextFigure = new Figure(this);
    }

    private void makeFigure() {
        figure = nextFigure;
        makeNextFigure();
    }

    private void next() {
        if (state == GAME_OVER) {
            return;
        }
        if (state != VOID) {
            soundMonitor.play(0);
            critter.setPaused(true);
            if (critter.isDead()) {
                finish();
                return;
            }
            board.mergeFigure(figure);
            board.deleteCompletedRows();
            critter.correctYPosition(board.getCompletedRows());
            critter.setPaused(false);
            score();
        }
        makeFigure();
        if (critter.isDead() || !figure.isAllowedNewPosition()) {
            finish();
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
        monitor.actionPerformed(new ActionEvent(monitor, ActionEvent.ACTION_PERFORMED, "gameover"));
        state = GAME_OVER;
    }

    /*
    "Слушатель". По требованию добавляет/убирает спрайты.
    */
    public void keyPressed(KeyEvent e) {
        if (state == PAUSED || state == GAME_OVER) {
            return;
        }
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
                if (figure.moveLeft()) soundMonitor.play(2);
//            if(!critter.correct()) finish();
                break;
            case KeyEvent.VK_RIGHT:
                if (figure.moveRight()) soundMonitor.play(2);
//            if(!critter.correct()) finish();
                break;
            case KeyEvent.VK_DOWN:
                if (figure.moveDown()) soundMonitor.play(2);
//            if(!critter.correct()) finish();
                break;
            case KeyEvent.VK_UP:
                if (figure.rotateRight()) soundMonitor.play(1);
//            if(!critter.correct()) finish();
                break;
            case KeyEvent.VK_SPACE:
                figure.fall();
//            if(!critter.correct()) finish();
                next();
                break;
            case KeyEvent.VK_F12:
                score += 10000;
                score();
                break;
        }
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


    //3. Поток опускания фигуры - интервал зависит от скорости игры
    class NextMove extends Thread {
        public void run() {
            while (state != GAME_OVER) {
                if (state != PAUSED) if (figure.isFallen()) next();
                try {
                    sleep(1001 - level * 100);
//                    sleep(10001-level*100);
                } catch (InterruptedException e) {
                    //TODO
                }
                if ((state != PAUSED) && !figure.isFallen()) {
                    figure.moveDown();
                }
            }
        }
    }

    //2. Поток рисования - интервал 33 мс (30 кадров в секунду), больше не надо
    class NextFrame extends Thread {
        public void run() {
            while (state != GAME_OVER) {
                try {
                    sleep(33);
                } catch (InterruptedException e) {
                    //TODO
                }
                soundMonitor.schedule();
                int k = (critter.getAir() < 50) ? 1 : 0;
                if (critter.getAir() < 10) k = 2;
                switch (k) {
                    case 2:                 //скоро конец
                        if (soundMonitor.isLooping(3)) soundMonitor.stop(3);
                        if (!soundMonitor.isLooping(4)) soundMonitor.loop(4);
                        break;
                    case 1:                 //задыхаюсь
                        if (soundMonitor.isLooping(4)) soundMonitor.stop(4);
                        if (!soundMonitor.isLooping(3)) soundMonitor.loop(3);
                        break;
                    default:                //всё хорошо
                        if ((soundMonitor.isLooping(3)) && (!soundMonitor.ac[3].fade)) soundMonitor.fade(3);
                        if (soundMonitor.isLooping(4)) soundMonitor.stop(4);
                }
                panel.repaint();
                if (critter.isDead()) {
                    finish();
                }
            }
        }
    }

    private void score() {
        lines += board.getCompletedRows().size();

        score += config.scores.get(figure.getType());
        score += config.completedRowsBonus.get(board.getCompletedRows().size());

        level = score / config.nextLevel;
    }

    public GameState getState() {
        return state;
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
