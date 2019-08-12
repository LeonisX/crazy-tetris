package md.leonis.tetris.engine.config;

import java.util.Arrays;
import java.util.List;

public class ScoreConfig {

    private int startLevel;
    private int initialScore;
    private int initialLines;

    private int nextLevel;

    //                                                     O,  J,  L,  T,  Z,  S,  I
    private List<Integer> figureScoreList = Arrays.asList(10, 15, 15, 15, 20, 20, 10);

    //                                                       0   1    2    3    4
    private List<Integer> completedRowsBonusList = Arrays.asList(0, 100, 250, 400, 600);

    public ScoreConfig() {
        this.startLevel = 0;
        this.initialScore = 0;
        this.initialLines = 0;
        this.nextLevel = 10000; //TODO the game ends very quickly, probably you need to increase
    }

    public int getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(int startLevel) {
        this.startLevel = startLevel;
    }

    public int getInitialScore() {
        return initialScore;
    }

    public void setInitialScore(int initialScore) {
        this.initialScore = initialScore;
    }

    public int getInitialLines() {
        return initialLines;
    }

    public void setInitialLines(int initialLines) {
        this.initialLines = initialLines;
    }

    public int getNextLevel() {
        return nextLevel;
    }

    public void setNextLevel(int nextLevel) {
        this.nextLevel = nextLevel;
    }

    public List<Integer> getFigureScoreList() {
        return figureScoreList;
    }

    public void setFigureScoreList(List<Integer> figureScoreList) {
        this.figureScoreList = figureScoreList;
    }

    public List<Integer> getCompletedRowsBonusList() {
        return completedRowsBonusList;
    }

    public void setCompletedRowsBonusList(List<Integer> completedRowsBonusList) {
        this.completedRowsBonusList = completedRowsBonusList;
    }
}
