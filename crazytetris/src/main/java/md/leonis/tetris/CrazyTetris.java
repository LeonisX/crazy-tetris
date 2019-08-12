package md.leonis.tetris;

import md.leonis.tetris.engine.LanguageProvider;
import md.leonis.tetris.engine.StorageInterface;
import md.leonis.tetris.engine.config.Config;
import md.leonis.tetris.engine.model.Language;

import javax.swing.*;

/*
 * Отсюда начинается выполнение программы
 */
public class CrazyTetris {

    public static void main(String[] args) {

        String version = CrazyTetris.class.getPackage().getImplementationVersion();
        boolean debug = (version == null);
        String effectiveVersion = debug ? "(Debug)" : "v" + version;

        Config config = new Config(debug);
        LanguageProvider languageProvider = new LanguageProvider(Language.RU);

        StorageInterface storage = new FileSystemStorage();

        GameService gameService = new GameService(config, languageProvider, storage);

        SwingUtilities.invokeLater(() -> {
            GameFrame window = new GameFrame("Crazy Tetris " + effectiveVersion, gameService);
            gameService.setGui(window);
            window.setResizable(false);
        });
    }
}
