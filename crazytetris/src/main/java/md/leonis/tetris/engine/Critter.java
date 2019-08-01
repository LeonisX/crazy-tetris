package md.leonis.tetris.engine;

import java.util.List;

import static md.leonis.tetris.engine.CritterState.*;

public class Critter extends Thread {

    private CritterState status;
    private double air;
    private int freeTilesCount;

    private int x, y;
    private int maxJump = 2;
    private int jumpPower = 0;
    private int direction = 1;        // 1 - вправо, -1 - влево, 0 - никуда
    private int verticalDirection = 0; // аналогично
    private boolean isPaused = false;

    private PropertiesHolder properties;

    public Critter(PropertiesHolder properties) {
        this.properties = properties;
        x = 0;
        y = 0;
        air = 100; //сделать учёт воздуха относительно полости
        status = MOVING;
    }

    public void run() {
        while (status != DEAD) {
            int speed = (verticalDirection == 0) ? 400 : 80;
            try {
                sleep(speed);
            } catch (InterruptedException e) {
                //TODO
            }
            if (isCoordinateIsOccupied(x, y)) {
                status = DEAD;
                break;
            }
            if (isPaused) {
                continue;
            }
            airControl();
            makeMove();
        }
    }

    private void airControl() {
        if (isPaused || (status == DEAD)) return;
        final double airMOVING = 2;        //  /2
        final double airSTAYING = 5;
        final double airBLOCKED = 1;    //делимое
        final double airFALLING = 0;
        final double airJUMPING = 10;    //вычитается
        double ak;
        if (status == MOVING || status == STAYING) {
            ak = airSTAYING;
        } else {
            ak = 0;
        }
        switch (status) {
            case DEAD:
                air = 0;
                break;
            case MOVING:
                ak /= airMOVING;
                break;
            case FALLING:
                ak = airFALLING;
                break;
            case JUMPING:
                ak = -airJUMPING;
                break;
        }
        if (isBounded()) {
            ak = -airBLOCKED / freeTilesCount / airMOVING;
            if (status == STAYING) ak /= 2;
        }
        air += ak;
        air = Math.min(5.0, air);
        setState();
        if (air <= 0.0) {
            air = 0;
            status = DEAD;
        }
    }

    private void makeMove() {
        if (isPaused || (status == DEAD)) return;
        if (verticalDirection != 0) {
            moveVertical();
        } else {
            moveHorizontal();
        }
    }

    private void setState() {
        double k = isBounded() ? 50.0 : 80.0;
        status = (air > k) ? MOVING : STAYING;
    }

    private void moveHorizontal() {
        if (isStanding()) {
            if (isCoordinateIsOccupied(x + direction, y)) {
                if (status == MOVING) {
                    verticalDirection = -1;
                    jumpPower = maxJump;
                }
            } else {
                if (status == MOVING) {
                    x += direction;
                }
            }
        } else {
            verticalDirection = 1;
        }
    }

    private boolean isStanding() {
        return ((y + 1 == properties.getBoard().getHeight()) || (!(properties.getGlass()[x][y + 1] == properties.getTransparentColor())));
    }

    private void moveVertical() {
        switch (verticalDirection) {
            case 1:
                if (!isStanding()) {
                    y++;
                    status = FALLING;
                } else {
                    verticalDirection = 0;
                }
                break;
            case -1:
                if (isCoordinateIsOccupied(x + direction, y)) {           // если после подъёма нет возможности уйти в сторону
                    if (jumpPower == 0) {
                        direction = -direction;
                    }   // на излёте меняем направление движения
                } else {
                    x += direction;
                    setState();
                    verticalDirection = 0;
                    return;
                }// есть возможность уйти в сторону? уходим

                if (jumpPower == 0) {
                    verticalDirection = 1;
                    return;
                }            // если закончилась высота прыжка

                if (isCoordinateIsFree(x, y - 1)) {                // если есть куда ещё подниматься
                    status = JUMPING;
                    y--;
                    jumpPower--;
                } else {                        // если некуда больше подниматься (потолок)
                    jumpPower = 0;
                    verticalDirection = 1;
                    direction = -direction;
                }
        }
    }

    public boolean isDead() {
        return isCoordinateIsOccupied(x, y);
    }

    private boolean isCoordinateIsOccupied(int newX, int newY) {
        return !isCoordinateIsFree(newX, newY);
    }

    private boolean isCoordinateIsFree(int newX, int newY) {
        if (status == DEAD
                || newX < 0
                || newY < 0
                || newX >= properties.getBoard().getWidth()
                || newY >= properties.getBoard().getHeight()
                || properties.getGlass()[newX][newY] != properties.getTransparentColor()) {
            return false;
        }
        Figure figure = properties.getFigure();
        if (figure != null) {
            return figure.isNotConflict(newX, newY);
        }
        return true;
    }

    public boolean isBounded() {
        Pairs freeTiles = new Pairs();
        int ax, ay;
        freeTiles.add(x, y);
        do {
            int size = freeTiles.size();
            for (int i = 0; i < size; i++) {
                ax = freeTiles.get(i).getX() + 1;
                ay = freeTiles.get(i).getY();
                if (isCoordinateIsFree(ax, ay)) {
                    freeTiles.addUnique(ax, ay);
                }
                ax = freeTiles.get(i).getX() - 1;
                ay = freeTiles.get(i).getY();
                if (isCoordinateIsFree(ax, ay)) {
                    freeTiles.addUnique(ax, ay);
                }
                ax = freeTiles.get(i).getX();
                ay = freeTiles.get(i).getY() + 1;
                if (isCoordinateIsFree(ax, ay)) {
                    freeTiles.addUnique(ax, ay);
                }
                ax = freeTiles.get(i).getX();
                ay = freeTiles.get(i).getY() - 1;
                if (isCoordinateIsFree(ax, ay)) {
                    freeTiles.addUnique(ax, ay);
                }
            }
            if (freeTiles.size() == size || freeTiles.size() > 10) {
                break;
            }
        } while (true);
        freeTilesCount = freeTiles.size();
        return (freeTilesCount < 10);
    }

    public void correctYPosition(List<Integer> deletedLines) {
        y += deletedLines.stream().filter(dy -> dy > y).count();
    }

    public void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    public CritterState getStatus() {
        return status;
    }

    public void setStatus(CritterState status) {
        this.status = status;
    }

    public double getAir() {
        return air;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDirection() {
        return direction;
    }
}
