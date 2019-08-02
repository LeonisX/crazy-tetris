package md.leonis.tetris.engine;

import java.util.List;

public interface StorageInterface {

    void setFileName(String fileName);

    void save(List<Records.Rec> records);

    List<Records.Rec> load();
}
