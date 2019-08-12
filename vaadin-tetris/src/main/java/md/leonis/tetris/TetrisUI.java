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
import md.leonis.tetris.engine.Critter;
import md.leonis.tetris.engine.Figure;
import md.leonis.tetris.engine.GuiInterface;
import md.leonis.tetris.engine.Records;
import md.leonis.tetris.engine.config.ColorConfig;
import md.leonis.tetris.engine.config.Config;
import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.CritterState;
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

@Push
@Theme("valo")
@Title("Vaadin Crazy Tetris")
public class TetrisUI extends UI implements GuiInterface {

    @WebServlet(value = {"/*"}, asyncSupported = true, initParams = {
            @WebInitParam(name = Constants.I18N_PROVIDER, value = "com.vaadin.example.ui.TranslationProvider")})
    @VaadinServletConfiguration(productionMode = false, ui = TetrisUI.class)
    public static class Servlet extends VaadinServlet {
    }

    private static final int FPS = 10;

    private static final long serialVersionUID = -152735180021558969L;

    private Canvas canvas = new Canvas();
    private Canvas nextCanvas = new Canvas();

    private Button startGameBtn;
    private Button languageControlBtn;
    private ComboBox<String> gameTypeCB = new ComboBox<>();
    private Label nextFigureLabel = new Label();
    private Label scoreLabel = new Label();
    private Label linesLabel = new Label();
    private Label levelLabel = new Label();
    private Label airLabel = new Label();
    private Label statusLabel = new Label();
    private Label pauseLabel = new Label();

    private Window pauseWindow;
    private Window recordsWindow;
    private About about = new About();

    private Grid<Records.Rec> recordsGrid;
    private HorizontalLayout enterNameInnerContainer;
    private Label recordsLabel = new Label();
    private TextField nameTextField;
    private Button recordsButton;

    private Button dropBtn;

    private List<String> gameList;

    private GameService gameService;

    private boolean musicPlaying = false;

    @Override
    protected void init(VaadinRequest request) {
        Config config = new Config(false);
        config.fps = FPS;

        gameService = new GameService(config);
        gameService.setGui(this);

        Locale locale = VaadinService.getCurrentRequest().getLocale();
        if (TranslationProvider.isUnknownLocale(locale)) {
            locale = TranslationProvider.getDefaultLocale();
        }
        TranslationProvider.setCurrentLanguage(TranslationProvider.languageByLocale(locale));

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        layout.addComponent(about);

        // Button for moving left
        final Button leftBtn = new Button(VaadinIcons.ARROW_LEFT);
        leftBtn.addClickListener(e -> {
            gameService.processEvent(MOVE_LEFT);
            repaint();
        });
        leftBtn.setClickShortcut(KeyCode.ARROW_LEFT);

        // Button for moving right
        final Button rightBtn = new Button(VaadinIcons.ARROW_RIGHT);
        rightBtn.addClickListener(e -> {
            gameService.processEvent(MOVE_RIGHT);
            repaint();
        });
        rightBtn.setClickShortcut(KeyCode.ARROW_RIGHT);

        // Button for rotating clockwise
        final Button rotateCWBtn = new Button("[?]", VaadinIcons.ROTATE_RIGHT);
        rotateCWBtn.addClickListener(e -> {
            gameService.processEvent(ROTATE_LEFT);
            repaint();
        });
        //rotateCWBtn.setClickShortcut(KeyCode.ARROW_DOWN);

        // Button for rotating counter clockwise
        final Button rotateCCWBtn = new Button("[↑]", VaadinIcons.ROTATE_LEFT);
        rotateCCWBtn.addClickListener(e -> {
            gameService.processEvent(ROTATE_RIGHT);
            repaint();
        });
        rotateCCWBtn.setClickShortcut(KeyCode.ARROW_UP);

        // Button to accelerate lowering
        final Button stepBtn = new Button("[↓]", VaadinIcons.ARROW_LONG_DOWN);
        stepBtn.addClickListener(e -> {
            gameService.processEvent(STEP_DOWN);
            repaint();
        });
        stepBtn.setClickShortcut(KeyCode.ARROW_DOWN);

        // Button for dropping the piece
        dropBtn = new Button(gameService.translate("key.space"), VaadinIcons.ARROW_DOWN);
        dropBtn = new Button(VaadinIcons.ARROW_DOWN);
        dropBtn.addClickListener(e -> {
            gameService.processEvent(FALL_DOWN);
            repaint();
        });
        dropBtn.setClickShortcut(KeyCode.SPACEBAR);

        // Sound control button
        final Button soundControlBtn = new Button(VaadinIcons.SOUND_DISABLE);
        soundControlBtn.addClickListener(e -> {
            dropBtn.focus();
            if (musicPlaying) {
                muteSound();
            } else {
                enableSound();
            }
        });
        soundControlBtn.setClickShortcut(KeyCode.S);

        // Language control button
        languageControlBtn = new Button();
        languageControlBtn.addClickListener(e -> {
            gameService.switchLanguage();
            localizeInterface();
            dropBtn.focus();
        });
        languageControlBtn.setClickShortcut(KeyCode.L);

        // Level control button
        Button levelControlBtn = new Button(VaadinIcons.LEVEL_UP);
        levelControlBtn.addClickListener(e -> {
            gameService.processEvent(NEXT_LEVEL);
            repaint();
            dropBtn.focus();
        });
        levelControlBtn.setClickShortcut(KeyCode.L);

        gameTypeCB.setEmptySelectionAllowed(false);

        // Button for restarting the game
        startGameBtn = new PrimaryButton().withIcon(VaadinIcons.PLAY);
        startGameBtn.addClickListener(e -> {
            if (gameService.isFinished()) {
                gameService.getMusic().setShowControls(true);
                enableSound();
                boolean crazy = gameList.indexOf(gameTypeCB.getSelectedItem().orElse(gameList.get(0))) != 0;

                if (crazy) {
                    canvas.setWidth(config.tileWidth * config.crazyWidth + 1 + "px");
                    canvas.setHeight(config.tileHeight * config.crazyHeight + 1 + "px");
                } else {
                    canvas.setWidth(config.tileWidth * config.standardWidth + 1 + "px");
                    canvas.setHeight(config.tileHeight * config.standardHeight + 1 + "px");
                }
                int nextCanvasWidth = crazy ? 5 : 4;
                nextCanvas.setWidth(config.tileWidth * nextCanvasWidth + 1 + "px");
                nextCanvas.setHeight(config.tileHeight * nextCanvasWidth + 1 + "px");

                gameService.startGame(crazy);

                startGameBtn.setIcon(VaadinIcons.PAUSE);
                dropBtn.focus();
            } else {
                fadeSound();
                startGameBtn.setIcon(VaadinIcons.PLAY);
                gameService.pauseGame(true);
                addWindow(pauseWindow);
            }
        });
        startGameBtn.setClickShortcut(KeyCode.P);

        layout.addComponent(new MHorizontalLayout(
                gameTypeCB, startGameBtn, leftBtn, rightBtn, rotateCCWBtn, rotateCWBtn, stepBtn, dropBtn,
                soundControlBtn, languageControlBtn, levelControlBtn
        ));

        VerticalLayout verticalLayout =
                new VerticalLayout(nextFigureLabel, nextCanvas, scoreLabel, linesLabel, levelLabel, airLabel, statusLabel, gameService.getMusic());

        layout.addComponent(new MHorizontalLayout(canvas, verticalLayout));

        gameService.getSoundMonitor().getChannels().values().forEach(layout::addComponents);

        generatePauseWindow();
        generateRecordsWindow();

        localizeInterface();
    }

    /**
     * Update the score display.
     */
    public synchronized void updateStatistics() {
        access(() -> {
            nextFigureLabel.setValue(gameService.translate("next.figure"));
            scoreLabel.setValue(gameService.translate("score", gameService.getGameScore().getScore()));
            linesLabel.setValue(gameService.translate("lines", gameService.getGameScore().getLines()));
            levelLabel.setValue(gameService.translate("level", gameService.getGameScore().getLevel()));
            if (gameService.getCritter() != null) {
                airLabel.setValue(gameService.translate("air", (int) gameService.getCritter().getAir()));
                statusLabel.setValue(gameService.translate(gameService.getCritter().getStringStatus()));
            }
        });
    }

    @Override
    public synchronized void repaint() {
        // Draw the state
        access(() -> {
            // Reset and clear canvas
            ColorConfig config = gameService.getConfig().getColorConfig();
            canvas.clear();
            canvas.setFillStyle(config.getWebColor(config.transparentColor));
            int tileWidth = gameService.getTileWidth();
            int tileHeight = gameService.getTileHeight();
            canvas.fillRect(0, 0, gameService.getWidth() * tileWidth + 2, gameService.getHeight() * tileHeight + 2);

            // Draw glass
            for (int x = 0; x < gameService.getWidth(); x++) {
                for (int y = 0; y < gameService.getHeight(); y++) {

                    int tileColor = gameService.getGlass().get(x, y);
                    if (tileColor != config.transparentColor) {
                        canvas.setFillStyle(config.getWebColor(tileColor));
                        canvas.fillRect(x * tileWidth + 1, y * tileHeight + 1, tileWidth - 2, tileHeight - 2);
                    } else {
                        canvas.setFillStyle(config.getWebColor(config.grayColor));
                        canvas.fillRect(x * tileWidth, y * tileHeight, 1, 1);
                    }
                }
            }

            Figure figure = gameService.getFigure();

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
            Figure nextFigure = gameService.getNextFigure();
            int kx = nextFigure.getCoordinates().stream().map(Coordinate::getX).min(Integer::compare).orElse(0);
            int ky = nextFigure.getCoordinates().stream().map(Coordinate::getY).min(Integer::compare).orElse(0);

            for (Coordinate coordinate : nextFigure.getCoordinates()) {
                nextCanvas.setFillStyle(config.getWebColor(nextFigure.getColor(), 4));
                nextCanvas.fillRect((coordinate.getX() - kx) * tileWidth, (coordinate.getY() - ky) * tileHeight, tileWidth - 1, tileHeight - 1);

                nextCanvas.setFillStyle(config.getWebColor(nextFigure.getColor()));
                nextCanvas.fillRect((coordinate.getX() - kx) * tileWidth + 1, (coordinate.getY() - ky) * tileHeight + 1, tileWidth - 3, tileHeight - 3);
            }

            // Draw critter
            Critter critter = gameService.getCritter();
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

    /**
     * Quit the game.
     */
    public synchronized void gameOver() {
        if (recordsWindow.isAttached()) {
            return;
        }
        updateStatistics();
        startGameBtn.setIcon(VaadinIcons.PLAY);

        Records gameRecords = gameService.initializeRecords();
        int place = gameRecords.getPlace();

        recordsGrid.setItems(gameRecords.getRecords());
        recordsGrid.setHeightByRows(gameRecords.getRecords().size() + 1);

        if (gameRecords.canAddNewRecord()) {
            switch (place) {
                case 1:
                    recordsLabel.setCaption(gameService.translate("records.label.first.place.text"));
                    break;
                case 2:
                    recordsLabel.setCaption(gameService.translate("records.label.second.place.text"));
                    break;
                case 3:
                    recordsLabel.setCaption(gameService.translate("records.label.third.place.text"));
                    break;
                default:
                    recordsLabel.setCaption(gameService.translate("records.label.other.place.text", gameRecords.getScore()));
            }
            recordsButton.setCaption(gameService.translate("save.button.save.text"));
            if (enterNameInnerContainer.getComponentCount() == 1) {
                enterNameInnerContainer.addComponent(nameTextField);
            }
        } else {
            recordsLabel.setCaption(gameService.translate("records.label.no.place.text", gameRecords.getScore()));
            if (enterNameInnerContainer.getComponentCount() == 2) {
                enterNameInnerContainer.removeComponent(nameTextField);
            }
            recordsButton.setCaption(gameService.translate("save.button.ok.text"));
        }
        addWindow(recordsWindow);
    }

    private synchronized void saveGame() {
        gameService.saveRecord(nameTextField.getOptionalValue().orElse(gameService.translate("anonymous.name")));
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
            gameService.pauseGame(false);
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
            if (gameService.getGameRecords().canAddNewRecord()) {
                saveGame();
            }
            recordsWindow.close();
            dropBtn.focus();
        });
        recordsButton.setClickShortcut(KeyCode.SPACEBAR);

        recordsGrid = new Grid<>(Records.Rec.class);

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

    private void enableSound() {
        musicPlaying = true;
        gameService.playMusic();
    }

    private void fadeSound() {
        musicPlaying = false;
        gameService.fadeMusic();
    }

    private void muteSound() {
        musicPlaying = false;
        gameService.stopMusic();
    }

    private void localizeInterface() {
        int index = (gameList == null) ? 0 : gameList.indexOf(gameTypeCB.getSelectedItem().orElse(gameList.get(0)));
        gameList = Arrays.asList(gameService.translate("classic.game.item.text"), gameService.translate("crazy.game.item.text"));
        gameTypeCB.setItems(gameList);
        gameTypeCB.setSelectedItem(gameList.get(index));

        recordsGrid.getDefaultHeaderRow().getCell("name").setText(gameService.translate("records.table.name.column"));
        recordsGrid.getDefaultHeaderRow().getCell("score").setText(gameService.translate("records.table.score.column"));

        dropBtn.setCaption(gameService.translate("key.space"));

        if (gameService.isInitialized()) {
            updateStatistics();
        }

        about.setCaption(gameService.translate("about.caption"));

        pauseWindow.setCaption(gameService.translate("pause.window.caption"));
        pauseLabel.setCaption(gameService.translate("pause.label.text"));

        recordsWindow.setCaption(gameService.translate("records.window.caption"));

        String language = TranslationProvider.getCurrentLanguage().name().toLowerCase();
        languageControlBtn.setIcon(new ThemeResource(String.format("img/%s.png", language)));
        about.setContent(new RichText().withMarkDownResource(String.format("/about_%s.md", language)));
    }
}
