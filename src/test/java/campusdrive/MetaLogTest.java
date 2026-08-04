package campusdrive;

import campusdrive.infrastructure.FileStore;
import campusdrive.infrastructure.InMemoryFileStore;
import campusdrive.storage.MetaLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaLogTest {

    @Test
    void emptyLogReturnsEmptyList() {
        MetaLog log =
                new MetaLog(
                        new InMemoryFileStore()
                );

        assertEquals(
                List.of(),
                log.readAllLines()
        );
    }

    @Test
    void appendPutUsesExactFormat() {
        FileStore fileStore =
                new InMemoryFileStore();

        MetaLog log =
                new MetaLog(fileStore);

        log.appendPut(
                "file-a",
                "hash-a"
        );

        assertEquals(
                List.of(
                        "PUT file-a hash-a"
                ),
                log.readAllLines()
        );
    }

    @Test
    void preservesOperationOrder() {
        MetaLog log =
                new MetaLog(
                        new InMemoryFileStore()
                );

        log.appendPut("a", "h1");
        log.appendPut("b", "h2");
        log.appendDel("a");

        assertEquals(
                List.of(
                        "PUT a h1",
                        "PUT b h2",
                        "DEL a"
                ),
                log.readAllLines()
        );
    }
}