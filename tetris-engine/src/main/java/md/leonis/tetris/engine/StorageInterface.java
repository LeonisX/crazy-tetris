package md.leonis.tetris.engine;

import java.util.List;

public interface StorageInterface {

    void setRecordsStorageName(String storageName);

    void saveRecord(List<Records.Rec> records);

    List<Records.Rec> loadRecords();
}
