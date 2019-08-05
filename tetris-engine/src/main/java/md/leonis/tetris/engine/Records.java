package md.leonis.tetris.engine;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Records {

    private static final int MAX_RECORDS = 10;

    private List<Rec> records;

    @SuppressWarnings("unchecked")
    public Records(StorageInterface storage) {
        records = storage.loadRecords();
        Collections.sort(records);
    }

    public void verifyAndAddScore(String name, int score) {
        if (records.size() < MAX_RECORDS) {
            records.add(new Rec(name, score));
        } else if (score >= records.get(records.size() - 1).score) {
            records.remove(records.get(records.size() - 1));
            records.add(new Rec(name, score));
        }
    }

    public boolean canAddNewRecord(int place) {
        return place <= MAX_RECORDS;
    }

    public int getPlace(int score) {
        return (int) records.stream().filter(r -> r.score > score).count() + 1;
    }

    public List<Rec> getRecords() {
        return records;
    }

    public static class Rec implements Serializable, Comparable {

        String name;
        Integer score;

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
    }
}
