package md.leonis.tetris;

import javax.swing.*;

/*
 * Отсюда начинается выполнение программы
 */
public class CrazyTetris {

    public static void main(String[] args) {

        String version = CrazyTetris.class.getPackage().getImplementationVersion();
        boolean isDebug = (version == null);
        String effectiveVersion = isDebug ? "(Debug)" : "v" + version;

        SwingUtilities.invokeLater(() -> {
            GameFrame window = new GameFrame("Crazy Tetris " + effectiveVersion, isDebug);
            window.setResizable(false);
        });
    }
}
