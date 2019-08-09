package md.leonis.tetris.engine;

import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.Coordinates;
import md.leonis.tetris.engine.model.CritterState;

import java.util.List;

import static md.leonis.tetris.engine.model.CritterState.*;

public class Critter extends Thread {

    private static final double MOVING_AIR_FLOW_RATE = 2.5;
    private static final double STAYING_AIR_FLOW_RATE = 5;
    private static final double BOUNDED_AIR_FLOW_RATE = 1;    // divident
    private static final double FALLING_AIR_FLOW_RATE = 0;
    private static final double JUMPING_AIR_FLOW_RATE = 10;    // deducted

    private static final int VERTICAL_SPEED = 400;
    private static final int HORIZONTAL_SPEED = 80;

    private CritterState status;
    private double air;
    private int availableAirVolume;

    private int x, y;
    private int maxJump = 2;
    private int jumpPower = 0;

    private int horizontalDirection = 1; // 1 - to the right, -1 - to the left, 0 - nowhere
    private int verticalDirection = 0; // same way

    private boolean isPaused = false;

    private PropertiesHolder properties;

    Critter(PropertiesHolder properties) {
        this.properties = properties;
        x = 0;
        y = 0;
        air = 100; // air accounting relative to the cavity
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

    boolean isStandingOnTheGround() {
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
                if (isOccupiedCoordinate(x + horizontalDirection, y)) { // if after lifting there is no way to go aside
                    if (jumpPower == 0) {
                        horizontalDirection = -horizontalDirection;
                    } // at the end we change direction
                } else {
                    x += horizontalDirection;
                    setState();
                    verticalDirection = 0;
                    return;
                } // is it possible to go aside? leaving

                if (jumpPower == 0) { // if the jump height is over
                    verticalDirection = 1;
                    return;
                }

                if (isFreeCoordinate(x, y - 1)) { // if there is still more to go
                    status = JUMPING;
                    y--;
                    jumpPower--;
                } else { // if there’s nowhere else to rise (ceiling)
                    jumpPower = 0;
                    verticalDirection = 1;
                    horizontalDirection = -horizontalDirection;
                }
        }
    }

    public boolean isDead() {
        return status == DEAD || isCrushed();
    }

    public boolean isCrushed() {
        return isOccupiedCoordinate(x, y);
    }

    private boolean isOccupiedCoordinate(int newX, int newY) {
        return !isFreeCoordinate(newX, newY);
    }

    private boolean isFreeCoordinate(int newX, int newY) {
        if (properties.getBoard().isCoordinateNotAllowed(newX, newY)) {
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

    public String getStringStatus() {
        if (isCrushed()) {
            return "critter.status.crushed";
        }
        if (air <= 0) {
            return "critter.status.suffocated";
        }
        if (isBounded()) {
            if (air < 50) {
                return "critter.status.choking";
            } else {
                return "critter.status.need.air";
            }
        }
        if (air < 75) {
            return "critter.status.need.to.breath";
        }
        return "critter.status.fine";
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
