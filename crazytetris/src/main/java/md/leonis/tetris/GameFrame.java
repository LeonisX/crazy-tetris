package md.leonis.tetris;

import md.leonis.tetris.engine.*;
import md.leonis.tetris.engine.event.Event;
import md.leonis.tetris.engine.event.GameEventListener;

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
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;

import static md.leonis.tetris.engine.event.Event.*;
import static md.leonis.tetris.ResourceUtils.getResourceAsStream;
import static md.leonis.tetris.engine.GameState.PAUSED;
import static md.leonis.tetris.engine.GameState.RUNNING;

class GameFrame extends JFrame {

    private JPanel startPanel;
    private JPanel pausePanel;
    private JPanel recordPanel;
    private JButton saveButton;
    private JButton continueButton;
    private JButton startButton;
    private JLabel saveLabel;
    private GamePanel myPanel;
    private final JTextField myTextField;                        // поле для ввода текста
    private Tetris tetris;
    private Records gameRecords;
    private StorageInterface storage;
    private DefaultTableModel model;
    private Image bg, title, ic;
    private int height = 480;
    private int width = 380;
    private MusicChannel musicChannel;
    private SoundMonitor soundMonitor;                    // монитор для звуковых эффектов
    private boolean crazy = false;

    private int tileWidth, tileHeight;

    private Config config;

    GameFrame(String title, boolean isDebug) {                            // конструктор
        config = new Config();
        config.isDebug = isDebug;

        this.tileWidth = config.tileWidth;
        this.tileHeight = config.tileHeight;


        storage = new FileSystemStorage();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);            // при закрытии закрывается окно
        setTitle(title);

        setSize(width, height);                        // габариты

        setLocationRelativeTo(null);

        myPanel = new GamePanel();                    // основная панель
        myPanel.setFocusable(true);

        Monitor monitor = new Monitor();

        musicChannel = new MusicChannel(getResourceAsStream("audio/music.mp3", isDebug));

        soundMonitor = new SoundMonitor();            // создаём монитор звуковых эффектов
        soundMonitor.addSound(getResourceAsStream("audio/falled.wav", config.isDebug));        // 1 звук
        soundMonitor.addSound(getResourceAsStream("audio/rotate.wav", config.isDebug));        // 2 звук
        soundMonitor.addSound(getResourceAsStream("audio/click.wav", config.isDebug));         // 3 звук
        soundMonitor.addSound(getResourceAsStream("audio/heartbeat-a.wav", config.isDebug));   // 4 звук
        soundMonitor.addSound(getResourceAsStream("audio/heartbeat-b.wav", config.isDebug));   // 5 звук
        soundMonitor.setGain(0, 0.9f);               // громкость (от 0 до 1.0f)
        soundMonitor.setGain(1, 0.9f);
        soundMonitor.setGain(2, 1.0f);
        soundMonitor.setGain(3, 0.8f);
        soundMonitor.setGain(4, 0.9f);

        try {
            bg = ImageIO.read(getResourceAsStream("bg.jpg", isDebug));
            this.title = ImageIO.read(getResourceAsStream("title.jpg", isDebug));
        } catch (IOException e) {
            //TODO
        }
        ic = this.title;
        // отступы сверху и снизу по 170
        // так виртуально компоную содержимое панели в середине окна
        /*
         * Что делается ниже:
         * 1. Создаётся панель, разделённая на верх и низ.
         * 2. В верхнюю часть размещаем текстовое сообщение, в нижнюю элементы управления
         * 3. И так три раза для каждой из рабочих панелей
         */
        startPanel = new JPanel();
        startPanel.setBorder(BorderFactory.createRaisedBevelBorder());    // рамка панели
        startPanel.setLayout(new GridLayout(3, 1));                // две ячейки
        startButton = new JButton("Обычная игра");                // создаём кнопку
        startButton.addActionListener(monitor);                // слушатель событий кнопки
        startButton.setActionCommand("starta");                // команда, которая передаётся слушателю
        JButton startButton2 = new JButton("Сумасшедшая игра");                // создаём кнопку
        startButton2.addActionListener(monitor);                // слушатель событий кнопки
        startButton2.setActionCommand("startb");                // команда, которая передаётся слушателю

        JButton closeButton = new JButton("Выход");
        closeButton.setActionCommand("close");
        closeButton.addActionListener(monitor);
        startPanel.add(startButton);                    // добавляем кнопку в контейнер для нижней ячейки
        startPanel.add(startButton2);                    // добавляем кнопку в контейнер для нижней ячейки
        startPanel.add(closeButton);                    // вторая кнопка, будет справа от первой, обе отцентрованы

        pausePanel = new JPanel();
        pausePanel.setBorder(BorderFactory.createRaisedBevelBorder());
        pausePanel.setLayout(new GridLayout(2, 1));
        pausePanel.add(new JLabel(" Для продолжения нажмите кнопку "));
        JPanel pausePanel2 = new JPanel();
        continueButton = new JButton("Тык");
        continueButton.addActionListener(monitor);
        continueButton.addKeyListener(monitor);
        continueButton.setActionCommand("continue");
        pausePanel2.add(continueButton);
        pausePanel.add(pausePanel2);

        recordPanel = new JPanel();
        recordPanel.setBorder(BorderFactory.createRaisedBevelBorder());
//    recordPanel.setLayout(new GridLayout(4,1,0,0));
        recordPanel.setLayout(new BoxLayout(recordPanel, BoxLayout.Y_AXIS));
        // будем менять эти три панели
        JPanel recordPanel2 = new JPanel();
        saveLabel = new JLabel();
        recordPanel2.add(saveLabel);      //"Новый рекорд! Ваше имя: "
        myTextField = new JTextField("", 16);
        recordPanel2.add(myTextField);
        JPanel recordPanel3 = new JPanel();
        saveButton = new JButton("Записать");
        recordPanel3.add(saveButton);
        saveButton.setActionCommand("save");
        saveButton.addActionListener(monitor);

        model = new DefaultTableModel();
        JTable table = new JTable(model);
        model.addColumn("N");
        model.addColumn("Имя");
        model.addColumn("Рекорд");

        recordPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
        recordPanel.add(table);
        recordPanel.add(recordPanel2);
        recordPanel.add(recordPanel3);

        myPanel.add(startPanel);
        myPanel.add(pausePanel);
        myPanel.add(recordPanel);
        add(myPanel);

        myPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, height / 3));    // выравнивание по горизонтали - по середине
        startPanel.setVisible(true);
        pausePanel.setVisible(false);
        recordPanel.setVisible(false);
        setVisible(true);                        // делаем видимым окно
        musicChannel.play();
        startButton.requestFocus();
    }

    class GamePanel extends JPanel {
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage frameBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = (Graphics2D) frameBuffer.getGraphics();
            g2d.drawImage(ic, 0, 0, null);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setColor(Color.black);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2d.drawString("© Leonis, 2015-2019", width - 117, height - 50);
            g2d.dispose();
            if (tetris != null && tetris.isInitialized()) {
                draw(frameBuffer.getGraphics());
            }
            g.drawImage(frameBuffer, 0, 0, this);
        }

        void draw(Graphics gx) {
//        g=frameBuffer.getGraphics();
            //рисуем стакан
            Graphics2D g = (Graphics2D) gx;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(10, 10);
            g.setColor(Color.BLACK);

            int width = tetris.getWidth();
            int height = tetris.getHeight();

            g.fillRect(0, 0, width * tileWidth, (height - 2) * tileHeight);
            g.setColor(new Color(100, 100, 100));
            for (int i = 1; i < width; i++) {
                for (int j = 3; j < height; j++) {
                    g.drawRect(i * tileWidth, (j - 2) * tileHeight, 0, 0);
                }
            }

            for (int i = 0; i < width; i++) {
                for (int j = 2; j < height; j++) {
                    g.setColor(config.colors[tetris.getGlass().get(i, j)]);
                    g.fillRoundRect(i * tileWidth, (j - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
                }
            }

            //выводим счёт, линии.
            int lpos = width * tileWidth + 7;
            g.setColor(Color.BLACK);
            g.drawString("Счёт: " + tetris.getScore(), lpos, 10);
            g.drawString("Линий: " + tetris.getLines(), lpos, 30);
            g.drawString("Уровень: " + tetris.getLevel(), lpos, 50);
            String st = "Жизнь прекрасна :)";
            if (tetris.getCritter().getAir() < 75) {
                st = "Надо отдышаться...";
            }
            Critter critter = tetris.getCritter();
            if (critter.isBounded()) {
                if (critter.getAir() < 50) {
                    st = "Задыхаюсь!!!";
                } else {
                    st = "Тут мало воздуха...";
                }
            }
            g.drawString("Воздух: " + (int) critter.getAir() + "%", lpos, 70);
            g.drawString(st, lpos, 90);

            //рисуем фигуру
            Figure figure = tetris.getFigure();
            for (Coordinate coordinate : figure.getCoordinates()) {
                int k = figure.getGhostTop();
                g.setColor(new Color(config.colors[figure.getColor()].getRed() / 7, config.colors[figure.getColor()].getGreen() / 7, config.colors[figure.getColor()].getBlue() / 4));
                if ((coordinate.getY() + k) >= 2)
                    g.fillRoundRect((coordinate.getX() + figure.getLeft()) * tileWidth, (coordinate.getY() + k - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
            }

            int kx = 0;
            int ky = 0;
            Figure nextFigure = tetris.getNextFigure();
            for (Coordinate coordinate : nextFigure.getCoordinates()) {
                if (coordinate.getX() < kx) {
                    kx = coordinate.getX();
                }
                if (coordinate.getY() < ky) {
                    ky = coordinate.getY();
                }
            }

            for (Coordinate coordinate : nextFigure.getCoordinates()) {
                g.setColor(new Color(config.colors[figure.getColor()].getRed() / 4, config.colors[figure.getColor()].getGreen() / 4, config.colors[figure.getColor()].getBlue() / 3));
                g.fillRoundRect((coordinate.getX() - kx) * tileWidth + lpos + 1, (coordinate.getY() - ky) * tileHeight + 100 + 1, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);

                g.setColor(config.colors[nextFigure.getColor()]);
                g.fillRoundRect((coordinate.getX() - kx) * tileWidth + lpos, (coordinate.getY() - ky) * tileHeight + 100, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);

            }

            for (Coordinate coordinate : figure.getCoordinates()) {
                g.setColor(config.colors[figure.getColor()]);
                if ((coordinate.getY() + figure.getTop()) >= 2)
                    g.fillRoundRect((coordinate.getX() + figure.getLeft()) * tileWidth, (coordinate.getY() + figure.getTop() - 2) * tileHeight, tileWidth - 1, tileHeight - 1, tileWidth / 2, tileHeight / 2);
            }

            //рисую персонажа
            if (critter.getStatus() != CritterState.DEAD) {
                g.setColor(Color.WHITE);
                g.drawOval(critter.getX() * tileWidth, (critter.getY() - 2) * tileHeight, tileWidth, tileHeight);
                kx = critter.getHorizontalDirection() * 2;
                ky = 0;
                if (critter.getStatus() == CritterState.FALLING) ky = 1;
                if (critter.getStatus() == CritterState.JUMPING) ky = -1;
                if (critter.getStatus() == CritterState.STAYING) kx = 0;
                //глаза
                g.drawArc(critter.getX() * tileWidth + 7 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
                g.drawArc(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky, 1, 1, 0, 360);
                //глаза
                if (critter.getAir() < 50) {
                    g.drawRect(critter.getX() * tileWidth + 7 + kx + 1, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
                    g.drawRect(critter.getX() * tileWidth + 12 + kx, (critter.getY() - 2) * tileHeight + 6 + ky - 1, 0, 0);
                }
                //рот
                int wx;
                if (critter.isBounded()) wx = 2;
                else wx = 6;
                if ((critter.getAir() > 75) && (!critter.isBounded())) {
                    g.drawArc(critter.getX() * tileWidth + 7 + kx - 1, (critter.getY() - 2) * tileHeight + 14 - 3, wx + 2, 3, 0, -180);
                } else
                    g.drawRect(critter.getX() * tileWidth + 7 + kx + (6 - wx) / 2, (critter.getY() - 2) * tileHeight + 14, wx, 0);
            }
        }
    }

    class Monitor implements ActionListener, EventListener, KeyListener, GameEventListener {

        @Override
        public void actionPerformed(ActionEvent e) { // Swing events
            processAction(e.getActionCommand());
        }

        @Override
        public void notify(Event event, String message) { // internal events, GAME_OVER only
            switch (event) {
                case REPAINT:
                    myPanel.repaint();
                    break;
                case GAME_OVER:
                    processAction("gameover");
                    break;
            }
        }

        private void processAction(String action) {
            //TODO enum
            if (action.equals("starta")) {
                crazy = false;
                action = "start";

            } else if (action.equals("startb")) {
                crazy = true;
                action = "start";

            }
            switch (action) {
                case "start":
                    ic = bg;
                    musicChannel.stop();
                    startPanel.setVisible(false);
                    tetris = new Tetris(config, crazy);
                    Arrays.asList(REPAINT, GAME_OVER).forEach(event -> tetris.addListener(event, this));

                    Arrays.asList(PLAY_SOUND, START_LOOPING_SOUND, STOP_LOOPING_SOUND, FADE_LOOPING_SOUND, SUPPORT_LOOPING_SOUNDS)
                            .forEach(event -> tetris.addListener(event, soundMonitor));

                    tetris.start();
                    myPanel.addKeyListener(this);
                    break;

                case "continue":
                    pausePanel.setVisible(false);
                    tetris.pause(false);
                    break;

                case "gameover":
                    myPanel.removeKeyListener(this);
                    String fileName = crazy ? "crazy.res" : "tet.res";
                    storage.setFileName(fileName);
                    gameRecords = new Records(storage);
                    int place = gameRecords.getPlace(tetris.getScore());
                    if (gameRecords.canAddNewRecord(place)) {
                        fillModel(gameRecords, model);
                        myTextField.setVisible(true);
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
                        myTextField.setVisible(false);
                        saveButton.setText("Тык");
                        saveLabel.setText("Набрано " + tetris.getScore() + " очков. Маловато для рекорда!");
                    }
                    myPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, height / 5));    // выравнивание по горизонтали - по середине

                    recordPanel.setVisible(true);
                    if (place < 30) myTextField.requestFocus();
                    else saveButton.requestFocus();
                    break;

                case "save":
                    recordPanel.setVisible(false);
                    String str = myTextField.getText();
                    if (str.length() == 0) str = "Капитан Немо";
                    gameRecords.verifyAndAddScore(str, tetris.getScore());
                    storage.save(gameRecords.getRecords());
                    myPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, height / 3));    // выравнивание по горизонтали - по середине

                    startPanel.setVisible(true);
                    startButton.requestFocus();
                    break;

                case "close":
                    if (tetris != null) tetris.finish();
                    System.exit(0);                        // закрываем программу

            }
            myPanel.repaint();                        // вызываем перерисовку панели
        }

        // "Слушатель". Отвечает за интерфейс
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.isConsumed()) {
                return;
            }
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_ESCAPE:
                    tetris.finish();
                    storage.save(gameRecords.getRecords());
                    System.exit(0);
                    break;
                case KeyEvent.VK_P:
                    if (tetris.getState() == RUNNING) {
                        tetris.pause(true);
                        myPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 170, height / 3));    // выравнивание по горизонтали - по середине
                        pausePanel.setVisible(true);
                        continueButton.requestFocus();
                    } else if (tetris.getState() == PAUSED) {
                        pausePanel.setVisible(false);
                        tetris.pause(false);
                    }
                    myPanel.repaint();                        // вызываем перерисовку панели
                    break;
                default:
                    tetris.keyPressed(e.getKeyCode());
            }
            e.consume();
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        private void fillModel(Records records, DefaultTableModel model) {
            model.setRowCount(0);
            List<Records.Rec> recordsRecords = records.getRecords();
            for (int i = 0; i < records.getRecords().size(); i++) {
                Records.Rec r = recordsRecords.get(i);
                model.addRow(new Object[]{i + 1, r.getName(), r.getScore()});
            }
        }
    }
}
