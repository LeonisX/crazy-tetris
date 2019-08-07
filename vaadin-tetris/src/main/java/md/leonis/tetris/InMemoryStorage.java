package md.leonis.tetris;

import md.leonis.tetris.engine.Records;
import md.leonis.tetris.engine.StorageInterface;

import java.util.ArrayList;
import java.util.List;

public class InMemoryStorage implements StorageInterface {

    private List<Records.Rec> records = new ArrayList<>();

    @Override
    public void setRecordsStorageName(String storageName) {
    }

    @Override
    public void saveRecord(List<Records.Rec> records) {
        this.records = records;
    }

    @Override
    public List<Records.Rec> loadRecords() {
        return records;
    }
}
