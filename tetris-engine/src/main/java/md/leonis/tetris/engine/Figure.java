package md.leonis.tetris.engine;

import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.Coordinates;

import java.util.*;

public class Figure {

    private static final Random RAND = new Random();       // для генерации случайных чисел

    private static List<Coordinates> FIGURE_DEF = new ArrayList<>();

    static {
        FIGURE_DEF.add(new Coordinates(-1,0,-1,1,0,0,0,1)); // O
        FIGURE_DEF.add(new Coordinates(-1,0,0,0,1,0,1,1)); // J
        FIGURE_DEF.add(new Coordinates(-1,0,0,0,1,0,1,-1)); // L
        FIGURE_DEF.add(new Coordinates(-1,0,0,0,1,0,0,1)); // T
        FIGURE_DEF.add(new Coordinates(-1,0,0,0,0,1,1,1)); // Z
        FIGURE_DEF.add(new Coordinates(-1,1,0,1,0,0,1,0)); // S
        FIGURE_DEF.add(new Coordinates(-1,0,0,0,1,0,2,0)); // I
    }

    private int color;
    private int type;
    private Coordinates coordinates; // Figure tiles coordinates
    private Coordinates coordinatesCopy; // Reserve copy
    private int left, top;         // положение в "стакане"
    private int leftCopy, topCopy;         // резервное положение в "стакане"
    private boolean fallen;

    private final PropertiesHolder properties;

    Figure(PropertiesHolder properties) {
        this.properties = properties;
        initFigure();
    }

    private void initFigure() {
        fallen = false;
        left = properties.getWidth() / 2;
        top = 2;

        type = RAND.nextInt(FIGURE_DEF.size());
        int tilesCount = figureTilesCount();
        coordinates = new Coordinates(FIGURE_DEF.get(type), tilesCount);
        color = RAND.nextInt(properties.getColorsCount() - 1) + 1;

        // Поворот фигуры в градусах
        int angle = RAND.nextInt(3);
        for (int i = 0; i < angle; i++) {
            rotateLeft();
        }

        //тут сделать генерацию пятого элемента
        if (tilesCount == 5) {
            generateFifthElement();
        }
    }

    private int figureTilesCount() {
        if (properties.isCrazy()) {
            int r = RAND.nextInt(10);
            if (r == 0) {
                return RAND.nextInt(3) + 1;
            }
            if (r > 5) {
                return 5;
            }
        }
        return 4;
    }

    private void generateFifthElement() {
        int newX = 0, newY = 0;
        boolean f = true;
        do {
            int r = RAND.nextInt(coordinates.size());
            int ra = RAND.nextInt(4);
            switch (ra) {
                case 0:
                    newX = coordinates.get(r).getX() + 1;
                    newY = coordinates.get(r).getY();
                    f = coordinates.contains(newX, newY);
                    break;
                case 1:
                    newX = coordinates.get(r).getX() - 1;
                    newY = coordinates.get(r).getY();
                    f = coordinates.contains(newX, newY);
                    break;
                case 2:
                    newX = coordinates.get(r).getX();
                    newY = coordinates.get(r).getY() + 1;
                    f = coordinates.contains(newX, newY);
                    break;
                case 3:
                    newX = coordinates.get(r).getX();
                    newY = coordinates.get(r).getY() - 1;
                    f = coordinates.contains(newX, newY);
                    break;
            }
        } while (f);
        coordinates.add(newX, newY);
    }

    private void backup() {
        leftCopy = left;
        topCopy = top;
        coordinatesCopy = coordinates.copy();
    }

    private void restore() {
        left = leftCopy;
        top = topCopy;
        coordinates = coordinatesCopy;
    }

    boolean moveLeft() {
        backup();
        left--;
        if (!isAllowedNewPosition()) {
            restore();
            return false;
        }
        return true;
    }

    boolean moveRight() {
        backup();
        left++;
        if (!isAllowedNewPosition()) {
            restore();
            return false;
        }
        return true;
    }

    boolean rotateRight() {
        backup();
        coordinates.forEach(c -> {
            int k = c.getX();
            c.setX(c.getY());
            c.setY(-k);
        });
        if (!isAllowedNewPosition()) {
            restore();
            return false;
        }
        return true;
    }

    boolean rotateLeft() {
        backup();
        coordinates.forEach(c -> {
            int k = c.getY();
            c.setY(c.getX());
            c.setX(-k);
        });
        if (!isAllowedNewPosition()) {
            restore();
            return false;
        }
        return true;
    }

    boolean moveDown() {
        backup();
        top++;
        if (!isAllowedNewPosition()) {
            restore();
            fallen = true;
            return false;
        }
        return true;
    }

    void fallDown() {
        do {
            backup();
            top++;
            if (properties.getCritter().isCrushed() && !properties.getCritter().isStandingOnTheGround()) {
                properties.getCritter().correctYPosition(Collections.singletonList(300));
            }
        } while (isAllowedNewPosition());
        restore();
        fallen = true;
    }

    public int getGhostTop() {
        int ghostTop = top;
        boolean flag = true;
        do {
            ghostTop++;
            for (Coordinate coordinate : coordinates) {
                if (properties.getBoard().isCoordinateNotAllowed(coordinate.getX() + left, coordinate.getY() + ghostTop)) {
                    flag = false;
                    break;
                }
            }
        } while (flag);
        return --ghostTop;
    }

    boolean isAllowedNewPosition() {
        for (Coordinate coordinate : coordinates) {
            if (properties.getBoard().isCoordinateNotAllowed(coordinate.getX() + left, coordinate.getY() + top)) {
                return false;
            }
        }
        return true;
    }

    boolean isNotOccupied(int newX, int newY) {
        for (Coordinate coordinate : coordinates) {
            if ((newX == (coordinate.getX() + left)) && (newY == (coordinate.getY() + top))) {
                return false;
            }
        }
        return true;
    }

    public int getColor() {
        return color;
    }

    int getType() {
        return type;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    boolean isFallen() {
        return fallen;
    }
}
