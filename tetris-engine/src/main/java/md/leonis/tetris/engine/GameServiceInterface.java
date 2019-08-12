package md.leonis.tetris.engine;

import md.leonis.tetris.engine.config.Config;
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.model.GameScore;
import md.leonis.tetris.engine.model.Language;

import java.awt.*;

public interface GameServiceInterface {

    void setGui(GuiInterface gui);

    void startGame(boolean crazy);

    void pauseGame(boolean paused);

    Records initializeRecords();
    void saveRecord(String text);

    void playMusic();
    void fadeMusic();
    void stopMusic();
    
    void processEvent(GameEvent gameOver);

    Language switchLanguage();

    String translate(String key, Object... params);

    boolean isDebug();

    boolean isInitialized();

    boolean isFinished();

    Color getTransparentColor();
    Color getGrayColor();
    Color getCritterColor();
    Color getColor(int color);
    Color getColor(int color, int factor);

    int getWidth();
    int getHeight();

    int getTileWidth();
    int getTileHeight();

    Board.Glass getGlass();
    Figure getFigure();
    Figure getNextFigure();
    Critter getCritter();

    GameScore getGameScore();

    Config getConfig();
}
