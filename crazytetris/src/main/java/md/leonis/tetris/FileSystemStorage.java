package md.leonis.tetris;

import md.leonis.tetris.engine.Records;
import md.leonis.tetris.engine.StorageInterface;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileSystemStorage implements StorageInterface {

    private String fileName;

    @Override
    public void setRecordsFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void saveRecord(List<Records.Rec> records) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(records);
            oos.close();
        } catch (IOException e) {
            //TODO
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Records.Rec> loadRecords() {
        List<Records.Rec> records = null;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            records = (List<Records.Rec>) ois.readObject();
            ois.close();
        } catch (FileNotFoundException e) {
            records = new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            //TODO
            e.printStackTrace();
        }
        return records;
    }

    static InputStream getResourceAsStream(String path, boolean isDebug) {
        if (isDebug) {
            try {
                return new BufferedInputStream(new FileInputStream(FileSystemStorage.class.getProtectionDomain().getCodeSource().getLocation().getPath() + path));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new BufferedInputStream(FileSystemStorage.class.getClassLoader().getResourceAsStream(path));
        }
    }
}
