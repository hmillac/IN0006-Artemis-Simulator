package campusdrive.infrastructure;

import java.util.List;

public interface FileStore {

    boolean exists(String path);

    void append(String path, byte[] content);

    byte[] readAll(String path);

    void delete(String path);

    List<String> list(String prefix);
}