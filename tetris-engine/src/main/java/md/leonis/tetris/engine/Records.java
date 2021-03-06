package md.leonis.tetris.engine;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Records {

    private static final int MAX_RECORDS = 10;

    private List<Rec> records;
    private int place;
    private int score;

    @SuppressWarnings("unchecked")
    public Records(StorageInterface storage, int score) {
        records = storage.loadRecords();
        Collections.sort(records);
        this.score = score;
        this.place = (int) records.stream().filter(r -> r.score > score).count() + 1;
    }

    public void verifyAndAddScore(String name, int score) {
        if (records.size() < MAX_RECORDS) {
            records.add(new Rec(name, score));
        } else if (score >= records.get(records.size() - 1).score) {
            records.remove(records.get(records.size() - 1));
            records.add(new Rec(name, score));
        }
    }

    public boolean canAddNewRecord() {
        return place <= MAX_RECORDS;
    }

    public int getPlace() {
        return place;
    }

    public int getScore() {
        return score;
    }

    public List<Rec> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        return records.toString();
    }

    public static class Rec implements Serializable, Comparable {

        private static final long serialVersionUID = 4085619111165046704L;

        String name;
        Integer score;

        public Rec() {
        }

        Rec(String name, Integer score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public Integer getScore() {
            return score;
        }

        @Override
        public int compareTo(Object rec) {
            return (((Rec) rec).score).compareTo(score);
        }

        @Override
        public String toString() {
            return " {" + name + ": " + score + "}";
        }
    }
}
