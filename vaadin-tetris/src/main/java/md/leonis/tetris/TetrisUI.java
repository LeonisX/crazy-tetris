package md.leonis.tetris;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.flow.server.Constants;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import md.leonis.tetris.audio.GameAudio;
import md.leonis.tetris.audio.SoundMonitor;
import md.leonis.tetris.engine.*;
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.GameEventListener;
import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.CritterState;
import md.leonis.tetris.engine.model.GameState;
import md.leonis.tetris.engine.model.Language;
import org.vaadin.hezamu.canvas.Canvas;
import org.vaadin.viritin.button.PrimaryButton;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static md.leonis.tetris.engine.event.GameEvent.*;
import static md.leonis.tetris.engine.model.SoundId.*;

@Push
@Theme("valo")
@Title("Vaadin Crazy Tetris")
public class TetrisUI extends UI implements GameEventListener {

    @WebServlet(value = {"/*"}, asyncSupported = true, initParams = {
            @WebInitParam(name = Constants.I18N_PROVIDER, value = "com.vaadin.example.ui.TranslationProvider")})
    @VaadinServletConfiguration(productionMode = false, ui = TetrisUI.class)
    public static class Servlet extends VaadinServlet {
    }

    private static final int FPS = 10;
    private static final double MUSIC_VOLUME = 0.1;

    private static final long serialVersionUID = -152735180021558969L;

    private Tetris tetris;
    private Config config = new Config();
    private InMemoryStorage storage = new InMemoryStorage();
    private Records gameRecords;
    private boolean crazy;
    private int place;

    private Canvas canvas;
    private Canvas nextCanvas;

    private Button startGameBtn;
    private Button languageControlBtn;
    private ComboBox<String> gameTypeCB;
    private Label nextFigureLabel;
    private Label scoreLabel;
    private Label linesLabel;
    private Label levelLabel;
    private Label airLabel;
    private Label statusLabel;
    private Label pauseLabel;

    private Window pauseWindow;
    private Window recordsWindow;
    private About about;

    private Grid<Records.Rec> recordsGrid;
    private HorizontalLayout enterNameInnerContainer;
    private Label recordsLabel;
    private TextField nameTextField;
    private Button recordsButton;

    private Button dropBtn;

    private GameAudio music;
    private SoundMonitor soundMonitor;

    private double musicVolume;

    private TranslationProvider translationProvider = new TranslationProvider();

    private List<String> gameList;

    // TODO implement version
    // TODO separate services (AudioService, GameService, ...)
    //TODO docs
    //TODO tile size ???

    @Override
    protected void init(VaadinRequest request) {
        musicVolume = MUSIC_VOLUME;

        config = new Config();
        config.fps = FPS;

        // sorry, the sound slows down the gameplay
        soundMonitor = new SoundMonitor();            // создаём монитор звуковых эффектов
        soundMonitor.addSoundWithGain(FALLEN, new GameAudio(new ThemeResource("audio/fallen.wav")), 0.9, false);
        soundMonitor.addSoundWithGain(ROTATE, new GameAudio(new ThemeResource("audio/rotate.wav")), 0.9, false);
        soundMonitor.addSoundWithGain(CLICK, new GameAudio(new ThemeResource("audio/click.wav")), 1.0, false);
        soundMonitor.addSoundWithGain(HEARTBEAT_A, new GameAudio(new ThemeResource("audio/heartbeat-a.wav")), 0.8, true);
        soundMonitor.addSoundWithGain(HEARTBEAT_B, new GameAudio(new ThemeResource("audio/heartbeat-b.wav")), 0.9, true);

        storage = new InMemoryStorage();

        Locale locale = VaadinService.getCurrentRequest().getLocale();
        if (TranslationProvider.isUnknownLocale(locale)) {
            locale = TranslationProvider.getDefaultLocale();
        }
        TranslationProvider.setCurrentLanguage(TranslationProvider.languageByLocale(locale));

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        about = new About();
        layout.addComponent(about);

        // Button for moving left
        final Button leftBtn = new Button(VaadinIcons.ARROW_LEFT);
        leftBtn.addClickListener(e -> {
            tetris.processEvent(MOVE_LEFT);
            drawGameState();
        });
        leftBtn.setClickShortcut(KeyCode.ARROW_LEFT);

        // Button for moving right
        final Button rightBtn = new Button(VaadinIcons.ARROW_RIGHT);
        rightBtn.addClickListener(e -> {
            tetris.processEvent(MOVE_RIGHT);
            drawGameState();
        });
        rightBtn.setClickShortcut(KeyCode.ARROW_RIGHT);

        // Button for rotating clockwise
        final Button rotateCWBtn = new Button("[?]", VaadinIcons.ROTATE_RIGHT);
        rotateCWBtn.addClickListener(e -> {
            tetris.processEvent(ROTATE_LEFT);
            drawGameState();
        });
        //rotateCWBtn.setClickShortcut(KeyCode.ARROW_DOWN);

        // Button for rotating counter clockwise
        final Button rotateCCWBtn = new Button("[↑]", VaadinIcons.ROTATE_LEFT);
        rotateCCWBtn.addClickListener(e -> {
            tetris.processEvent(ROTATE_RIGHT);
            drawGameState();
        });
        rotateCCWBtn.setClickShortcut(KeyCode.ARROW_UP);

        // Button to accelerate lowering
        final Button stepBtn = new Button("[↓]", VaadinIcons.ARROW_LONG_DOWN);
        stepBtn.addClickListener(e -> {
            tetris.processEvent(STEP_DOWN);
            drawGameState();
        });
        stepBtn.setClickShortcut(KeyCode.ARROW_DOWN);

        // Button for dropping the piece
        dropBtn = new Button(translate("key.space"), VaadinIcons.ARROW_DOWN);
        dropBtn = new Button(VaadinIcons.ARROW_DOWN);
        dropBtn.addClickListener(e -> {
            tetris.processEvent(FALL_DOWN);
            drawGameState();
        });
        dropBtn.setClickShortcut(KeyCode.SPACEBAR);

        // Sound control button
        final Button soundControlBtn = new Button(VaadinIcons.SOUND_DISABLE);
        soundControlBtn.addClickListener(e -> {
            dropBtn.focus();
            if (music.isPlaying()) {
                music.stop();
                muteSound();
            } else {
                enableSound();
            }
        });
        soundControlBtn.setClickShortcut(KeyCode.S);

        // Language control button
        languageControlBtn = new Button();
        languageControlBtn.addClickListener(e -> {
            switchLanguage();
            dropBtn.focus();
        });
        languageControlBtn.setClickShortcut(KeyCode.L);

        gameTypeCB = new ComboBox<>();
        gameTypeCB.setEmptySelectionAllowed(false);

        // Button for restarting the game
        startGameBtn = new PrimaryButton().withIcon(VaadinIcons.PLAY);
        startGameBtn.addClickListener(e -> {
            if (null == tetris || tetris.getState() == GameState.FINISHED) {
                music.setShowControls(true);
                enableSound();
                crazy = gameList.indexOf(gameTypeCB.getSelectedItem().orElse(gameList.get(0))) != 0;
                int nextCanvasWidth = crazy ? 5 : 4;

                if (crazy) {
                    canvas.setWidth(config.tileWidth * config.crazyWidth + 1 + "px");
                    canvas.setHeight(config.tileHeight * config.crazyHeight + 1 + "px");
                } else {
                    canvas.setWidth(config.tileWidth * config.standardWidth + 1 + "px");
                    canvas.setHeight(config.tileHeight * config.standardHeight + 1 + "px");
                }
                nextCanvas.setWidth(config.tileWidth * nextCanvasWidth + 1 + "px");
                nextCanvas.setHeight(config.tileHeight * nextCanvasWidth + 1 + "px");

                tetris = new Tetris(config, crazy);
                Arrays.asList(REPAINT, UPDATE_SCORE, GAME_OVER).forEach(event -> tetris.addListener(event, this));
                Arrays.asList(PLAY_SOUND, START_LOOPING_SOUND, STOP_LOOPING_SOUND, FADE_LOOPING_SOUND, SUPPORT_LOOPING_SOUNDS)
                        .forEach(event -> tetris.addListener(event, soundMonitor));

                tetris.start();

                startGameBtn.setIcon(VaadinIcons.PAUSE);
                dropBtn.focus();
            } else {
                musicVolume = music.getVolume();
                music.fade();
                startGameBtn.setIcon(VaadinIcons.PLAY);
                muteSound();
                tetris.processEvent(PAUSE);
                addWindow(pauseWindow);
            }
        });
        startGameBtn.setClickShortcut(KeyCode.P);

        music = new GameAudio(new ThemeResource("audio/music.mp3"));
        music.setShowControls(false);
        music.setVolume(musicVolume);
        music.setLoop(true);

        layout.addComponent(new MHorizontalLayout(
                gameTypeCB, startGameBtn, leftBtn, rightBtn, rotateCCWBtn, rotateCWBtn, stepBtn, dropBtn, soundControlBtn, languageControlBtn
        ));

        // Label for score
        nextFigureLabel = new Label();
        scoreLabel = new Label();
        linesLabel = new Label();
        levelLabel = new Label();
        airLabel = new Label();
        statusLabel = new Label();

        nextCanvas = new Canvas();

        VerticalLayout verticalLayout =
                new VerticalLayout(nextFigureLabel, nextCanvas, scoreLabel, linesLabel, levelLabel, airLabel, statusLabel, music);

        canvas = new Canvas();

        layout.addComponent(new MHorizontalLayout(canvas, verticalLayout));

        soundMonitor.getChannels().values().forEach(layout::addComponents);

        generatePauseWindow();
        generateRecordsWindow();

        localizeInterface();
    }

    private void enableSound() {
        music.setVolume(musicVolume);
        music.play();
        if (tetris != null) {
            tetris.processEvent(ENABLE_SOUND);
        }
    }

    private void muteSound() {
        if (tetris != null) {
            tetris.processEvent(MUTE_SOUND);
        }
        soundMonitor.getChannels().values().forEach(GameAudio::stop);
    }

    @Override
    public synchronized void notify(GameEvent event, String message) {
        switch (event) {
            case REPAINT:
                drawGameState();
                updateStatistics();
                break;
            case UPDATE_SCORE:
                updateStatistics();
                break;
            case GAME_OVER:
                gameOver();
                break;
        }
    }

    /**
     * Update the score display.
     */
    private synchronized void updateStatistics() {
        access(() -> {
            nextFigureLabel.setValue(translate("next.figure"));
            scoreLabel.setValue(translate("score", tetris.getScore()));
            linesLabel.setValue(translate("lines", tetris.getLines()));
            levelLabel.setValue(translate("level", tetris.getLevel()));
            if (tetris.getCritter() != null) {
                airLabel.setValue(translate("air", (int) tetris.getCritter().getAir()));
                statusLabel.setValue(translate(tetris.getCritter().getStringStatus()));
            }
        });
    }

    /**
     * Quit the game.
     */
    private synchronized void gameOver() {
        if (recordsWindow.isAttached()) {
            return;
        }
        updateStatistics();
        startGameBtn.setIcon(VaadinIcons.PLAY);

        String cookieName = crazy ? "crazy.res" : "tet.res";
        storage.setRecordsStorageName(cookieName);
        gameRecords = new Records(storage);
        int score = tetris.getScore();
        place = gameRecords.getPlace(score);

        recordsGrid.setItems(gameRecords.getRecords());
        recordsGrid.setHeightByRows(gameRecords.getRecords().size() + 1);

        if (gameRecords.canAddNewRecord(place)) {
            switch (place) {
                case 1:
                    recordsLabel.setCaption(translate("records.label.first.place.text"));
                    break;
                case 2:
                    recordsLabel.setCaption(translate("records.label.second.place.text"));
                    break;
                case 3:
                    recordsLabel.setCaption(translate("records.label.third.place.text"));
                    break;
                default:
                    recordsLabel.setCaption(translate("records.label.other.place.text", score));
            }
            recordsButton.setCaption(translate("save.button.save.text"));
            if (enterNameInnerContainer.getComponentCount() == 1) {
                enterNameInnerContainer.addComponent(nameTextField);
            }
        } else {
            recordsLabel.setCaption(translate("records.label.no.place.text", score));
            if (enterNameInnerContainer.getComponentCount() == 2) {
                enterNameInnerContainer.removeComponent(nameTextField);
            }
            recordsButton.setCaption(translate("save.button.ok.text"));
        }
        addWindow(recordsWindow);
    }

    private synchronized void saveGame() {
        String name = nameTextField.getOptionalValue().orElse(translate("anonymous.name"));
        gameRecords.verifyAndAddScore(name, tetris.getScore());
        storage.saveRecord(gameRecords.getRecords());
    }

    /**
     * Draw the current game state.
     */
    private synchronized void drawGameState() {

        // Draw the state
        access(() -> {
            // Reset and clear canvas
            canvas.clear();
            canvas.setFillStyle(config.getWebColor(config.transparentColor));
            int tileWidth = config.tileWidth;
            int tileHeight = config.tileHeight;
            canvas.fillRect(0, 0, tetris.getWidth() * tileWidth + 2, tetris.getHeight() * tileHeight + 2);

            // Draw glass
            for (int x = 0; x < tetris.getWidth(); x++) {
                for (int y = 0; y < tetris.getHeight(); y++) {

                    int tileColor = tetris.getGlass().get(x, y);
                    if (tileColor != config.transparentColor) {
                        canvas.setFillStyle(config.getWebColor(tileColor));
                        canvas.fillRect(x * tileWidth + 1, y * tileHeight + 1, tileWidth - 2, tileHeight - 2);
                    } else {
                        canvas.setFillStyle(config.getWebColor(config.grayColor));
                        canvas.fillRect(x * tileWidth, y * tileHeight, 1, 1);
                    }
                }
            }

            Figure figure = tetris.getFigure();

            // Draw ghost figure
            figure.getCoordinates().forEach(c -> {
                int k = figure.getGhostTop();
                canvas.setFillStyle(config.getWebColor(figure.getColor(), 4));
                if (c.getY() + k >= 2) {
                    canvas.fillRect((c.getX() + figure.getLeft()) * tileWidth + 1, (c.getY() + k) * tileHeight + 1, tileWidth - 2, tileHeight - 2);
                }
            });

            // Draw figure
            figure.getCoordinates().forEach(c -> {
                canvas.setFillStyle(config.getWebColor(figure.getColor()));
                canvas.fillRect((c.getX() + figure.getLeft()) * tileWidth + 1, (c.getY() + figure.getTop()) * tileHeight + 1, tileWidth - 2, tileHeight - 2);
            });

            nextCanvas.clear();

            // Draw Next Figure
            Figure nextFigure = tetris.getNextFigure();
            int kx = nextFigure.getCoordinates().stream().map(Coordinate::getX).min(Integer::compare).orElse(0);
            int ky = nextFigure.getCoordinates().stream().map(Coordinate::getY).min(Integer::compare).orElse(0);

            for (Coordinate coordinate : nextFigure.getCoordinates()) {
                nextCanvas.setFillStyle(config.getWebColor(nextFigure.getColor(), 4));
                nextCanvas.fillRect((coordinate.getX() - kx) * tileWidth, (coordinate.getY() - ky) * tileHeight, tileWidth - 1, tileHeight - 1);

                nextCanvas.setFillStyle(config.getWebColor(nextFigure.getColor()));
                nextCanvas.fillRect((coordinate.getX() - kx) * tileWidth + 1, (coordinate.getY() - ky) * tileHeight + 1, tileWidth - 3, tileHeight - 3);
            }

            // Draw critter
            Critter critter = tetris.getCritter();
            if (critter != null && !critter.isDead()) {
                kx = (critter.getStatus() == CritterState.STAYING) || (critter.getAir() < 50) ? 0 : critter.getHorizontalDirection();

                String imageName = "happy";

                if (!critter.isBounded()) {
                    imageName = addDirection(imageName, kx);
                } else {
                    if (critter.getAir() < 2) {
                        imageName = "dead";
                    } else {
                        imageName = addDirection("sad", kx);
                    }
                }

                canvas.setFillStyle(config.getWebColor(config.critterColor));
                //canvas.fillRect(critter.getX() * tileWidth + 2, critter.getY() * tileHeight + 2, tileWidth - 3, tileHeight - 3);
                canvas.drawImage1(String.format("/VAADIN/themes/valo/img/%s.png", imageName), critter.getX() * tileWidth, critter.getY() * tileHeight);
            }
        });
    }

    private String addDirection(String imageName, int kx) {
        switch (kx) {
            case 1:
                return imageName + "-right";
            case -1:
                return imageName + "-left";
            default:
                return imageName;
        }
    }

    private void generatePauseWindow() {
        pauseWindow = getWindow();
        VerticalLayout content = new VerticalLayout();
        pauseWindow.setContent(content);
        pauseLabel = new Label();
        content.addComponent(pauseLabel);

        Button continueBtn = new Button(VaadinIcons.CLOSE);
        continueBtn.addClickListener(e -> {
            tetris.processEvent(CONTINUE);
            enableSound();
            startGameBtn.setIcon(VaadinIcons.PAUSE);
            pauseWindow.close();
            dropBtn.focus();
        });
        continueBtn.setClickShortcut(KeyCode.SPACEBAR);
        content.addComponent(continueBtn);
        content.setComponentAlignment(continueBtn, Alignment.TOP_CENTER);
    }

    private void generateRecordsWindow() {
        recordsWindow = getWindow();
        VerticalLayout content = new VerticalLayout();
        recordsWindow.setContent(content);

        recordsButton = new Button();
        recordsButton.addClickListener(e -> {
            if (gameRecords.canAddNewRecord(place)) {
                saveGame();
            }
            recordsWindow.close();
            dropBtn.focus();
        });
        recordsButton.setClickShortcut(KeyCode.SPACEBAR);

        recordsGrid = new Grid<>(Records.Rec.class);
        //recordsGrid.setColumns(translate("name"), translate("score"));

        recordsLabel = new Label();
        nameTextField = new TextField();
        enterNameInnerContainer = new HorizontalLayout();

        enterNameInnerContainer.addComponents(recordsLabel, nameTextField);

        content.addComponents(recordsGrid, enterNameInnerContainer, recordsButton);
        content.setComponentAlignment(recordsButton, Alignment.TOP_CENTER);
    }

    private Window getWindow() {
        Window window = new Window("");
        window.setClosable(false);
        window.setDraggable(false);
        window.setResizable(false);
        window.setModal(true);
        window.center();
        return window;
    }

    private void switchLanguage() {
        switch (LanguageProvider.getCurrentLanguage()) {
            case EN:
                LanguageProvider.setCurrentLanguage(Language.RU);
                break;
            case RU:
                LanguageProvider.setCurrentLanguage(Language.EN);
                break;
        }
        localizeInterface();
    }

    private void localizeInterface() {
        int index = (gameList == null) ? 0 : gameList.indexOf(gameTypeCB.getSelectedItem().orElse(gameList.get(0)));
        gameList = Arrays.asList(translate("classic.game.item.text"), translate("crazy.game.item.text"));
        gameTypeCB.setItems(gameList);
        gameTypeCB.setSelectedItem(gameList.get(index));

        recordsGrid.getDefaultHeaderRow().getCell("name").setText(translate("records.table.name.column"));
        recordsGrid.getDefaultHeaderRow().getCell("score").setText(translate("records.table.score.column"));

        dropBtn.setCaption(translate("key.space"));

        if (tetris != null) {
            updateStatistics();
        }

        about.setCaption(translate("about.caption"));

        pauseWindow.setCaption(translate("pause.window.caption"));
        pauseLabel.setCaption(translate("pause.label.text"));

        recordsWindow.setCaption(translate("records.window.caption"));

        String language = TranslationProvider.getCurrentLanguage().name().toLowerCase();
        languageControlBtn.setIcon(new ThemeResource(String.format("img/%s.png", language)));
        about.setContent(new RichText().withMarkDownResource(String.format("/about_%s.md", language)));
    }

    private String translate(String key, Object... params) {
        return translationProvider.getTranslation(key, params);
    }
}
