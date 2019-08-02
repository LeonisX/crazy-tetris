package md.leonis.tetris.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Board {

    private final int width;
    private final int height;
    private final int transparentColor;

    private volatile Glass glass;

    private List<Integer> completedRows = new ArrayList<>();

    public Board(PropertiesHolder properties) {
        this.width = properties.getWidth();
        this.height = properties.getHeight();
        this.transparentColor = properties.getTransparentColor();
        glass = new Glass();
    }

    public void mergeFigure(Figure figure) {
        for (int i = 0; i < figure.getX().length; i++) {
            glass.set(figure.getX()[i] + figure.getLeft(), figure.getY()[i] + figure.getTop(), figure.getColor());
        }
    }

    public void deleteCompletedRows() {
        completedRows = glass.getCompletedRows();
        glass.removeCompletedRowsAndAddEmpty(completedRows);
    }

    boolean isCoordinateNotAllowed(int x, int y) {
        return outOfBound(x, y) || isCoordinateOccupied(x, y);
    }

    private boolean outOfBound(int x, int y) {
        return (x < 0) || (y < 0) || (x >= width) || (y >= height);
    }

    private boolean isCoordinateOccupied(int x, int y) {
        return glass.get(x, y) != transparentColor;
    }

    public Board.Glass getGlass() {
        return glass;
    }

    public List<Integer> getCompletedRows() {
        return completedRows;
    }

    public class Glass {

        private List<Row> rows;

        Glass() {
            rows = IntStream.range(0, height).mapToObj(i -> new Row(width, transparentColor)).collect(Collectors.toList());
        }

        void set(int x, int y, int color) {
            rows.get(y).getElements()[x] = color;
        }

        public int get(int x, int y) {
            return rows.get(y).getElements()[x];
        }

        List<Integer> getCompletedRows() {
            List<Integer> completedRows = new ArrayList<>();
            for (int j = rows.size() - 1; j >= 0; j--) {
                if (rows.get(j).isFull()) {
                    completedRows.add(j);
                }
            }
            return completedRows;
        }

        void removeCompletedRowsAndAddEmpty(List<Integer> completedRows) {
            rows = rows.stream().filter(Row::isNotFull).collect(Collectors.toList());
            completedRows.forEach(i -> rows.add(0, new Row(width, transparentColor)));
        }

        @Override
        public String toString() {
            return "Glass{" +
                    "rows=" + rows.stream().map(row -> row.toString() + "\n").collect(Collectors.joining()) +
                    '}';
        }
    }
}
