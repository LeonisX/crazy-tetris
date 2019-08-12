package md.leonis.tetris.engine;

import md.leonis.tetris.engine.config.Config;
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.GameEventListener;
import md.leonis.tetris.engine.model.GameScore;
import md.leonis.tetris.engine.model.GameState;
import md.leonis.tetris.engine.model.Language;

import java.awt.*;

public abstract class AbstractGameService implements GameServiceInterface, GameEventListener {

    private Config config;
    private LanguageProvider languageProvider;
    private StorageInterface storage;

    private Tetris tetris;
    private Records gameRecords;

    private boolean crazy = false;
    private GuiInterface gui;

    @Override
    public void notify(GameEvent event, Object message) {
        switch (event) {
            case REPAINT:
                gui.repaint();
                break;

            case UPDATE_SCORE:
                gui.updateStatistics();
                break;

            case GAME_OVER:
                gui.gameOver();
                break;
        }
    }

    @Override
    public void processEvent(GameEvent event) {
        if (tetris != null) {
            tetris.processEvent(event);
        }
    }

    @Override
    public void saveRecord(String name) {
        if (name.length() == 0) {
            name = translate("anonymous.name");
        }
        gameRecords.verifyAndAddScore(name, getGameScore().getScore());
        storage.saveRecord(gameRecords.getRecords());
    }

    @Override
    public String translate(String key, Object... params) {
        return languageProvider.getTranslation(key, params);
    }

    @Override
    public Language switchLanguage() {
        switch (LanguageProvider.getCurrentLanguage()) {
            case EN:
                LanguageProvider.setCurrentLanguage(Language.RU);
                break;
            case RU:
                LanguageProvider.setCurrentLanguage(Language.EN);
                break;
        }
        return LanguageProvider.getCurrentLanguage();
    }

    @Override
    public void setGui(GuiInterface gui) {
        this.gui = gui;
    }

    @Override
    public boolean isDebug() {
        return config.isDebug();
    }

    @Override
    public void pauseGame(boolean paused) {
        if (tetris != null) {
            tetris.pause(paused);
        }
    }

    @Override
    public boolean isInitialized() {
        return (tetris != null) && tetris.isInitialized();
    }

    @Override
    public boolean isFinished() {
        return (tetris == null) || tetris.getState() == GameState.FINISHED;
    }

    @Override
    public Color getTransparentColor() {
        return config.getColorConfig().getColor(tetris.getTransparentColor());
    }

    @Override
    public Color getGrayColor() {
        return config.getColorConfig().getColor(config.getColorConfig().grayColor);
    }

    @Override
    public Color getCritterColor() {
        return config.getColorConfig().getColor(config.getColorConfig().critterColor);
    }

    @Override
    public Color getColor(int color) {
        return config.getColorConfig().getColor(color);
    }

    @Override
    public Color getColor(int color, int factor) {
        return config.getColorConfig().getColor(color, factor);
    }

    @Override
    public int getWidth() {
        return crazy ? config.crazyWidth : config.standardWidth;
    }

    @Override
    public int getHeight() {
        return crazy ? config.crazyHeight : config.standardHeight;
    }

    @Override
    public int getTileWidth() {
        return config.tileWidth;
    }

    @Override
    public int getTileHeight() {
        return config.tileHeight;
    }

    @Override
    public Board.Glass getGlass() {
        return tetris.getGlass();
    }

    @Override
    public Figure getFigure() {
        return tetris.getFigure();
    }

    @Override
    public Figure getNextFigure() {
        return tetris.getNextFigure();
    }

    @Override
    public Critter getCritter() {
        return tetris.getCritter();
    }

    @Override
    public GameScore getGameScore() {
        return tetris.getGameScore();
    }

    @Override
    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public LanguageProvider getLanguageProvider() {
        return languageProvider;
    }

    public void setLanguageProvider(LanguageProvider languageProvider) {
        this.languageProvider = languageProvider;
    }

    public StorageInterface getStorage() {
        return storage;
    }

    public void setStorage(StorageInterface storage) {
        this.storage = storage;
    }

    public Tetris getTetris() {
        return tetris;
    }

    public void setTetris(Tetris tetris) {
        this.tetris = tetris;
    }

    public Records getGameRecords() {
        return gameRecords;
    }

    public void setGameRecords(Records gameRecords) {
        this.gameRecords = gameRecords;
    }

    public boolean isCrazy() {
        return crazy;
    }

    public void setCrazy(boolean crazy) {
        this.crazy = crazy;
    }

    public GuiInterface getGui() {
        return gui;
    }
}
