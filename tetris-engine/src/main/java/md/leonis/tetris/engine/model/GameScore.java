package md.leonis.tetris.engine.model;

import md.leonis.tetris.engine.config.ScoreConfig;

public class GameScore {

    private ScoreConfig scoreConfig;
    private int initialLevel;
    private int level;
    private int score;
    private int lines;

    public GameScore(ScoreConfig scoreConfig) {
        this.scoreConfig = scoreConfig;
        this.initialLevel = scoreConfig.getStartLevel();
        this.level = scoreConfig.getStartLevel();
        this.score = scoreConfig.getInitialScore();
        this.lines = scoreConfig.getInitialLines();
    }

    public void countCompletedRows(int completedRows) {
        this.lines += completedRows;
        this.score += scoreConfig.getCompletedRowsBonusList().get(completedRows);
        updateLevel();
    }

    public void countFigure(int figureType) {
        this.score += scoreConfig.getFigureScoreList().get(figureType);
        updateLevel();
    }

    private void updateLevel() {
        this.level = initialLevel + score / scoreConfig.getNextLevel();
    }

    public void levelUp() {
        initialLevel++;
    }

    public int getInitialLevel() {
        return initialLevel;
    }

    public void setInitialLevel(int initialLevel) {
        this.initialLevel = initialLevel;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }
}
