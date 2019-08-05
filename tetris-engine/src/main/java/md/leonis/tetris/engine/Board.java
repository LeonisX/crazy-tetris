package md.leonis.tetris.engine;

import md.leonis.tetris.engine.model.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Board {

    private final int width;
    private final int height;
    private final int transparentColor;

    private volatile Glass glass;

    private List<Integer> completedRows = new ArrayList<>();

    Board(PropertiesHolder properties) {
        this.width = properties.getWidth();
        this.height = properties.getHeight();
        this.transparentColor = properties.getTransparentColor();
        glass = new Glass();
    }

    void mergeFigure(Figure figure) {
        for (Coordinate coordinate: figure.getCoordinates()) {
            glass.set(coordinate.getX() + figure.getLeft(), coordinate.getY() + figure.getTop(), figure.getColor());
        }
    }

    void deleteCompletedRows() {
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

    Board.Glass getGlass() {
        return glass;
    }

    List<Integer> getCompletedRows() {
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
