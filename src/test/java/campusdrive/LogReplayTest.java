package campusdrive;

import campusdrive.infrastructure.InMemoryFileStore;
import campusdrive.storage.DriveIndex;
import campusdrive.storage.LogReplay;
import campusdrive.storage.MetaLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogReplayTest {

    @Test
    void replayAppliesPut() {
        MetaLog log =
                new MetaLog(
                        new InMemoryFileStore()
                );

        log.appendPut("a", "hash-a");

        DriveIndex index =
                new DriveIndex();

        new LogReplay().replay(
                log,
                index
        );

        assertEquals(
                "hash-a",
                index.lookup("a")
        );
    }

    @Test
    void replayUsesLatestPutForSameId() {
        MetaLog log =
                new MetaLog(
                        new InMemoryFileStore()
                );

        log.appendPut("a", "old-hash");
        log.appendPut("a", "new-hash");

        DriveIndex index =
                new DriveIndex();

        new LogReplay().replay(
                log,
                index
        );

        assertEquals(
                "new-hash",
                index.lookup("a")
        );
    }

    @Test
    void replayAppliesPutAndDeleteInOrder() {
        MetaLog log =
                new MetaLog(
                        new InMemoryFileStore()
                );

        log.appendPut("a", "h1");
        log.appendPut("b", "h2");
        log.appendDel("a");

        DriveIndex index =
                new DriveIndex();

        new LogReplay().replay(
                log,
                index
        );

        assertNull(index.lookup("a"));

        assertEquals(
                "h2",
                index.lookup("b")
        );
    }
}