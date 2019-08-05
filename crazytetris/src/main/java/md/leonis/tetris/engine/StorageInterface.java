package md.leonis.tetris.engine;

import java.util.List;

public interface StorageInterface {

    void setRecordsFileName(String fileName);

    void saveRecord(List<Records.Rec> records);

    List<Records.Rec> loadRecords();
}
