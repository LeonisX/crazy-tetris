package md.leonis.tetris.engine;

import java.util.ArrayList;

public class Coordinates extends ArrayList<Coordinate> {

    void addUnique(int x, int y) {
        Coordinate coordinate = new Coordinate(x, y);
        if (!this.contains(coordinate)) {
            this.add(coordinate);
        }
    }

    void add(int x, int y) {
        this.add(new Coordinate(x, y));
    }

    boolean notContains(int x, int y) {
        return !this.contains(new Coordinate(x, y));
    }
}
