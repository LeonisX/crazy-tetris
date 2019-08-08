package md.leonis.tetris;

import md.leonis.tetris.engine.*;
import md.leonis.tetris.engine.event.GameEvent;
import md.leonis.tetris.engine.event.GameEventListener;
import md.leonis.tetris.engine.model.Coordinate;
import md.leonis.tetris.engine.model.CritterState;
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

import static md.leonis.tetris.FileSystemStorage.getResourceAsStream;
import static md.leonis.tetris.engine.event.GameEvent.*;
import static md.leonis.tetris.engine.model.GameState.PAUSED;
import static md.leonis.tetris.engine.model.GameState.RUNNING;
import static md.leonis.tetris.engine.model.SoundId.*;

class GameFrame extends JFrame {

    private JPanel startPanel;
    private JButton startButton;

    private JPanel pausePanel;
    private JButton continueButton;

    private JPanel recordPanel;
    private JButton saveButton;
    private JLabel saveLabel;
    private JTextField nameTextField;                        // поле для ввода текста

    private GamePanel gamePanel;

    private DefaultTableModel model;

    private Image backgroundImage, titleImage, currentBackgroundImage;

    private EventMapper eventMapper;
    private StorageInterface storage;
    private MusicChannel musicChannel;
    private SoundMonitor soundMonitor;                    // монитор для звуковых эффектов

    private Tetris tetris;
    private Records gameRecords;

    private Config config;

    private boolean crazy = false;

    GameFrame(String title, boolean isDebug) {                            // конструктор
        config = new Config();

        eventMapper = new EventMapper();

        storage = new FileSystemStorage();

        musicChannel = new MusicChannel(getResourceAsStream("audio/music.mp3", isDebug));

        soundMonitor = new SoundMonitor();            // создаём монитор звуковых эффектов
        soundMonitor.addSoundWithGain(FALLED, getResourceAsStream("audio/falled.wav", isDebug), 0.9f);        // 0 звук; громкость (от 0 до 1.0f)
        soundMonitor.addSoundWithGain(ROTATE, getResourceAsStream("audio/rotate.wav", isDebug), 0.9f);        // 1 звук
        soundMonitor.addSoundWithGain(CLICK, getResourceAsStream("audio/click.wav", isDebug), 1.0f);         // 2 звук
        soundMonitor.addSoundWithGain(HEARTBEAT_A, getResourceAsStream("audio/heartbeat-a.wav", isDebug), 0.8f);   // 3 звук
        soundMonitor.addSoundWithGain(HEARTBEAT_B, getResourceAsStream("audio/heartbeat-b.wav", isDebug), 0.9f);   // 4 звук

        try {
            backgroundImage = ImageIO.read(getResourceAsStream("bg.jpg", isDebug));
            titleImage = ImageIO.read(getResourceAsStream("title.jpg", isDebug));
        } catch (IOException e) {
            //TODO
        }
        currentBackgroundImage = titleImage;

        EventsMonitor eventsMonitor = new EventsMonitor();

        setSize(config.windowWidth, config.windowHeight);                        // габариты
        createGUI(title, eventsMonitor);

        if (tetris.isSoundOn()) {
            musicChannel.play();
        }
        startButton.requestFocus();
    }

    private void createGUI(String title, EventsMonitor eventsMonitor) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);            // при закрытии закрывается окно
        setTitle(title);

        setLocationRelativeTo(null);

        // отступы сверху и снизу по 170. Так виртуально компоную содержимое панели в середине окна
        /*
         * Что делается ниже:
         * 1. Создаётся панель, разделённая на верх и низ.
         * 2. В верхнюю часть размещаем текстовое сообщение, в нижнюю элементы управления
         * 3. И так три раза для каждой из рабочих панелей
         */
        // Start Game
        startButton = new JButton("Обычная игра");                // создаём кнопку
        startButton.addActionListener(eventsMonitor);                // слушатель событий кнопки
        startButton.setActionCommand(START_GAME_A.name());                // команда, которая передаётся слушателю

        JButton startButton2 = new JButton("Сумасшедшая игра");                // создаём кнопку
        startButton2.addActionListener(eventsMonitor);                // слушатель событий кнопки
        startButton2.setActionCommand(START_GAME_B.name());                // команда, которая передаётся слушателю

        JButton closeButton = new JButton("Выход");
        closeButton.setActionCommand(EXIT.name());
        closeButton.addActionListener(eventsMonitor);

        startPanel = new JPanel();
        startPanel.setBorder(BorderFactory.createRaisedBevelBorder());    // рамка панели
        startPanel.setLayout(new GridLayout(3, 1));                // 3 ячейки
        startPanel.add(startButton);                    // добавляем кнопку в контейнер для нижней ячейки
        startPanel.add(startButton2);                    // добавляем кнопку в контейнер для нижней ячейки
        startPanel.add(closeButton);                    // вторая кнопка, будет справа от первой, обе отцентрованы

        // Pause
        continueButton = new JButton("Тык");
        continueButton.addActionListener(eventsMonitor);
        continueButton.addKeyListener(eventsMonitor);
        continueButton.setActionCommand(CONTINUE.name());
        JPanel continuePanel = new JPanel();
        continuePanel.add(continueButton);

        pausePanel = new JPanel();
        pausePanel.setBorder(BorderFactory.createRaisedBevelBorder());
        pausePanel.setLayout(new GridLayout(2, 1));
        pausePanel.add(new JLabel(" Для продолжения нажмите кнопку "));
        pausePanel.add(continuePanel);

        // Game Over / Records
        // будем менять эти три панели
        saveLabel = new JLabel();
        JPanel newRecordPanel = new JPanel();
        newRecordPanel.add(saveLabel);      //"Новый рекорд! Ваше имя: "
        nameTextField = new JTextField("", 16);
        newRecordPanel.add(nameTextField);

        saveButton = new JButton("Записать");
        saveButton.setActionCommand(SAVE.name());
        saveButton.addActionListener(eventsMonitor);

        JPanel savePanel = new JPanel();
        savePanel.add(saveButton);

        model = new DefaultTableModel();
        model.addColumn("N");
        model.addColumn("Имя");
        model.addColumn("Рекорд");

        JTable table = new JTable(model);

        recordPanel = new JPanel();
        recordPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        recordPanel.setLayout(new BoxLayout(recordPanel, BoxLayout.Y_AXIS));
        recordPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
        recordPanel.add(table);
        recordPanel.add(newRecordPanel);
        recordPanel.add(savePanel);

        // Main panel (container)
        gamePanel = new GamePanel();
        gamePanel.setFocusable(true);
        gamePanel.add(startPanel);
        gamePanel.add(pausePanel);
        gamePanel.add(recordPanel);
        gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3));    // выравнивание по горизонтали - по середине

        this.add(gamePanel);

        startPanel.setVisible(true);
        pausePanel.setVisible(false);
        recordPanel.setVisible(false);

        this.setVisible(true);                        // делаем видимым окно
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
            //рисуем стакан
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

            //выводим счёт, линии.
            int leftPos = width * tileWidth + 7;
            g2d.setColor(config.getColor(config.transparentColor));
            g2d.drawString("Счёт: " + tetris.getScore(), leftPos, 10);
            g2d.drawString("Линий: " + tetris.getLines(), leftPos, 30);
            g2d.drawString("Уровень: " + tetris.getLevel(), leftPos, 50);
            Critter critter = tetris.getCritter();
            if (critter != null) {
                g2d.drawString("Воздух: " + (int) critter.getAir() + "%", leftPos, 70);
                g2d.drawString(critter.getStringStatus(), leftPos, 90);
            }

            //рисуем фигуру
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

            //рисую персонажа
            if (critter != null) {
                if (critter.getStatus() != CritterState.DEAD) {
                    g2d.setColor(config.getColor(config.critterColor));
                    g2d.drawOval(critter.getX() * tileWidth, (critter.getY() - 2) * tileHeight, tileWidth, tileHeight);
                    kx = critter.getHorizontalDirection() * 2;
                    ky = 0;
                    if (critter.getStatus() == CritterState.FALLING) ky = 1;
                    if (critter.getStatus() == CritterState.JUMPING) ky = -1;
                    if (critter.getStatus() == CritterState.STAYING) kx = 0;
                    //глаза
                    g2d.drawArc(critter.getX() * tileWidth + 7 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
                    g2d.drawArc(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
                    //глаза
                    if (critter.getAir() < 50) {
                        g2d.drawRect(critter.getX() * tileWidth + 7 + kx + 1, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
                        g2d.drawRect(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
                    }
                    //рот
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
                    System.exit(0);                        // закрываем программу
            }
            gamePanel.repaint();                        // вызываем перерисовку панели
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
            if (tetris.getState() == RUNNING) {
                tetris.processEvent(PAUSE);
                gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3));    // выравнивание по горизонтали - по середине
                pausePanel.setVisible(true);
                continueButton.requestFocus();
                gamePanel.repaint();
            } else if (tetris.getState() == PAUSED) {
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
                fillModel(gameRecords, model);
                nameTextField.setVisible(true);
                saveButton.setText("Записать");
                switch (place) {
                    case 1:
                        saveLabel.setText("Первое место!!! Ваше имя: ");
                        break;
                    case 2:
                        saveLabel.setText("Второе место!! Ваше имя: ");
                        break;
                    case 3:
                        saveLabel.setText("Третье место! Ваше имя: ");
                        break;
                    default:
                        saveLabel.setText(tetris.getScore() + " очков! Ваше имя: ");
                }
            } else {
                nameTextField.setVisible(false);
                saveButton.setText("Тык");
                saveLabel.setText("Набрано " + tetris.getScore() + " очков. Маловато для рекорда!");
            }
            gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 5));    // выравнивание по горизонтали - по середине

            recordPanel.setVisible(true);
            if (place < 30) {
                nameTextField.requestFocus();
            } else {
                saveButton.requestFocus();
            }
        }

        private void fillModel(Records records, DefaultTableModel defaultTableModel) {
            defaultTableModel.setRowCount(0);
            List<Records.Rec> recordsRecords = records.getRecords();
            for (int i = 0; i < records.getRecords().size(); i++) {
                Records.Rec r = recordsRecords.get(i);
                defaultTableModel.addRow(new Object[]{i + 1, r.getName(), r.getScore()});
            }
        }

        private void saveGame() {
            recordPanel.setVisible(false);
            String str = nameTextField.getText();
            if (str.length() == 0) {
                str = "Капитан Немо";
            }
            gameRecords.verifyAndAddScore(str, tetris.getScore());
            storage.saveRecord(gameRecords.getRecords());
            gamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, getHeight() / 3));    // выравнивание по горизонтали - по середине

            startPanel.setVisible(true);
            startButton.requestFocus();
        }
    }
}
