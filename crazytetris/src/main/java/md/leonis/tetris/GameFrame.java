package md.leonis.tetris;

import md.leonis.tetris.engine.*;
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.GameEventListener;
import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.CritterState;
import md.leonis.tetris.engine.model.Language;
import md.leonis.tetris.sound.MusicChannel;
import md.leonis.tetris.sound.SoundMonitor;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.logging.Logger;

import static md.leonis.tetris.FileSystemStorage.getResourceAsStream;
import static md.leonis.tetris.engine.event.GameEvent.*;
import static md.leonis.tetris.engine.model.GameState.PAUSED;
import static md.leonis.tetris.engine.model.GameState.RUNNING;
import static md.leonis.tetris.engine.model.SoundId.*;

class GameFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger("GameFrame");

    private JPanel startPanel = new JPanel();
    private JButton startClassicGameButton = new JButton();
    private JButton startCrazyGameButton = new JButton();
    private JButton exitButton = new JButton();
    private JButton languageButton = new JButton();

    private JPanel pausePanel = new JPanel();
    private JLabel pauseLabel = new JLabel();
    private JButton continueButton = new JButton();

    private JPanel recordPanel = new JPanel();
    private DefaultTableModel recordsTable = new DefaultTableModel();
    private JButton saveButton = new JButton();
    private JLabel saveLabel = new JLabel();
    private JTextField nameTextField;

    private GamePanel gamePanel = new GamePanel();

    private Image backgroundImage, titleImage, currentBackgroundImage, ruImage, enImage;

    private Config config = new Config();
    private LanguageProvider languageProvider = new LanguageProvider(Language.RU);

    private EventMapper eventMapper = new EventMapper();
    private StorageInterface storage = new FileSystemStorage();
    private MusicChannel musicChannel;
    private SoundMonitor soundMonitor;

    private Tetris tetris;
    private Records gameRecords;

    private boolean crazy = false;

    GameFrame(String title, boolean isDebug) {
        musicChannel = new MusicChannel(getResourceAsStream("audio/music.mp3", isDebug));

        soundMonitor = new SoundMonitor();
        soundMonitor.addSoundWithGain(FALLEN, getResourceAsStream("audio/fallen.wav", isDebug), 0.9f);
        soundMonitor.addSoundWithGain(ROTATE, getResourceAsStream("audio/rotate.wav", isDebug), 0.9f);
        soundMonitor.addSoundWithGain(CLICK, getResourceAsStream("audio/click.wav", isDebug), 1.0f);
        soundMonitor.addSoundWithGain(HEARTBEAT_A, getResourceAsStream("audio/heartbeat-a.wav", isDebug), 0.8f);
        soundMonitor.addSoundWithGain(HEARTBEAT_B, getResourceAsStream("audio/heartbeat-b.wav", isDebug), 0.9f);

        try {
            backgroundImage = ImageIO.read(getResourceAsStream("bg.jpg", isDebug));
            titleImage = ImageIO.read(getResourceAsStream("title.jpg", isDebug));
            ruImage = ImageIO.read(getResourceAsStream("ru.png", isDebug));
            enImage = ImageIO.read(getResourceAsStream("en.png", isDebug));
        } catch (IOException e) {
            LOGGER.warning("Can't load background images!");
        }
        currentBackgroundImage = titleImage;

        setSize(config.windowWidth, config.windowHeight); // Window size
        createGUI(title, new EventsMonitor());

        if (config.soundOn) {
            musicChannel.play();
        }

        startClassicGameButton.requestFocus();
    }

    private void createGUI(String title, EventsMonitor eventsMonitor) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close window on exit
        setTitle(title);

        setLocationRelativeTo(null);

        // Start Game
        startClassicGameButton.addActionListener(eventsMonitor);
        startClassicGameButton.setActionCommand(START_GAME_A.name());

        startCrazyGameButton.addActionListener(eventsMonitor);
        startCrazyGameButton.setActionCommand(START_GAME_B.name());

        exitButton.setActionCommand(EXIT.name());
        exitButton.addActionListener(eventsMonitor);

        JPanel startPanelButtonsContainer = new JPanel();
        startPanelButtonsContainer.setBorder(BorderFactory.createRaisedBevelBorder());
        startPanelButtonsContainer.setLayout((new GridLayout(3, 1)));
        startPanelButtonsContainer.add(startClassicGameButton);
        startPanelButtonsContainer.add(startCrazyGameButton);
        startPanelButtonsContainer.add(exitButton);

        languageButton.addActionListener(eventsMonitor);
        languageButton.setActionCommand(CHANGE_LANGUAGE.name());

        JPanel languagePanel = new JPanel();
        languagePanel.setBackground(new Color(0, 0, 0, 0));
        languagePanel.add(languageButton);

        startPanel.setLayout(new BorderLayout());
        startPanel.setBackground(new Color(0, 0, 0, 0));
        startPanel.add(startPanelButtonsContainer, BorderLayout.CENTER);
        startPanel.add(languagePanel, BorderLayout.SOUTH);

        // Pause
        continueButton.addActionListener(eventsMonitor);
        continueButton.addKeyListener(eventsMonitor);
        continueButton.setActionCommand(CONTINUE.name());
        JPanel continuePanel = new JPanel();
        continuePanel.add(continueButton);

        pausePanel.setBorder(BorderFactory.createRaisedBevelBorder());
        pausePanel.setLayout(new GridLayout(2, 1));
        pausePanel.add(pauseLabel);
        pausePanel.add(continuePanel);

        // Game Over/Records
        JPanel newRecordPanel = new JPanel();
        newRecordPanel.add(saveLabel); // "New record! Your name: "
        nameTextField = new JTextField("", 16);
        newRecordPanel.add(nameTextField);

        saveButton.setActionCommand(SAVE.name());
        saveButton.addActionListener(eventsMonitor);

        JPanel savePanel = new JPanel();
        savePanel.add(saveButton);

        JTable table = new JTable(recordsTable);

        recordPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        recordPanel.setLayout(new BoxLayout(recordPanel, BoxLayout.Y_AXIS));
        recordPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
        recordPanel.add(table);
        recordPanel.add(newRecordPanel);
        recordPanel.add(savePanel);

        // Main panel (container)
        gamePanel.setFocusable(true);
        gamePanel.add(startPanel);
        gamePanel.add(pausePanel);
        gamePanel.add(recordPanel);
        gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3)); // horizontal alignment: middle

        this.add(gamePanel);

        localizeInterface();

        // We will switch these three panels: Start Game, Pause, Game Over/Records
        startPanel.setVisible(true);
        pausePanel.setVisible(false);
        recordPanel.setVisible(false);

        this.setVisible(true);
    }

    class GamePanel extends JPanel {

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage frameBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = (Graphics2D) frameBuffer.getGraphics();
            g2d.drawImage(currentBackgroundImage, 0, 0, null);

            if (tetris != null && tetris.isInitialized()) {
                drawPlayField(frameBuffer.createGraphics());
            } else {
                drawCopyright(g2d);
            }

            g2d.dispose();
            g.drawImage(frameBuffer, 0, 0, this);
        }

        void drawCopyright(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setColor(Color.black);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2d.drawString("© Leonis, 2015-" + LocalDate.now().getYear(), getWidth() - 117, getHeight() - 50);
        }

        void drawPlayField(Graphics2D g2d) {
            // Draw a glass
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.translate(10, 10);
            g2d.setColor(config.getColor(config.transparentColor));

            int width = tetris.getWidth();
            int height = tetris.getHeight();
            int tileWidth = config.tileWidth;
            int tileHeight = config.tileHeight;

            g2d.fillRect(0, 0, width * tileWidth, (height - 2) * tileHeight);
            g2d.setColor(config.getColor(config.grayColor));
            for (int i = 1; i < width; i++) {
                for (int j = 3; j < height; j++) {
                    g2d.drawRect(i * tileWidth, (j - 2) * tileHeight, 0, 0);
                }
            }

            for (int i = 0; i < width; i++) {
                for (int j = 2; j < height; j++) {
                    g2d.setColor(config.getColor(tetris.getGlass().get(i, j)));
                    g2d.fillRoundRect(i * tileWidth, (j - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
                }
            }

            // Show score, lines, other statistics
            int leftPos = width * tileWidth + 7;
            g2d.setColor(config.getColor(config.transparentColor));
            g2d.drawString(translate("score", tetris.getScore()), leftPos, 10);
            g2d.drawString(translate("lines", tetris.getLines()), leftPos, 30);
            g2d.drawString(translate("level", tetris.getLevel()), leftPos, 50);
            Critter critter = tetris.getCritter();
            if (critter != null) {
                g2d.drawString(translate("air", (int) critter.getAir()), leftPos, 70);
                g2d.drawString(translate(critter.getStringStatus()), leftPos, 90);
            }

            // Draw figure
            Figure figure = tetris.getFigure();
            for (Coordinate coordinate : figure.getCoordinates()) {
                int k = figure.getGhostTop();
                g2d.setColor(config.getColor(figure.getColor(), 4));
                if (coordinate.getY() + k >= 2) {
                    g2d.fillRoundRect((coordinate.getX() + figure.getLeft()) * tileWidth, (coordinate.getY() + k - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
                }
            }

            Figure nextFigure = tetris.getNextFigure();
            int kx = nextFigure.getCoordinates().stream().map(Coordinate::getX).min(Integer::compare).orElse(0);
            int ky = nextFigure.getCoordinates().stream().map(Coordinate::getY).min(Integer::compare).orElse(0);

            for (Coordinate coordinate : nextFigure.getCoordinates()) {
                g2d.setColor(config.getColor(figure.getColor(), 4));
                g2d.fillRoundRect((coordinate.getX() - kx) * tileWidth + leftPos + 1, (coordinate.getY() - ky) * tileHeight + 100 + 1, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);

                g2d.setColor(config.getColor(nextFigure.getColor()));
                g2d.fillRoundRect((coordinate.getX() - kx) * tileWidth + leftPos, (coordinate.getY() - ky) * tileHeight + 100, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
            }

            for (Coordinate coordinate : figure.getCoordinates()) {
                g2d.setColor(config.getColor(figure.getColor()));
                if ((coordinate.getY() + figure.getTop()) >= 2)
                    g2d.fillRoundRect((coordinate.getX() + figure.getLeft()) * tileWidth, (coordinate.getY() + figure.getTop() - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
            }

            // Draw critter
            if (critter != null) {
                if (critter.getStatus() != CritterState.DEAD) {
                    g2d.setColor(config.getColor(config.critterColor));
                    g2d.drawOval(critter.getX() * tileWidth, (critter.getY() - 2) * tileHeight, tileWidth, tileHeight);
                    kx = critter.getHorizontalDirection() * 2;
                    ky = 0;
                    if (critter.getStatus() == CritterState.FALLING) ky = 1;
                    if (critter.getStatus() == CritterState.JUMPING) ky = -1;
                    if (critter.getStatus() == CritterState.STAYING) kx = 0;
                    // eyes
                    g2d.drawArc(critter.getX() * tileWidth + 7 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
                    g2d.drawArc(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
                    // eyes
                    if (critter.getAir() < 50) {
                        g2d.drawRect(critter.getX() * tileWidth + 7 + kx + 1, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
                        g2d.drawRect(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
                    }
                    // mouth
                    int wx = critter.isBounded() ? 2 : 6;

                    if ((critter.getAir() > 75) && !critter.isBounded()) {
                        g2d.drawArc(critter.getX() * tileWidth + 7 + kx - 1, (critter.getY() - 2) * tileHeight + 14 - 3, wx + 2, 3, 0, -180);
                    } else
                        g2d.drawRect(critter.getX() * tileWidth + 7 + kx + (6 - wx) / 2, (critter.getY() - 2) * tileHeight + 14, wx, 0);
                }
            }
        }
    }

    class EventsMonitor implements ActionListener, EventListener, KeyListener, GameEventListener {

        @Override // Swing actions
        public void actionPerformed(ActionEvent e) {
            processEvent(GameEvent.valueOf(e.getActionCommand()));
        }

        @Override // Swing key actions
        public void keyPressed(KeyEvent e) {
            if (e.isConsumed()) {
                return;
            }

            GameEvent event = eventMapper.map(e.getKeyCode());
            tetris.processEvent(event);
            this.processEvent(event);

            e.consume();
        }

        @Override // Unused
        public void keyReleased(KeyEvent e) {
        }

        @Override // Unused
        public void keyTyped(KeyEvent e) {
        }

        @Override // Internal "native" events
        public void notify(GameEvent event, String message) {
            processEvent(event);
        }

        private void processEvent(GameEvent event) {
            switch (event) {
                case START_GAME:
                    startGame();
                    break;

                case START_GAME_A:
                    crazy = false;
                    startGame();
                    break;

                case START_GAME_B:
                    crazy = true;
                    startGame();
                    break;

                case REPAINT:
                    gamePanel.repaint();
                    break;

                case CHANGE_LANGUAGE:
                    switchLanguage();
                    break;

                case PAUSE:
                    pauseGame();
                    break;

                case CONTINUE:
                    continueGame();
                    break;

                case GAME_OVER:
                    gameOver();
                    break;

                case SAVE:
                    saveGame();
                    break;

                case EXIT:
                    if (tetris != null) {
                        tetris.processEvent(GAME_OVER);
                    }
                    System.exit(0);
            }
            gamePanel.repaint();
        }

        private void startGame() {
            currentBackgroundImage = backgroundImage;
            musicChannel.stop();
            startPanel.setVisible(false);
            tetris = new Tetris(config, crazy);
            Arrays.asList(REPAINT, GAME_OVER).forEach(e -> tetris.addListener(e, this));

            Arrays.asList(PLAY_SOUND, START_LOOPING_SOUND, STOP_LOOPING_SOUND, FADE_LOOPING_SOUND, SUPPORT_LOOPING_SOUNDS)
                    .forEach(e -> tetris.addListener(e, soundMonitor));

            tetris.start();
            gamePanel.addKeyListener(this);
        }

        private void pauseGame() {
            if (tetris.getState() == PAUSED) {
                tetris.processEvent(PAUSE);
                gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3)); // horizontal alignment: middle
                pausePanel.setVisible(true);
                continueButton.requestFocus();
                gamePanel.repaint();
            } else if (tetris.getState() == RUNNING) {
                continueGame();
            }
        }

        private void continueGame() {
            pausePanel.setVisible(false);
            tetris.processEvent(CONTINUE);
            gamePanel.repaint();
        }

        private void gameOver() {
            gamePanel.removeKeyListener(this);
            String fileName = crazy ? "crazy.res" : "tet.res";
            storage.setRecordsStorageName(fileName);
            gameRecords = new Records(storage);
            int place = gameRecords.getPlace(tetris.getScore());

            if (gameRecords.canAddNewRecord(place)) {
                fillModel(gameRecords, recordsTable);
                nameTextField.setVisible(true);
                saveButton.setText(translate("save.button.save.text"));
                switch (place) {
                    case 1:
                        saveLabel.setText(translate("save.label.first.place.text"));
                        break;
                    case 2:
                        saveLabel.setText(translate("save.label.second.place.text"));
                        break;
                    case 3:
                        saveLabel.setText(translate("save.label.third.place.text"));
                        break;
                    default:
                        saveLabel.setText(translate("save.label.other.place.text", tetris.getScore()));
                }
            } else {
                nameTextField.setVisible(false);
                saveButton.setText(translate("save.button.ok.text"));
                saveLabel.setText(translate("save.label.no.place.text", tetris.getScore()));
            }
            gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 5)); // horizontal alignment: middle

            recordPanel.setVisible(true);
            if (place < 30) {
                nameTextField.requestFocus();
            } else {
                saveButton.requestFocus();
            }
        }

        private void fillModel(Records records, DefaultTableModel recordsTable) {
            recordsTable.setRowCount(0);
            List<Records.Rec> recordsRecords = records.getRecords();
            for (int i = 0; i < records.getRecords().size(); i++) {
                Records.Rec r = recordsRecords.get(i);
                recordsTable.addRow(new Object[]{i + 1, r.getName(), r.getScore()});
            }
        }

        private void saveGame() {
            recordPanel.setVisible(false);
            String str = nameTextField.getText();
            if (str.length() == 0) {
                str = translate("anonymous.name");
            }
            gameRecords.verifyAndAddScore(str, tetris.getScore());
            storage.saveRecord(gameRecords.getRecords());
            gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3)); // horizontal alignment: middle

            startPanel.setVisible(true);
            startClassicGameButton.requestFocus();
        }
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
        startClassicGameButton.setText(translate("start.classic.game.button.text"));
        startCrazyGameButton.setText(translate("start.crazy.game.button.text"));
        exitButton.setText(translate("exit.button.text"));

        continueButton.setText(translate("continue.button.text"));
        pauseLabel.setText(" " + translate("pause.label.text") + " ");

        saveButton.setText(translate("save.button.save.text"));

        recordsTable.setColumnCount(0);
        recordsTable.addColumn(translate("records.table.number.column"));
        recordsTable.addColumn(translate("records.table.name.column"));
        recordsTable.addColumn(translate("records.table.score.column"));

        switch (LanguageProvider.getCurrentLanguage()) {
            case EN:
                languageButton.setIcon(new ImageIcon(enImage));
                break;
            case RU:
                languageButton.setIcon(new ImageIcon(ruImage));
                break;
        }
    }

    private String translate(String key, Object... params) {
        return languageProvider.getTranslation(key, params);
    }
}
