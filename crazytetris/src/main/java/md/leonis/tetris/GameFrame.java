package md.leonis.tetris;

import md.leonis.tetris.engine.*;
import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.CritterState;
import md.leonis.tetris.engine.model.GuiAction;
import md.leonis.tetris.engine.model.Language;

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

import static java.awt.event.KeyEvent.*;
import static md.leonis.tetris.FileSystemStorage.getResourceAsStream;
import static md.leonis.tetris.engine.event.GameEvent.*;

class GameFrame extends JFrame implements GuiInterface {

    private static final Logger LOGGER = Logger.getLogger("GameFrame");

    private static final int windowWidth = 380;
    private static final int windowHeight = 480;

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

    private GameService gameService;
    private EventsMonitor eventsMonitor = new EventsMonitor();

    private boolean paused = false;

    GameFrame(String title, GameService gameService) {
        this.gameService = gameService;

        try {
            boolean debug = gameService.isDebug();
            backgroundImage = ImageIO.read(getResourceAsStream("bg.jpg", debug));
            titleImage = ImageIO.read(getResourceAsStream("title.jpg", debug));
            ruImage = ImageIO.read(getResourceAsStream("ru.png", debug));
            enImage = ImageIO.read(getResourceAsStream("en.png", debug));
        } catch (IOException e) {
            LOGGER.warning("Can't load background images!");
        }
        currentBackgroundImage = titleImage;

        setSize(windowWidth, windowHeight); // Window size
        createGUI(title);

        gameService.playMusic();

        startClassicGameButton.requestFocus();
    }

    private void createGUI(String title) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close window on exit
        setTitle(title);

        setLocationRelativeTo(null);

        // Start Game
        startClassicGameButton.addActionListener(eventsMonitor);
        startClassicGameButton.setActionCommand(START_GAME_A.name());

        startCrazyGameButton.addActionListener(eventsMonitor);
        startCrazyGameButton.setActionCommand(START_GAME_B.name());

        exitButton.setActionCommand(GuiAction.EXIT.name());
        exitButton.addActionListener(eventsMonitor);

        JPanel startPanelButtonsContainer = new JPanel();
        startPanelButtonsContainer.setBorder(BorderFactory.createRaisedBevelBorder());
        startPanelButtonsContainer.setLayout((new GridLayout(3, 1)));
        startPanelButtonsContainer.add(startClassicGameButton);
        startPanelButtonsContainer.add(startCrazyGameButton);
        startPanelButtonsContainer.add(exitButton);

        languageButton.addActionListener(eventsMonitor);
        languageButton.setActionCommand(GuiAction.CHANGE_LANGUAGE.name());

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
        continueButton.setActionCommand(GuiAction.CONTINUE.name());
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

        saveButton.setActionCommand(GuiAction.SAVE.name());
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

        localizeInterface(gameService.switchLanguage());

        // We will switch these three panels: Start Game, Pause, Game Over/Records
        startPanel.setVisible(true);
        pausePanel.setVisible(false);
        recordPanel.setVisible(false);

        this.setVisible(true);
    }

    private void startGame(boolean crazy) {
        currentBackgroundImage = backgroundImage;
        startPanel.setVisible(false);

        gameService.stopMusic();
        gameService.startGame(crazy);
        gamePanel.addKeyListener(eventsMonitor);
    }

    private void pauseGame() {
        gameService.pauseGame(paused);
        if (paused) {
            gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3)); // horizontal alignment: middle
            pausePanel.setVisible(true);
            continueButton.requestFocus();
            gamePanel.repaint();
        } else {
            continueGame();
        }
    }

    private void continueGame() {
        gameService.pauseGame(paused);
        pausePanel.setVisible(false);
        gamePanel.repaint();
    }

    private void exit() {
        gameService.processEvent(GAME_OVER);
        System.exit(0);
    }

    private void saveGame() {
        gameService.saveRecord(nameTextField.getText());

        recordPanel.setVisible(false);
        gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3)); // horizontal alignment: middle
        startPanel.setVisible(true);
        startClassicGameButton.requestFocus();
    }

    @Override
    public void repaint() {
        gamePanel.repaint();
    }

    @Override
    public void updateStatistics() {
        // not need
    }

    @Override
    public void gameOver() {
        gamePanel.removeKeyListener(eventsMonitor);

        Records gameRecords = gameService.initializeRecords();
        int place = gameRecords.getPlace();

        gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 5)); // horizontal alignment: middle
        recordPanel.setVisible(true);

        if (gameRecords.canAddNewRecord()) {
            fillModel(gameRecords, recordsTable);
            nameTextField.setVisible(true);
            nameTextField.requestFocus();
            saveButton.setText(gameService.translate("save.button.save.text"));
            switch (place) {
                case 1:
                    saveLabel.setText(gameService.translate("save.label.first.place.text"));
                    break;
                case 2:
                    saveLabel.setText(gameService.translate("save.label.second.place.text"));
                    break;
                case 3:
                    saveLabel.setText(gameService.translate("save.label.third.place.text"));
                    break;
                default:
                    saveLabel.setText(gameService.translate("save.label.other.place.text", gameRecords.getScore()));
            }
        } else {
            nameTextField.setVisible(false);
            saveButton.setText(gameService.translate("save.button.ok.text"));
            saveLabel.setText(gameService.translate("save.label.no.place.text", gameRecords.getScore()));
            saveButton.requestFocus();
        }
        gamePanel.repaint();
    }

    private void fillModel(Records records, DefaultTableModel recordsTable) {
        recordsTable.setRowCount(0);
        List<Records.Rec> recordsRecords = records.getRecords();
        for (int i = 0; i < records.getRecords().size(); i++) {
            Records.Rec r = recordsRecords.get(i);
            recordsTable.addRow(new Object[]{i + 1, r.getName(), r.getScore()});
        }
    }

    class GamePanel extends JPanel {

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage frameBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = (Graphics2D) frameBuffer.getGraphics();
            g2d.drawImage(currentBackgroundImage, 0, 0, null);

            if (gameService.isInitialized()) {
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
            g2d.setColor(gameService.getTransparentColor());

            int width = gameService.getWidth();
            int height = gameService.getHeight();
            int tileWidth = gameService.getTileWidth();
            int tileHeight = gameService.getTileHeight();

            g2d.fillRect(0, 0, width * tileWidth, (height - 2) * tileHeight);
            g2d.setColor(gameService.getGrayColor());
            for (int i = 1; i < width; i++) {
                for (int j = 3; j < height; j++) {
                    g2d.drawRect(i * tileWidth, (j - 2) * tileHeight, 0, 0);
                }
            }

            for (int i = 0; i < width; i++) {
                for (int j = 2; j < height; j++) {
                    g2d.setColor(gameService.getColor(gameService.getGlass().get(i, j)));
                    g2d.fillRoundRect(i * tileWidth, (j - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
                }
            }

            // Show score, lines, other statistics
            int leftPos = width * tileWidth + 7;
            g2d.setColor(gameService.getTransparentColor());
            g2d.drawString(gameService.translate("score", gameService.getGameScore().getScore()), leftPos, 10);
            g2d.drawString(gameService.translate("lines", gameService.getGameScore().getLines()), leftPos, 30);
            g2d.drawString(gameService.translate("level", gameService.getGameScore().getLevel()), leftPos, 50);
            Critter critter = gameService.getCritter();
            if (critter != null) {
                g2d.drawString(gameService.translate("air", (int) critter.getAir()), leftPos, 70);
                g2d.drawString(gameService.translate(critter.getStringStatus()), leftPos, 90);
            }

            // Draw figure
            Figure figure = gameService.getFigure();
            for (Coordinate coordinate : figure.getCoordinates()) {
                int k = figure.getGhostTop();
                g2d.setColor(gameService.getColor(figure.getColor(), 4));
                if (coordinate.getY() + k >= 2) {
                    g2d.fillRoundRect((coordinate.getX() + figure.getLeft()) * tileWidth, (coordinate.getY() + k - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
                }
            }

            Figure nextFigure = gameService.getNextFigure();
            int kx = nextFigure.getCoordinates().stream().map(Coordinate::getX).min(Integer::compare).orElse(0);
            int ky = nextFigure.getCoordinates().stream().map(Coordinate::getY).min(Integer::compare).orElse(0);

            for (Coordinate coordinate : nextFigure.getCoordinates()) {
                g2d.setColor(gameService.getColor(figure.getColor(), 4));
                g2d.fillRoundRect((coordinate.getX() - kx) * tileWidth + leftPos + 1, (coordinate.getY() - ky) * tileHeight + 100 + 1, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);

                g2d.setColor(gameService.getColor(nextFigure.getColor()));
                g2d.fillRoundRect((coordinate.getX() - kx) * tileWidth + leftPos, (coordinate.getY() - ky) * tileHeight + 100, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
            }

            for (Coordinate coordinate : figure.getCoordinates()) {
                g2d.setColor(gameService.getColor(figure.getColor()));
                if ((coordinate.getY() + figure.getTop()) >= 2)
                    g2d.fillRoundRect((coordinate.getX() + figure.getLeft()) * tileWidth, (coordinate.getY() + figure.getTop() - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
            }

            // Draw critter
            if (critter != null) {
                if (critter.getStatus() != CritterState.DEAD) {
                    g2d.setColor(gameService.getCritterColor());
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

    class EventsMonitor implements ActionListener, EventListener, KeyListener {

        @Override // Swing actions
        public void actionPerformed(ActionEvent e) {
            this.processAction(GuiAction.valueOf(e.getActionCommand()));
        }

        private void processAction(GuiAction action) {
            switch (action) {
                case START_GAME_A:
                    startGame(false);
                    break;

                case START_GAME_B:
                    startGame(true);
                    break;

                case PAUSE:
                    paused = true;
                    pauseGame();
                    break;

                case CONTINUE:
                    paused = false;
                    continueGame();
                    break;

                case CHANGE_LANGUAGE:
                    switchLanguage();
                    break;

                case SAVE:
                    saveGame();
                    break;

                case EXIT:
                    exit();
            }
        }

        @Override // Swing key actions
        public void keyPressed(KeyEvent e) {
            if (e.isConsumed()) {
                return;
            }

            switch(e.getKeyCode()) {
                case VK_LEFT:
                    gameService.processEvent(MOVE_LEFT);
                    break;
                case VK_RIGHT:
                    gameService.processEvent(MOVE_RIGHT);
                    break;
                case VK_DOWN:
                    gameService.processEvent(STEP_DOWN);
                    break;
                case VK_SPACE:
                    gameService.processEvent(FALL_DOWN);
                    break;
                case VK_INSERT:
                    gameService.processEvent(ROTATE_LEFT);
                    break;
                case VK_UP:
                    gameService.processEvent(ROTATE_RIGHT);
                    break;
                case VK_P:
                    if (paused) {
                        processAction(GuiAction.CONTINUE);
                    } else {
                        processAction(GuiAction.PAUSE);
                    }
                    break;
                case VK_F12:
                    gameService.processEvent(NEXT_LEVEL);
                    break;
                case VK_ESCAPE:
                    processAction(GuiAction.EXIT);
                    break;
            }
            e.consume();
        }

        @Override // Unused
        public void keyReleased(KeyEvent e) {
        }

        @Override // Unused
        public void keyTyped(KeyEvent e) {
        }
    }

    private void switchLanguage() {
        localizeInterface(gameService.switchLanguage());
    }

    private void localizeInterface(Language language) {
        startClassicGameButton.setText(gameService.translate("start.classic.game.button.text"));
        startCrazyGameButton.setText(gameService.translate("start.crazy.game.button.text"));
        exitButton.setText(gameService.translate("exit.button.text"));

        continueButton.setText(gameService.translate("continue.button.text"));
        pauseLabel.setText(" " + gameService.translate("pause.label.text") + " ");

        saveButton.setText(gameService.translate("save.button.save.text"));

        recordsTable.setColumnCount(0);
        recordsTable.addColumn(gameService.translate("records.table.number.column"));
        recordsTable.addColumn(gameService.translate("records.table.name.column"));
        recordsTable.addColumn(gameService.translate("records.table.score.column"));

        switch (language) {
            case EN:
                languageButton.setIcon(new ImageIcon(enImage));
                break;
            case RU:
                languageButton.setIcon(new ImageIcon(ruImage));
                break;
        }
    }

}
