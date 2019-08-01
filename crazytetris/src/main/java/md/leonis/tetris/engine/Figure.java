package md.leonis.tetris.engine;

import java.util.Arrays;
import java.util.Random;

public class Figure {

    private int[][] figureDef = {           // Координаты тайлов "кирпичиков"
            {-1, -1, 0, 0},                // "O" - X
            {0, 1, 0, 1},                  // "O" - Y
            {-1, 0, 1, 1},                 // "J" - X
            {0, 0, 0, 1},                  // "J" - Y
            {-1, 0, 1, 1},                 // "L" - X
            {0, 0, 0, -1},                 // "L" - Y
            {-1, 0, 1, 0},                 // "T" - X
            {0, 0, 0, 1},                  // "T" - Y
            {-1, 0, 0, 1},                 // "Z" - X
            {0, 0, 1, 1},                  // "Z" - Y
            {-1, 0, 0, 1},                 // "S" - X
            {1, 1, 0, 0},                  // "S" - Y
            {-1, 0, 1, 2},                 // "I" - X
            {0, 0, 0, 0}                   // "I" - Y
    };

    private Random rand = new Random();       // для генерации случайных чисел
    private int color;
    private int type;
    private int[] x, y;           // массив тайлов
    private int[] xt, yt;           // резервный массив тайлов
    private int left, top;         // положение в "стакане"
    private int leftt, topt;         // резервное положение в "стакане"
    private int angle;                      // Поворот фигуры в градусах
    private boolean falled;
    private int width, height;

    private final PropertiesHolder properties;

    public Figure(PropertiesHolder properties) {
        this.properties = properties;
        initFigure();
    }

    private void initFigure() {
        falled = false;
//        crazy=false;
        width = properties.getBoard().getWidth();
        height = properties.getBoard().getHeight();
        left = width / 2;
        top = 2;
        int k = 4;
        if (properties.isCrazy()) {
            int r = rand.nextInt(10);
            if (r == 0) k = rand.nextInt(3) + 1;
            if (r > 5) k = 5;
        }
        x = new int[k];
        y = new int[k];
        xt = new int[k];
        yt = new int[k];
        type = rand.nextInt(7);
        for (int i = 0; i < 4; i++)
            if (i < x.length) {
                x[i] = figureDef[type * 2][i];
                y[i] = figureDef[type * 2 + 1][i];
            }
        color = rand.nextInt(properties.getColorsCount() - 1) + 1;
        angle = rand.nextInt(3);
        for (int i = 0; i < angle; i++) rotateLeft();

        //тут сделать генерацию пятого элемента
        if (k == 5) {
            int ax = 0, ay = 0;
            boolean f = true;
            do {
                int r = rand.nextInt(5);
                int ra = rand.nextInt(4);
                switch (ra) {
                    case 0:
                        ax = x[r] + 1;
                        ay = y[r];
                        f = in(ax, ay);
                        break;
                    case 1:
                        ax = x[r] - 1;
                        ay = y[r];
                        f = in(ax, ay);
                        break;
                    case 2:
                        ax = x[r];
                        ay = y[r] + 1;
                        f = in(ax, ay);
                        break;
                    case 3:
                        ax = x[r];
                        ay = y[r] - 1;
                        f = in(ax, ay);
                        break;
                }
            } while (f);
            x[4] = ax;
            y[4] = ay;
        }
    }

    private boolean in(int dx, int dy) {
        boolean flag = false;
        for (int i = 0; i < x.length; i++) {
            if ((dx == x[i]) & (dy == y[i])) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    private void backup() {
        leftt = left;
        topt = top;
        xt = Arrays.copyOf(x, x.length);
        yt = Arrays.copyOf(y, y.length);
    }

    private void restore() {
        left = leftt;
        top = topt;
        x = Arrays.copyOf(xt, xt.length);
        y = Arrays.copyOf(yt, yt.length);
    }

    public boolean moveLeft() {
        backup();
        left--;
        if (!correct()) {
            restore();
            return false;
        }
        return true;
    }

    public boolean moveRight() {
        backup();
        left++;
        if (!correct()) {
            restore();
            return false;
        }
        return true;
    }

    public boolean rotateRight() {
        backup();
        int k;
        for (int i = 0; i < x.length; i++) {
            k = x[i];
            x[i] = y[i];
            y[i] = -k;
        }
        if (!correct()) {
            restore();
            return false;
        }
        return true;
    }

    public boolean rotateLeft() {
        backup();
        int k;
        for (int i = 0; i < x.length; i++) {
            k = y[i];
            y[i] = x[i];
            x[i] = -k;
        }
        if (!correct()) {
            restore();
            return false;
        }
        return true;
    }

    public boolean moveDown() {
        boolean result = true;
        backup();
        top++;
        if (!correct()) {
            restore();
            falled = true;
            result = false;
        }
        return result;
    }

    public void fall() {
        do {
            backup();
            top++;
        } while (correct());
        restore();
        falled = true;
    }

    public int ghostTop() {
        int k = top;
        boolean flag = true;
        do {
            k++;
            for (int i = 0; i < x.length; i++) {
                if (y[i] + k >= height) {
                    flag = false;
                    break;
                }
                if (!(properties.getGlass()[x[i] + left][y[i] + k] == properties.getTransparentColor())) {
                    flag = false;
                    break;
                }
            }
        } while (flag);
        return --k;
    }

    public boolean correct() {
        for (int i = 0; i < x.length; i++) {
            if (y[i] + top >= height) return false;
            if (x[i] + left < 0) return false;
            if (x[i] + left >= width) return false;
        }
        int k = 0;
        for (int i = 0; i < x.length; i++) {
            if (properties.getGlass()[x[i] + left][y[i] + top] == properties.getTransparentColor()) k++;
        }
        return (k == x.length);
    }

    public int getColor() {
        return color;
    }

    int getType() {
        return type;
    }

    public int[] getX() {
        return x;
    }

    public int[] getY() {
        return y;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public boolean isFalled() {
        return falled;
    }
}
