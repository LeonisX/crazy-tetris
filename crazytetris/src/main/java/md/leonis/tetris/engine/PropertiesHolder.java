package md.leonis.tetris.engine;

public interface PropertiesHolder {

    Board getBoard();

    int getColorsCount();

    int getTransparentColor();

    boolean isCrazy();

    int[][] getGlass();

}
