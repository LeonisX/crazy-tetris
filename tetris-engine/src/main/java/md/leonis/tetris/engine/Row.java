package md.leonis.tetris.engine;

import java.util.Arrays;

public class Row {

    private final Integer transparentColor;

    private final int[] elements;

    Row(int size, int transparentColor) {
        this.transparentColor = transparentColor;
        elements = new int[size];
        Arrays.fill(elements, transparentColor);
    }

    boolean isNotFull() {
        return !isFull();
    }

    boolean isFull() {
        return Arrays.stream(elements).noneMatch(transparentColor::equals);
    }

    int[] getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }
}
