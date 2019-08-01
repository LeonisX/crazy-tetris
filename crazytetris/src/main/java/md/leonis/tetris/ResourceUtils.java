package md.leonis.tetris;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ResourceUtils {

    public static InputStream getResourceAsStream(String path, boolean isDebug) {
        if (isDebug) {
            try {
                return new BufferedInputStream(new FileInputStream(ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath() + path));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new BufferedInputStream(ResourceUtils.class.getClassLoader().getResourceAsStream(path));
        }
    }
}
