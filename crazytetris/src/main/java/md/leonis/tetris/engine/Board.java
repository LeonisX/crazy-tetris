package md.leonis.tetris.engine;

import md.leonis.tetris.Tetris;

public class Board {
    //TODO interface for GUI instead of Tetris
    private Tetris tetris;
    private int width, height;
    private int[][] glass;//, newGlass;
    private int[] deleted = new int[4];
    private int deletedLines = 0;
    private int falledFigure = 255;

    public Board(Tetris tetris, int width, int height) {
        this.tetris = tetris;
        this.width = width;
        this.height = height;
        start();
    }

    private void start() {
        deletedLines = 0;
        glass = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                glass[i][j] = tetris.getTransparentColor();
            }
        }
    }

    public void falled(Figure figure) {
        deletedLines = 0;
        falledFigure = figure.getType();
        for (int i = 0; i < figure.getX().length; i++) {
            glass[figure.getX()[i] + figure.getLeft()][figure.getY()[i] + figure.getTop()] = figure.getColor();
        }
        int currentLine = height - 1;
        for (int j = height - 1; j >= 0; j--) {
            boolean f = false;
            for (int i = 0; i < width; i++) {
                if (glass[i][j] == tetris.getTransparentColor()) {
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
                deleted[deletedLines] = j;
                deletedLines++;
            }
        }
        for (int j = 0; j < currentLine; j++) {
            for (int i = 0; i < width; i++) {
                glass[i][j] = tetris.getTransparentColor();
            }
        }
    }

    public int[][] getGlass() {
        return glass;
    }

    public int[] getDeleted() {
        return deleted;
    }

    public int getDeletedLines() {
        return deletedLines;
    }

    public void setDeletedLines(int deletedLines) {
        this.deletedLines = deletedLines;
    }

    public int getFalledFigure() {
        return falledFigure;
    }
}
