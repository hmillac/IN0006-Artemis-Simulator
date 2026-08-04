package campusdrive.storage;

import campusdrive.infrastructure.FileStore;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MetaLog {

    private static final String LOG_PATH =
            "meta/log.txt";

    private final FileStore fileStore;

    public MetaLog(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    public void appendPut(
            String fileId,
            String hash
    ) {
        String line =
                "PUT "
                + fileId
                + " "
                + hash
                + "\n";

        fileStore.append(
                LOG_PATH,
                line.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    public void appendDel(String fileId) {
        String line =
                "DEL "
                + fileId
                + "\n";

        fileStore.append(
                LOG_PATH,
                line.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    public List<String> readAllLines() {
        if (!fileStore.exists(LOG_PATH)) {
            return List.of();
        }

        String content =
                new String(
                        fileStore.readAll(LOG_PATH),
                        StandardCharsets.UTF_8
                );

        return content.lines().toList();
    }
}