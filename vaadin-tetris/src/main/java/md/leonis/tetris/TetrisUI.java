package md.leonis.tetris;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
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
import org.vaadin.hezamu.canvas.Canvas;
import org.vaadin.viritin.button.PrimaryButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;

import javax.servlet.annotation.WebServlet;
import java.util.Arrays;

import static md.leonis.tetris.engine.event.GameEvent.*;
import static md.leonis.tetris.engine.model.SoundId.*;

@Push
@Theme("valo")
@Title("Vaadin Crazy Tetris")
public class TetrisUI extends UI implements GameEventListener {

    @WebServlet(value = {"/*"}, asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = TetrisUI.class)
    public static class Servlet extends VaadinServlet {
    }

    private static final double MUSIC_VOLUME = 0.1;

    private static final int PAUSE_TIME_MS = 500;

    private static final long serialVersionUID = -152735180021558969L;

    private Canvas canvas;
    private Canvas nextCanvas;

    private Config config;
    private Tetris tetris;
    private InMemoryStorage storage;
    private Records gameRecords;
    private boolean crazy;
    private int place;

    private Button startGameBtn;
    private ComboBox<String> gameTypeCB;
    private Label nextFigureLabel;
    private Label scoreLabel;
    private Label linesLabel;
    private Label levelLabel;
    private Label airLabel;
    private Label statusLabel;

    private Window pauseWindow;

    private Window recordsWindow;
    private Grid<Records.Rec> recordsGrid;
    private HorizontalLayout enterNameInnerContainer;
    private Label recordsLabel;
    private TextField nameTextField;
    private Button recordsButton;

    private Button dropBtn;

    private GameAudio music;
    private SoundMonitor soundMonitor;

    private double musicVolume;

    // TODO audio
    // TODO separate services (AudioService, GameService, ...)
    // https://vaadin.com/forum/thread/3355253/audio
    // https://vaadin.com/components/vaadin-icons/java-examples
    //TODO docs
    //TODO tile size

    @Override
    protected void init(VaadinRequest request) {

        musicVolume = MUSIC_VOLUME;

        config = new Config();

        // sorry, the sound slows down the gameplay
        soundMonitor = new SoundMonitor();            // создаём монитор звуковых эффектов
        soundMonitor.addSoundWithGain(FALLED, new GameAudio(new ThemeResource("audio/falled.wav")), 0.9, false);
        soundMonitor.addSoundWithGain(ROTATE, new GameAudio(new ThemeResource("audio/rotate.wav")), 0.9, false);
        soundMonitor.addSoundWithGain(CLICK, new GameAudio(new ThemeResource("audio/click.wav")), 1.0, false);
        soundMonitor.addSoundWithGain(HEARTBEAT_A, new GameAudio(new ThemeResource("audio/heartbeat-a.wav")), 0.8, true);
        soundMonitor.addSoundWithGain(HEARTBEAT_B, new GameAudio(new ThemeResource("audio/heartbeat-b.wav")), 0.9, true);

        storage = new InMemoryStorage();

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        layout.addComponent(new About());

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
        final Button rotateCWBtn = new Button("[key ???]", VaadinIcons.ROTATE_RIGHT);
        rotateCWBtn.addClickListener(e -> {
            tetris.processEvent(ROTATE_LEFT);
            drawGameState();
        });
        //rotateCWBtn.setClickShortcut(KeyCode.ARROW_DOWN);

        // Button for rotating counter clockwise
        final Button rotateCCWBtn = new Button("[key up]", VaadinIcons.ROTATE_LEFT);
        rotateCCWBtn.addClickListener(e -> {
            tetris.processEvent(ROTATE_RIGHT);
            drawGameState();
        });
        rotateCCWBtn.setClickShortcut(KeyCode.ARROW_UP);

        // Button to accelerate lowering
        final Button stepBtn = new Button("[key down]", VaadinIcons.ARROW_LONG_DOWN);
        stepBtn.addClickListener(e -> {
            tetris.processEvent(STEP_DOWN);
            drawGameState();
        });
        stepBtn.setClickShortcut(KeyCode.ARROW_DOWN);

        // Button for dropping the piece
        dropBtn = new Button("[space]", VaadinIcons.ARROW_DOWN);
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

        gameTypeCB = new ComboBox<>();
        gameTypeCB.setItems("Game A", "Game B");
        gameTypeCB.setSelectedItem("Game A");
        gameTypeCB.setEmptySelectionAllowed(false);

        // Button for restarting the game
        startGameBtn = new PrimaryButton().withIcon(VaadinIcons.PLAY);
        startGameBtn.addClickListener(e -> {
            if (null == tetris || tetris.getState() == GameState.FINISHED) {
                music.setShowControls(true);
                enableSound();
                crazy = gameTypeCB.getSelectedItem().orElse("").endsWith("B");
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
                startGameThread();

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
                gameTypeCB, startGameBtn, leftBtn, rightBtn, rotateCCWBtn, rotateCWBtn, stepBtn, dropBtn, soundControlBtn
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

    /**
     * Start the game thread that updates the game periodically.
     */
    private synchronized void startGameThread() {
        Thread t = new Thread(() -> {
            while (!(tetris.getState() == GameState.FINISHED)) {
                drawGameState();
                try {
                    Thread.sleep(PAUSE_TIME_MS);
                } catch (InterruptedException ignored) {
                }
            }
            gameOver();
        });
        t.start();
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
        }
    }

    /**
     * Update the score display.
     */
    private synchronized void updateStatistics() {
        access(() -> {
            nextFigureLabel.setValue("Next figure:");
            scoreLabel.setValue("Score: " + tetris.getScore());
            linesLabel.setValue("Lines: " + tetris.getLines());
            levelLabel.setValue("Level: " + tetris.getLevel());
            if (tetris.getCritter() != null) {
                airLabel.setValue("Air: " + (int) tetris.getCritter().getAir() + "%");
                statusLabel.setValue(tetris.getCritter().getStringStatus());
            }
        });
    }

    /**
     * Quit the game.
     */
    private synchronized void gameOver() {
        tetris.processEvent(GAME_OVER);
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
                    recordsLabel.setCaption("First place. Your name: ");
                    break;
                case 2:
                    recordsLabel.setCaption("Second place. Your name: ");
                    break;
                case 3:
                    recordsLabel.setCaption("Third place. Your name: ");
                    break;
                default:
                    recordsLabel.setCaption(score + " points! Your name: ");
            }
            recordsButton.setCaption("Save");
            if (enterNameInnerContainer.getComponentCount() == 1) {
                enterNameInnerContainer.addComponent(nameTextField);
            }
        } else {
            recordsLabel.setCaption("Scored " + score + " points. Not enough for a new record!");
            if (enterNameInnerContainer.getComponentCount() == 2) {
                enterNameInnerContainer.removeComponent(nameTextField);
            }
            recordsButton.setCaption("OK");
        }
        access(() -> addWindow(recordsWindow));
    }

    private synchronized void saveGame() {
        String name = nameTextField.getOptionalValue().orElse("Captain Nemo");
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
                        // Sorry, very CPU consuming operation :(
                        //roundRect(canvas, x * tileWidth + 1, y * tileHeight + 1, tileHeight - 1, tileWidth - 1, tileWidth / 4.0, true);
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

    private void roundRect(Canvas canvas, double x, double y, double width, double height, double radius, boolean fill) {
        canvas.beginPath();
        canvas.moveTo(x + radius, y);
        canvas.lineTo(x + width - radius, y);
        canvas.quadraticCurveTo(x + width, y, x + width, y + radius);
        canvas.lineTo(x + width, y + height - radius);
        canvas.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
        canvas.lineTo(x + radius, y + height);
        canvas.quadraticCurveTo(x, y + height, x, y + height - radius);
        canvas.lineTo(x, y + radius);
        canvas.quadraticCurveTo(x, y, x + radius, y);
        canvas.closePath();
        if (fill) {
            canvas.fill();
        } else {
            canvas.stroke();
        }
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
        pauseWindow = getWindow("Game paused");
        VerticalLayout content = new VerticalLayout();
        pauseWindow.setContent(content);
        content.addComponent(new Label("Press button to continue"));

        Button continueBtn = new Button(VaadinIcons.CLOSE);
        continueBtn.addClickListener(e -> {
            enableSound();
            tetris.processEvent(CONTINUE);
            startGameBtn.setIcon(VaadinIcons.PAUSE);
            pauseWindow.close();
            dropBtn.focus();
        });
        continueBtn.setClickShortcut(KeyCode.SPACEBAR);
        content.addComponent(continueBtn);
        content.setComponentAlignment(continueBtn, Alignment.TOP_CENTER);
    }

    private void generateRecordsWindow() {
        recordsWindow = getWindow("Game Over");
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
        recordsGrid.setColumns("name", "score");

        recordsLabel = new Label();
        nameTextField = new TextField();
        enterNameInnerContainer = new HorizontalLayout();

        enterNameInnerContainer.addComponents(recordsLabel, nameTextField);

        content.addComponents(recordsGrid, enterNameInnerContainer, recordsButton);
        content.setComponentAlignment(recordsButton, Alignment.TOP_CENTER);
    }

    private Window getWindow(String caption) {
        Window window = new Window(caption);
        window.setClosable(false);
        window.setDraggable(false);
        window.setResizable(false);
        window.setModal(true);
        window.center();
        return window;
    }
}
