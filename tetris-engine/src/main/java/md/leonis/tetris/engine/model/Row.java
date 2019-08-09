package md.leonis.tetris.engine.model;

import java.util.Arrays;

public class Row {

    private final Integer transparentColor;

    private final int[] elements;

    public Row(int size, int transparentColor) {
        this.transparentColor = transparentColor;
        elements = new int[size];
        Arrays.fill(elements, transparentColor);
    }

    public boolean isNotFull() {
        return !isFull();
    }

    public boolean isFull() {
        return Arrays.stream(elements).noneMatch(transparentColor::equals);
    }

    public int[] getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }
}
