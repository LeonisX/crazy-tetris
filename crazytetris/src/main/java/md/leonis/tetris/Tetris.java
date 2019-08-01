package md.leonis.tetris;

import md.leonis.tetris.engine.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import static md.leonis.tetris.ResourceUtils.getResourceAsStream;

public class Tetris extends KeyAdapter implements PropertiesHolder {

    private Color[] colors = {
            Color.BLACK, // == transparent
            Color.WHITE,
            Color.GRAY,
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.MAGENTA,
            Color.CYAN,
            Color.BLUE
    };

    private int transparentColor = 0;

    //TODO enum
    private final int NOTINIT = 0;
    private final int RUNNING = 1;
    private final int PAUSED = 2;
    private final int GAMEOVER = 3;
    private int nextLevel;
    private int startLevel;
    private int level;
    int score;
    private int lines;
    private int erasedLines;
    private int tileWidth, tileHeight;
    private int width = 10, height = 22;
    private Board board;
    private Figure figure, nextFigure;
    private Critter critter;
    private NextMove nm;
    private NextFrame nf;
    JPanel panel;
    Monitor monitor;
    int state = NOTINIT;
    private boolean crazy = false;
    BufferedImage frameBuffer;
    private SoundMonitor soundMonitor;                    // монитор для звуковых эффектов

    public Tetris(boolean isDebug) {
        tileWidth = 20;
        tileHeight = 20;
        nextLevel = 10000; //игра очень быстро заканчивается, вероятно надо увеличить
        startLevel = 0;
        //TODO resources separately!!!
        soundMonitor = new SoundMonitor();            // создаём монитор звуковых эффектов
        soundMonitor.addSound(getResourceAsStream("audio/falled.wav", isDebug));        // 1 звук
        soundMonitor.addSound(getResourceAsStream("audio/rotate.wav", isDebug));        // 2 звук
        soundMonitor.addSound(getResourceAsStream("audio/click.wav", isDebug));         // 3 звук
        soundMonitor.addSound(getResourceAsStream("audio/heartbeat-a.wav", isDebug));   // 4 звук
        soundMonitor.addSound(getResourceAsStream("audio/heartbeat-b.wav", isDebug));   // 5 звук
        soundMonitor.setGain(0, 0.9f);               // громкость (от 0 до 1.0f)
        soundMonitor.setGain(1, 0.9f);
        soundMonitor.setGain(2, 1.0f);
        soundMonitor.setGain(3, 0.8f);
        soundMonitor.setGain(4, 0.9f);
    }

    public void setCrazy(boolean crazy) {
        this.crazy = crazy;
        if (crazy) {
            width = 12;
            height = 23;
        } else {
            width = 10;
            height = 22;
        }
    }

    //TODO move to callbacks



    public void start() {
        board = new Board(this, width, height);
        critter = new Critter(this);
        level = startLevel;
        score = 0;
        lines = 0;
        erasedLines = 0;
        makeNextFigure();
        next();
        nm = new NextMove();
        nf = new NextFrame();
        state = RUNNING;
        critter.start();
        nm.start();
        nf.start();
    }

    private void makeNextFigure() {
        nextFigure = new Figure(this);
    }

    private void makeFigure() {
        figure = nextFigure;
        critter.setFigure(figure);
        makeNextFigure();
    }

    private void next() {
        if (state == GAMEOVER) return;
        if (state > NOTINIT) {
            soundMonitor.play(0);
            critter.setPaused(true);
            if (critter.isNotCorrect()) {
                finish();
                return;
            }
            board.falled(figure);
            critter.correctY(board.getDeleted(), board.getDeletedLines());
            critter.setPaused(false);
            score();
        }
        makeFigure();
        if (critter.isNotCorrect()) finish();
        if (!figure.correct()) finish();
    }

    public void pause(boolean paused) {
        if (paused) state = PAUSED;
        else state = RUNNING;
        critter.setPaused(paused);
    }

    public void finish() {
        critter.setStatus(CritterState.DEAD);
        monitor.actionPerformed(new ActionEvent(monitor, ActionEvent.ACTION_PERFORMED, "gameover"));
        state = GAMEOVER;
    }

    /*
    "Слушатель". По требованию добавляет/убирает спрайты.
    */
    public void keyPressed(KeyEvent e) {
        if (state >= PAUSED) return;
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

    //3. Поток опускания фигуры - интервал зависит от скорости игры
    class NextMove extends Thread {
        public void run() {
            while (state < GAMEOVER) {
                if (state != PAUSED) if (figure.isFalled()) next();
                try {
                    sleep(1001 - level * 100);
//                    sleep(10001-level*100);
                } catch (InterruptedException e) {
                    //TODO
                }
                if (state != PAUSED) if (!figure.isFalled()) figure.moveDown();
            }
        }
    }

    //2. Поток рисования - интервал 33 мс (30 кадров в секунду), больше не надо
    class NextFrame extends Thread {
        public void run() {
            while (state < GAMEOVER) {
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
            }
        }
    }

    private void score() {
        lines += board.getDeletedLines();
        switch (board.getFalledFigure()) {
            case 0:
            case 6:
                score += 10;
                break;
            case 1:
            case 2:
            case 3:
                score += 15;
                break;
            case 4:
            case 5:
                score += 20;
        }
        switch (board.getDeletedLines()) {
            case 1:
                score += 100;
                break;
            case 2:
                score += 250;
                break;
            case 3:
                score += 400;
                break;
            case 4:
                score += 600;
        }
        level = score / nextLevel;
        board.setDeletedLines(0);
//        board.falledFigure=255;
    }

    public void draw(Graphics gx) {
//        g=frameBuffer.getGraphics();
        //рисуем стакан
        Graphics2D g = (Graphics2D) gx;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.translate(10, 10);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width * tileWidth, (height - 2) * tileHeight);
        g.setColor(new Color(100, 100, 100));
        for (int i = 1; i < width; i++) {
            for (int j = 3; j < height; j++) {
                g.drawRect(i * tileWidth, (j - 2) * tileHeight, 0, 0);
            }
        }

        for (int i = 0; i < width; i++) {
            for (int j = 2; j < height; j++) {
                g.setColor(colors[board.getGlass()[i][j]]);
                g.fillRoundRect(i * tileWidth, (j - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
            }
        }

        //выводим счёт, линии.
        int lpos = width * tileWidth + 7;
        g.setColor(Color.BLACK);
        g.drawString("Счёт: " + score, lpos, 10);
        g.drawString("Линий: " + lines, lpos, 30);
        g.drawString("Уровень: " + level, lpos, 50);
        String st = "Жизнь прекрасна :)";
        if (critter.getAir() < 75) st = "Надо отдышаться...";
        if (critter.isBlocked()) if (critter.getAir() < 50) st = "Задыхаюсь!!!";
        else st = "Тут мало воздуха...";
        g.drawString("Дыхание: " + (int) critter.getAir() + "%", lpos, 70);
        g.drawString(st, lpos, 90);

        //рисуем фигуру
        for (int i = 0; i < figure.getX().length; i++) {
            int k = figure.ghostTop();
            g.setColor(new Color(colors[figure.getColor()].getRed() / 7, colors[figure.getColor()].getGreen() / 7, colors[figure.getColor()].getBlue() / 4));
            if ((figure.getY()[i] + k) >= 2)
                g.fillRoundRect((figure.getX()[i] + figure.getLeft()) * tileWidth, (figure.getY()[i] + k - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
        }

        int kx = 0;
        for (int i = 0; i < nextFigure.getX().length; i++) if (nextFigure.getX()[i] < kx) kx = nextFigure.getX()[i];
        int ky = 0;
        for (int i = 0; i < nextFigure.getY().length; i++) if (nextFigure.getY()[i] < ky) ky = nextFigure.getY()[i];

        for (int i = 0; i < nextFigure.getX().length; i++) {
            g.setColor(new Color(colors[figure.getColor()].getRed() / 4, colors[figure.getColor()].getGreen() / 4, colors[figure.getColor()].getBlue() / 3));
            g.fillRoundRect((nextFigure.getX()[i] - kx) * tileWidth + lpos + 1, (nextFigure.getY()[i] - ky) * tileHeight + 100 + 1, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);

            g.setColor(colors[nextFigure.getColor()]);
            g.fillRoundRect((nextFigure.getX()[i] - kx) * tileWidth + lpos, (nextFigure.getY()[i] - ky) * tileHeight + 100, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);

        }

        for (int i = 0; i < figure.getX().length; i++) {
            g.setColor(colors[figure.getColor()]);
            if ((figure.getY()[i] + figure.getTop()) >= 2)
                g.fillRoundRect((figure.getX()[i] + figure.getLeft()) * tileWidth, (figure.getY()[i] + figure.getTop() - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
        }

        //рисую персонажа
        if (critter.getStatus() != CritterState.DEAD) {
            g.setColor(Color.WHITE);
            g.drawOval(critter.getX() * tileWidth, (critter.getY() - 2) * tileHeight, tileWidth, tileHeight);
            kx = critter.getDirection() * 2;
            ky = 0;
            if (critter.getStatus() == CritterState.FALLING) ky = 1;
            if (critter.getStatus() == CritterState.JUMPING) ky = -1;
            if (critter.getStatus() == CritterState.STAYING) kx = 0;
            //глаза
            g.drawArc(critter.getX() * tileWidth + 7 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
            g.drawArc(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
            //глаза
            if (critter.getAir() < 50) {
                g.drawRect(critter.getX() * tileWidth + 7 + kx + 1, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
                g.drawRect(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
            }
            //рот
            int wx;
            if (critter.isBlocked()) wx = 2;
            else wx = 6;
            if ((critter.getAir() > 75) && (!critter.isBlocked())) {
                g.drawArc(critter.getX() * tileWidth + 7 + kx - 1, (critter.getY() - 2) * tileHeight + 14 - 3, wx + 2, 3, 0, -180);
            } else g.drawRect(critter.getX() * tileWidth + 7 + kx + (6 - wx) / 2, (critter.getY() - 2) * tileHeight + 14, wx, 0);

        }
//        g.translate(0,0);
    }

    public Color getColor(int index) {
        return colors[index];
    }

    public int getColorsCount() {
        return colors.length;
    }

    public int getTransparentColor() {
        return transparentColor;
    }

    public Board getBoard() {
        return board;
    }

    public boolean isCrazy() {
        return crazy;
    }

    public int[][] getGlass() {
        return board.getGlass();
    }
}

abstract class Monitor implements ActionListener, KeyListener {
    abstract public void actionPerformed(ActionEvent e);

    abstract public void keyPressed(KeyEvent e);

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }
}