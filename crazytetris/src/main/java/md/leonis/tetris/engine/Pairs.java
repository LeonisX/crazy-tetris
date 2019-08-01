package md.leonis.tetris.engine;

import java.util.ArrayList;

public class Pairs extends ArrayList<Pair> {

    void addUnique(int x, int y) {
        Pair pair = new Pair(x, y);
        if (!this.contains(pair)) {
            this.add(pair);
        }
    }

    void add(int x, int y) {
        this.add(new Pair(x, y));
    }

    boolean notContains(int x, int y) {
        return !this.contains(new Pair(x, y));
    }
}
