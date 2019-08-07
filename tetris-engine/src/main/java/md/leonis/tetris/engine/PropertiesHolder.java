package md.leonis.tetris.engine;

public interface PropertiesHolder {

    int getWidth();

    int getHeight();

    Board getBoard();

    int getColorsCount();

    Integer getTransparentColor();

    boolean isCrazy();

    Board.Glass getGlass();

    Figure getFigure();

    Critter getCritter();
}
