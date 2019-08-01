package md.leonis.tetris;

import java.awt.*;

public class Board {
    private Tetris tetris;
    private int width, height;
    Color[][] glass;//, newGlass;
    int[] deleted = new int[4];
    int deletedLines = 0;
    int falledFigure = 255;

    public Board(Tetris tetris, int width, int height) {
        this.tetris = tetris;
        this.width = width;
        this.height = height;
        start();
    }

    private void start() {
        deletedLines = 0;
        glass = new Color[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                glass[i][j] = Color.BLACK;
            }
        }
    }

    public void falled(Figure figure) {
        deletedLines = 0;
        falledFigure = figure.type;
        for (int i = 0; i < figure.x.length; i++) {
            glass[figure.x[i] + figure.left][figure.y[i] + figure.top] = figure.color;
        }
        int currentLine = height - 1;
        for (int j = height - 1; j >= 0; j--) {
            boolean f = false;
            for (int i = 0; i < width; i++) {
                if (glass[i][j].equals(Color.BLACK)) {
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
        for (int j = 0; j < currentLine; j++)
            for (int i = 0; i < width; i++) glass[i][j] = Color.BLACK;
    }
}
