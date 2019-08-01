package md.leonis.tetris.engine;

import java.util.ArrayList;
import java.util.List;

public class Board {
    //TODO interface for GUI instead of Tetris
    private PropertiesHolder properties;
    private int width, height;
    private int[][] glass;//, newGlass;
    private List<Integer> deletedLines;
    private int falledFigure = 255;

    public Board(PropertiesHolder properties, int width, int height) {
        this.properties = properties;
        this.width = width;
        this.height = height;
        start();
    }

    private void start() {
        resetDeletedLines();
        glass = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                glass[i][j] = properties.getTransparentColor();
            }
        }
    }

    public void falled(Figure figure) {
        resetDeletedLines();
        falledFigure = figure.getType();
        for (int i = 0; i < figure.getX().length; i++) {
            glass[figure.getX()[i] + figure.getLeft()][figure.getY()[i] + figure.getTop()] = figure.getColor();
        }
        int currentLine = height - 1;
        for (int j = height - 1; j >= 0; j--) {
            boolean f = false;
            for (int i = 0; i < width; i++) {
                if (glass[i][j] == properties.getTransparentColor()) {
                    f = true;
                    break;
                }
            }
            if (f) {
                if (currentLine != j) {
                    for (int i = 0; i < width; i++) {
                        glass[i][currentLine] = glass[i][j];
                    }
                }
                currentLine--;
            } else {
                deletedLines.add(j);
            }
        }
        for (int j = 0; j < currentLine; j++) {
            for (int i = 0; i < width; i++) {
                glass[i][j] = properties.getTransparentColor();
            }
        }
    }

    public int[][] getGlass() {
        return glass;
    }

    public List<Integer> getDeletedLines() {
        return deletedLines;
    }

    public int getFalledFigure() {
        return falledFigure;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void resetDeletedLines() {
        deletedLines = new ArrayList<>();
    }
}
