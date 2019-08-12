package md.leonis.tetris.engine.config;

public class Config {

    public Config(boolean debug) {
        this.debug = debug;
    }

    private boolean debug;

    private ColorConfig colorConfig = new ColorConfig();
    private ScoreConfig scoreConfig = new ScoreConfig();

    public int fps = 30;
    public int refreshAfter = 1000 / fps;

    public boolean soundOn = true;

    public int tileWidth = 20;
    public int tileHeight = 20;

    public int standardWidth = 10;
    public int standardHeight = 22;

    public int crazyWidth = 12;
    public int crazyHeight = 23;

    public boolean critterEnabled = true;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public ColorConfig getColorConfig() {
        return colorConfig;
    }

    public void setColorConfig(ColorConfig colorConfig) {
        this.colorConfig = colorConfig;
    }

    public ScoreConfig getScoreConfig() {
        return scoreConfig;
    }

    public void setScoreConfig(ScoreConfig scoreConfig) {
        this.scoreConfig = scoreConfig;
    }
}
