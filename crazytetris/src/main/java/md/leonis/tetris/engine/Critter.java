package md.leonis.tetris.engine;

import md.leonis.tetris.Tetris;

import static md.leonis.tetris.engine.CritterState.*;

//TODO need thread???
public class Critter extends Thread {

    private CritterState status;
    private double air;
    private int count;
    private int placeCount;

    private int x, y, dx, dy;
    private int maxJump = 2;
    private int jumpPower = 0, jumpPowert;
    private int direction = 1;        // 1 - вправо, -1 - влево, 0 - никуда
    private int verticalDirection = 0; // аналогично
    private boolean paused = false;
    private int[][] glass;
    private int width, height;
    private Figure figure = null;
    private int speed;
    private int[][] v;

    private Tetris tetris;

    public Critter(Tetris tetris) {
        this.tetris = tetris;
        this.glass = tetris.getBoard().getGlass();
        width = glass.length;
        height = glass[0].length;
//        System.out.println(width+"|"+height);
        x = 0;
        y = 0;
        air = 100; //сделать учёт воздуха относительно полости
        status = MOVING;
    }

    public void setFigure(Figure figure) {
        this.figure = figure;
    }

    public void run() {
        while (status != DEAD) {
            if (verticalDirection == 0) speed = 400;
            else speed = 80;
            if (paused) speed = 10;
            try {
                sleep(speed);
            } catch (InterruptedException e) {
                //TODO
            }
            if (!isCorrect(x, y)) status = DEAD;
            airControl();
            if (!paused) makeMove();
        }
    }

    private void airControl() {
        if ((paused) || (status == DEAD)) return;
        final double airMOVING = 2;        //  /2
        final double airSTAYING = 5;
        final double airBLOCKED = 1;    //делимое
        final double airFALLING = 0;
        final double airJUMPING = 10;    //вычитается
        double ak;
        if (status == MOVING || status == STAYING) {
            ak = airSTAYING;
        } else {
            ak = 0;
        }
        switch (status) {
            //TODO
            case DEAD:
                air = 0;
                break;
            case MOVING:
                ak /= airMOVING;
                break;
            case FALLING:
                ak = airFALLING;
                break;
            case JUMPING:
                ak = -airJUMPING;
                break;
        }
        if (isBlocked()) {
            ak = -airBLOCKED / placeCount / airMOVING;
            if (status == STAYING) ak /= 2;
//            System.out.println(ak);
        }
        air += ak;
        if (air > 100.0) air = 100.0;
        setState();
        if (air <= 0.0) {
            air = 0;
            status = DEAD;
        }
    }

    private void makeMove() {
        if ((paused) || (status == DEAD)) return;
        if (verticalDirection != 0) moveVertical();
        else moveHorizontal();
    }

    private void setState() {
        double k = 80.0;
        if (isBlocked()) k = 50.0;
        if (air > k) status = MOVING;
        else status = STAYING;
    }

    private void moveHorizontal() {
//        System.out.println("preparing for moving to ("+direction+")");
        if (isStanding()) {
            if (!isCorrect(x + direction, y)) {
                if (status == MOVING) {
                    verticalDirection = -1;
                    jumpPower = maxJump;
                }   //System.out.println("will jump!");
            } else {
                if (status == MOVING) {
                    x += direction;
                }
            }    //System.out.println("moved to direction ("+direction+")");
        } else {
//        System.out.println("i'm falling!");
            verticalDirection = 1;
        }
    }

    private boolean isStanding() {
        return ((y + 1 == height) || (!(glass[x][y + 1] == tetris.getTransparentColor())));
    }

    private void moveVertical() {
        switch (verticalDirection) {
            case 1:
//                System.out.println("i'm falling... (in process)");
                if (!isStanding()) {
                    y++;
                    status = FALLING;
                } else {
                    verticalDirection = 0;
                } //System.out.println("falled!");
                break;
            case -1:
//                System.out.println("i'm jumping... (in process)");
                if (!isCorrect(x + direction, y)) {           // если после подъёма нет возможности уйти в сторону
//                    System.out.println("can't move to direction ("+direction+")");
                    if (jumpPower == 0) {
                        direction = -direction;
                    }   // System.out.println("on the top changed direction"); на излёте меняем направление движения
                } else {
                    x += direction;
                    setState();
                    verticalDirection = 0;
                    return;
                }// System.out.println("moved to direction ("+direction+")");есть возможность уйти в сторону? уходим

                if (jumpPower == 0) {
                    verticalDirection = 1;
                    return;
                }            // System.out.println("now will falling");если закончилась высота прыжка

                if (isCorrect(x, y - 1)) {                // если есть куда ещё подниматься
                    status = JUMPING;
                    y--;
                    jumpPower--;
//                    System.out.println("jumped by 1");
                } else {                        // если некуда больше подниматься (потолок)
                    jumpPower = 0;
                    verticalDirection = 1;
//                    System.out.println("Ceiling! Will fall. Changed direction");
                    direction = -direction;
                }
        }
    }

    public boolean isNotCorrect(){
        return !isCorrect(x, y);
    }

    private boolean isCorrect(int dx, int dy){
//        System.out.println("correct: x="+x+" dx="+dx+" y="+y+" dy="+dy);
        if (status == DEAD) return false;
//        System.out.println("correct: i'm living");
        if (dx < 0) return false;
//        System.out.println("correct: dx>=0");
        if (dx >= width) return false;
//        System.out.println("correct: dx<width");
        if (dy >= height) return false;
//        System.out.println("correct: dy<height");
        if (dy < 0) return false;
//        System.out.println("correct: dy>=0");
        if (glass[dx][dy] != tetris.getTransparentColor()) return false;
//        System.out.println("correct: board==black");
        if (figure != null)
            for (int i = 0; i < figure.getX().length; i++)
                if ((dx == (figure.getX()[i] + figure.getLeft())) && (dy == (figure.getY()[i] + figure.getTop()))) return false;
//        System.out.println("correct: not intersect by figure");
//        return result;
        return true;
    }


    private boolean isNotIn(int dx, int dy) {
        boolean flag = false;
        for (int i = 0; i < count; i++) {
            if ((dx == v[0][i]) & (dy == v[1][i])) {
                flag = true;
                break;
            }
        }
        return !flag;
    }

    public boolean isBlocked() {
        v = new int[2][24];
        int ax, ay;
        v[0][0] = x;
        v[1][0] = y;
        count = 1;
        int cn;
        boolean f;
        do {
            f = false;
            cn = count;
            for (int i = 0; i < cn; i++) {
//        System.out.println("value="+x);
//        System.out.println("x="+x+" y="+y);
                ax = v[0][i] + 1;
                ay = v[1][i];
                if (isCorrect(ax, ay)) if (isNotIn(ax, ay)) {
                    v[0][count] = ax;
                    v[1][count] = ay;
                    f = true;
                    ++count;
                }
                ax = v[0][i] - 1;
                ay = v[1][i];
                if (isCorrect(ax, ay)) if (isNotIn(ax, ay)) {
                    v[0][count] = ax;
                    v[1][count] = ay;
                    f = true;
                    ++count;
                }
                ax = v[0][i];
                ay = v[1][i] + 1;
                if (isCorrect(ax, ay)) if (isNotIn(ax, ay)) {
                    v[0][count] = ax;
                    v[1][count] = ay;
                    f = true;
                    ++count;
                }
                ax = v[0][i];
                ay = v[1][i] - 1;
                if (isCorrect(ax, ay)) if (isNotIn(ax, ay)) {
                    v[0][count] = ax;
                    v[1][count] = ay;
                    f = true;
                    ++count;
                }
            }
//        System.out.println(count);
            if (count > 10) f = false;
        } while (f);
        placeCount = count;
        return (placeCount < 10);
    }

    public void correctY(int[] mas, int size) {
//        System.out.println("1: "+y);
        int k = 0;
        for (int i = 0; i < size; i++) {
            if (mas[i] > y) k++;
        }
        y += k;
//        System.out.println("2: "+y);
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public CritterState getStatus() {
        return status;
    }

    public void setStatus(CritterState status) {
        this.status = status;
    }

    public double getAir() {
        return air;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDirection() {
        return direction;
    }
}
