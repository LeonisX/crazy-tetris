package md.leonis.tetris.engine.config;

import java.awt.*;
import java.util.Arrays;

public class Config {

    public Config(boolean debug) {
        this.debug = debug;
    }

    public int fps = 30;
    public int refreshAfter = 1000 / fps;

    public boolean soundOn = true;

    public Color[] colors = {
            Color.BLACK, // == transparent
            Color.GRAY,
            Color.WHITE,
            Color.RED,
            Color.YELLOW,
            Color.GREEN,
            Color.MAGENTA,
            Color.CYAN,
            Color.BLUE
    };

    public int transparentColor = 0;
    public int grayColor = 1;
    public int critterColor = 2;

    public String[] webColors = Arrays.stream(colors).map(this::formatWebColor).toArray(String[]::new);

    private String formatWebColor(Color color) {
        return String.format("#%1$06X", color.getRGB() & 0x00FFFFFF);
    }

    public Color getColor(int color, int factor) {
        return new Color(
                colors[color].getRed() / factor,
                colors[color].getGreen() / factor,
                colors[color].getBlue() / (int) Math.round((factor + 1.0) / 2) // We need more brighter blue color
        );
    }

    public Color getColor(int color) {
        return colors[color];
    }

    public String getWebColor(int color, int factor) {
        return formatWebColor(getColor(color, factor));
    }

    public String getWebColor(int color) {
        return webColors[color];
    }

    public int tileWidth = 20;
    public int tileHeight = 20;

    public int standardWidth = 10;
    public int standardHeight = 22;

    public int crazyWidth = 12;
    public int crazyHeight = 23;

    public int width = 10;
    public int height = 22;

    public boolean critterEnabled = true;

    public ScoreConfig scoreConfig = new ScoreConfig();

    private boolean debug = false;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
