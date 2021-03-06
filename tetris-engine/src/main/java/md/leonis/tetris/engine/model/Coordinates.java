package md.leonis.tetris.engine.model;

import java.util.ArrayList;

public class Coordinates extends ArrayList<Coordinate> {

    public Coordinates() {
    }

    public Coordinates(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        add(x1, y1);
        add(x2, y2);
        add(x3, y3);
        add(x4, y4);
    }

    public Coordinates(Coordinates coordinates, int tilesCount) {
        for (int i = 0; i < Math.min(coordinates.size(), tilesCount); i++) {
            this.add(new Coordinate(coordinates.get(i)));
        }
    }

    public void addUnique(int x, int y) {
        Coordinate coordinate = new Coordinate(x, y);
        if (!this.contains(coordinate)) {
            this.add(coordinate);
        }
    }

    public void add(int x, int y) {
        this.add(new Coordinate(x, y));
    }

    boolean notContains(int x, int y) {
        return !this.contains(new Coordinate(x, y));
    }

    public boolean contains(int x, int y) {
        return this.contains(new Coordinate(x, y));
    }

    public Coordinates copy() {
        Coordinates coordinates = new Coordinates();
        this.forEach(c -> coordinates.add(new Coordinate(c)));
        return coordinates;
    }
}
