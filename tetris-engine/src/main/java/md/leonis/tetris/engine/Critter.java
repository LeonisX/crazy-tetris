package md.leonis.tetris.engine;

import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.Coordinates;
import md.leonis.tetris.engine.model.CritterState;

import java.util.List;

import static md.leonis.tetris.engine.model.CritterState.*;

public class Critter extends Thread {

    private static final double MOVING_AIR_FLOW_RATE = 2.5;
    private static final double STAYING_AIR_FLOW_RATE = 5;
    private static final double BOUNDED_AIR_FLOW_RATE = 1;    //делимое
    private static final double FALLING_AIR_FLOW_RATE = 0;
    private static final double JUMPING_AIR_FLOW_RATE = 10;    //вычитается

    private static final int VERTICAL_SPEED = 400;
    private static final int HORIZONTAL_SPEED = 80;

    private CritterState status;
    private double air;
    private int availableAirVolume;

    private int x, y;
    private int maxJump = 2;
    private int jumpPower = 0;

    private int horizontalDirection = 1;        // 1 - вправо, -1 - влево, 0 - никуда
    private int verticalDirection = 0; // аналогично

    private boolean isPaused = false;

    private PropertiesHolder properties;

    Critter(PropertiesHolder properties) {
        this.properties = properties;
        x = 0;
        y = 0;
        air = 100; //сделать учёт воздуха относительно полости
        status = MOVING;
    }

    public void run() {
        while (status != DEAD) {
            int speed = (verticalDirection == 0) ? VERTICAL_SPEED : HORIZONTAL_SPEED;
            try {
                Critter.sleep(speed);
            } catch (InterruptedException e) {
                //TODO
            }
            if (isOccupiedCoordinate(x, y)) {
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
        double airReplenishment = 0;
        switch (status) {
            case STAYING:
                airReplenishment = STAYING_AIR_FLOW_RATE;
                break;
            case MOVING:
                airReplenishment = MOVING_AIR_FLOW_RATE;
                break;
            case FALLING:
                airReplenishment = FALLING_AIR_FLOW_RATE;
                break;
            case JUMPING:
                airReplenishment = -JUMPING_AIR_FLOW_RATE;
                break;
        }
        if (isBounded()) {
            airReplenishment = -BOUNDED_AIR_FLOW_RATE / availableAirVolume / 2;
            if (status == STAYING) {
                airReplenishment /= 2;
            }
        }
        air += airReplenishment;
        air = Math.min(100.0, air);
        setState();
        if (air <= 0.0) {
            air = 0;
            status = DEAD;
        }
    }

    private void setState() {
        double movementThreshold = isBounded() ? 50.0 : 80.0;
        status = (air > movementThreshold) ? MOVING : STAYING;
    }

    private void makeMove() {
        if (verticalDirection == 0) {
            moveHorizontal();
        } else {
            moveVertical();
        }
    }

    private void moveHorizontal() {
        if (isStandingOnTheGround()) {
            if (isOccupiedCoordinate(x + horizontalDirection, y)) {
                if (status == MOVING) {
                    verticalDirection = -1; // Try to jump
                    jumpPower = maxJump;
                }
            } else {
                if (status == MOVING) {
                    x += horizontalDirection; // Move
                }
            }
        } else {
            verticalDirection = 1; // Fall
        }
    }

    private boolean isStandingOnTheGround() {
        return isOccupiedCoordinate(x, y + 1);
    }

    private void moveVertical() {
        switch (verticalDirection) {
            case 1:
                if (!isStandingOnTheGround()) {
                    y++;
                    status = FALLING;
                } else {
                    verticalDirection = 0;
                }
                break;
            case -1:
                if (isOccupiedCoordinate(x + horizontalDirection, y)) {           // если после подъёма нет возможности уйти в сторону
                    if (jumpPower == 0) {
                        horizontalDirection = -horizontalDirection;
                    }   // на излёте меняем направление движения
                } else {
                    x += horizontalDirection;
                    setState();
                    verticalDirection = 0;
                    return;
                }// есть возможность уйти в сторону? уходим

                if (jumpPower == 0) {
                    verticalDirection = 1;
                    return;
                }            // если закончилась высота прыжка

                if (isFreeCoordinate(x, y - 1)) {                // если есть куда ещё подниматься
                    status = JUMPING;
                    y--;
                    jumpPower--;
                } else {                        // если некуда больше подниматься (потолок)
                    jumpPower = 0;
                    verticalDirection = 1;
                    horizontalDirection = -horizontalDirection;
                }
        }
    }

    boolean isDead() {
        return isOccupiedCoordinate(x, y);
    }

    private boolean isOccupiedCoordinate(int newX, int newY) {
        return !isFreeCoordinate(newX, newY);
    }

    private boolean isFreeCoordinate(int newX, int newY) {
        if (status == DEAD || properties.getBoard().isCoordinateNotAllowed(newX, newY)) {
            return false;
        }
        Figure figure = properties.getFigure();
        if (figure != null) {
            return figure.isNotOccupied(newX, newY);
        }
        return true;
    }

    public boolean isBounded() {
        Coordinates freeCoordinates = new Coordinates();
        freeCoordinates.add(x, y);
        do {
            int size = freeCoordinates.size();
            for (int i = 0; i < size; i++) {
                Coordinate coordinate = freeCoordinates.get(i);
                addFreeUniqueCoordinate(freeCoordinates, coordinate.getX() + 1, coordinate.getY());
                addFreeUniqueCoordinate(freeCoordinates, coordinate.getX() - 1, coordinate.getY());
                addFreeUniqueCoordinate(freeCoordinates, coordinate.getX(), coordinate.getY() + 1);
                addFreeUniqueCoordinate(freeCoordinates, coordinate.getX(), coordinate.getY() - 1);
            }
            if (freeCoordinates.size() == size || freeCoordinates.size() > 10) {
                break;
            }
        } while (true);
        availableAirVolume = freeCoordinates.size();
        return (availableAirVolume < 10);
    }

    private void addFreeUniqueCoordinate(Coordinates freeTiles, int x, int y) {
        if (isFreeCoordinate(x, y)) {
            freeTiles.addUnique(x, y);
        }
    }

    void correctYPosition(List<Integer> deletedRows) { // After deleting rows
        y += deletedRows.stream().filter(dy -> dy > y).count();
    }

    void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
    }

    public CritterState getStatus() {
        return status;
    }

    void setStatus(CritterState status) {
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

    public int getHorizontalDirection() {
        return horizontalDirection;
    }
}
