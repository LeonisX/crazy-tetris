package md.leonis.tetris;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static md.leonis.tetris.ResourceUtils.getResourceAsStream;
import static md.leonis.tetris.engine.GameState.PAUSED;
import static md.leonis.tetris.engine.GameState.RUNNING;

public class GameFrame extends JFrame {

    private JPanel startPanel, pausePanel, recordPanel, recordPanel2;            // будем менять эти три панели
    private JButton saveButton;
    private JButton continueButton;
    private JButton startButton;
    private JLabel saveLabel;
    private GamePanel myPanel;
    private final JTextField myTextField;                        // поле для ввода текста
    private Tetris tetris;
    private Monitor monitor;
    private Records GameRecords;
    private DefaultTableModel model;
    private JTable table;
    private BufferedImage frameBuffer;
    private Image bg, title, ic;
    private int height = 480;
    private int width = 380;
    private MusicChannel musicChannel;
    private boolean crazy = false;

    private boolean isDebug;

    GameFrame(String s, boolean isDebug) {                            // конструктор

        this.isDebug = isDebug;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);            // при закрытии закрывается окно
        setTitle(s);

        setSize(width, height);                        // габариты

        setLocationRelativeTo(null);

        myPanel = new GamePanel();                    // основная панель
        myPanel.setFocusable(true);

        monitor = new GameMonitor();

        musicChannel = new MusicChannel(getResourceAsStream("audio/music.mp3", isDebug));

        try {
            bg = ImageIO.read(getResourceAsStream("bg.jpg", isDebug));
            title = ImageIO.read(getResourceAsStream("title.jpg", isDebug));
        } catch (IOException e) {
            //TODO
        }
        ic = title;
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
        recordPanel2 = new JPanel();
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
        table = new JTable(model);
        model.addColumn("Имя");
        model.addColumn("Рекорд");
//    GameRecords.fillModel(model);

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
            frameBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);


            Graphics2D g2d = (Graphics2D) frameBuffer.getGraphics();
            g2d.drawImage(ic, 0, 0, null);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setColor(Color.black);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2d.drawString("© Leonis, 2015-2019", width - 117, height - 50);
            g2d.dispose();
            if (tetris != null) tetris.draw(frameBuffer.getGraphics());
            g.drawImage(frameBuffer, 0, 0, this);
        }
    }

    class GameMonitor extends Monitor {
        public void actionPerformed(ActionEvent e) {
            String s = e.getActionCommand();                // получаем команду
            //TODO enum
            if (s.equals("starta")) {
                crazy = false;
                s = "start";

            } else if (s.equals("startb")) {
                crazy = true;
                s = "start";

            }
            switch (s) {
                case "start":
                    ic = bg;
                    musicChannel.stop();
                    startPanel.setVisible(false);
                    tetris = new Tetris(isDebug);
                    tetris.setCrazy(crazy);
                    tetris.panel = myPanel;
                    tetris.monitor = this;
                    tetris.frameBuffer = frameBuffer;
                    tetris.start();
                    myPanel.addKeyListener(tetris);
                    myPanel.addKeyListener(this);
                    //GamePanel.removeKeyListener(tetris);
                    //GamePanel.removeKeyListener(this);
                    break;

                case "continue":
                    pausePanel.setVisible(false);
                    tetris.pause(false);
                    break;

                case "gameover":
                    if (crazy) s = "crazy.res";
                    else s = "tet.res";
                    GameRecords = new Records(s);
                    int place = GameRecords.getPlace(tetris.score);
                    if (GameRecords.verify(tetris.score)) {
                        GameRecords.fillModel(model);
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
                                saveLabel.setText(tetris.score + " очков! Ваше имя: ");
                        }
                    } else {
                        myTextField.setVisible(false);
                        saveButton.setText("Тык");
                        saveLabel.setText("Набрано " + tetris.score + " очков. Маловато для рекорда!");
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
                    GameRecords.verifyAndAdd(str, tetris.score);
                    GameRecords.save();
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

        /*
        "Слушатель". Отвечает за интерфейс
        */
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_ESCAPE:
                    tetris.finish();
                    GameRecords.save();
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
            }
        }
    }
}
