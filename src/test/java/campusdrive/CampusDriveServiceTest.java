package campusdrive;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;
import campusdrive.infrastructure.InMemoryFileStore;
import campusdrive.service.CampusDriveService;
import campusdrive.storage.ContentHash;
import campusdrive.storage.MetaLog;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CampusDriveServiceTest {

    @Test
    void basicUploadCanBeDownloaded()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        false
                );

        byte[] content =
                "hello".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", content);

        assertArrayEquals(
                content,
                service.download("a")
        );
    }

    @Test
    void basicUploadReplacesExistingFile()
            throws Exception {

        CampusDriveService service =
                new CampusDriveService(
                        new InMemoryFileStore(),
                        false
                );

        service.upload(
                "a",
                "first".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        service.upload(
                "a",
                "second".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        assertArrayEquals(
                "second".getBytes(
                        StandardCharsets.UTF_8
                ),
                service.download("a")
        );
    }

    @Test
    void deleteMakesFileUnavailable()
            throws Exception {

        CampusDriveService service =
                new CampusDriveService(
                        new InMemoryFileStore(),
                        false
                );

        service.upload(
                "a",
                new byte[]{1, 2, 3}
        );

        service.delete("a");

        assertThrows(
                NotFoundException.class,
                () -> service.download("a")
        );
    }

    @Test
    void deduplicatingServiceStoresOneBlob()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        byte[] content =
                "same".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", content);
        service.upload("b", content);

        assertEquals(
                1,
                fileStore.list(
                        "blobs/"
                ).size()
        );

        assertEquals(
                2,
                fileStore.list(
                        "refs/"
                ).size()
        );

        assertArrayEquals(
                content,
                service.download("a")
        );

        assertArrayEquals(
                content,
                service.download("b")
        );
    }

    @Test
    void uploadWritesPutEntryToLog() {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        false
                );

        byte[] content =
                "hello".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", content);

        String hash =
                ContentHash.hash(content);

        MetaLog log =
                new MetaLog(fileStore);

        assertEquals(
                List.of(
                        "PUT a " + hash
                ),
                log.readAllLines()
        );
    }

    @Test
    void deleteWritesDelEntryToLog() {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        false
                );

        byte[] content =
                "hello".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", content);
        service.delete("a");

        String hash =
                ContentHash.hash(content);

        MetaLog log =
                new MetaLog(fileStore);

        assertEquals(
                List.of(
                        "PUT a " + hash,
                        "DEL a"
                ),
                log.readAllLines()
        );
    }

    @Test
    void recoverRestoresBasicStoredFile()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService original =
                new CampusDriveService(
                        fileStore,
                        false
                );

        byte[] content =
                "medical record".getBytes(
                        StandardCharsets.UTF_8
                );

        original.upload("a", content);

        CampusDriveService recovered =
                CampusDriveService.recover(
                        fileStore
                );

        assertArrayEquals(
                content,
                recovered.download("a")
        );
    }

    @Test
    void recoverPreservesDeduplicatingStrategy()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService original =
                new CampusDriveService(
                        fileStore,
                        true
                );

        byte[] content =
                "same".getBytes(
                        StandardCharsets.UTF_8
                );

        original.upload("a", content);

        CampusDriveService recovered =
                CampusDriveService.recover(
                        fileStore
                );

        recovered.upload("b", content);

        /*
         * If recovery incorrectly selected
         * BasicStorage, these paths and counts
         * would not be correct.
         */
        assertEquals(
                1,
                fileStore.list(
                        "blobs/"
                ).size()
        );

        assertEquals(
                2,
                fileStore.list(
                        "refs/"
                ).size()
        );

        assertArrayEquals(
                content,
                recovered.download("b")
        );
    }

    @Test
    void recoveryKeepsDeletedFilesDeleted()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService original =
                new CampusDriveService(
                        fileStore,
                        true
                );

        original.upload(
                "a",
                "record".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        original.delete("a");

        CampusDriveService recovered =
                CampusDriveService.recover(
                        fileStore
                );

        assertThrows(
                NotFoundException.class,
                () -> recovered.download("a")
        );
    }

    @Test
    void garbageCollectionDeletesOnlyOrphans()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        byte[] shared =
                "shared".getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] orphan =
                "orphan".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", shared);
        service.upload("b", shared);
        service.upload("c", orphan);

        service.delete("a");
        service.delete("c");

        service.garbageCollect();

        /*
         * shared remains alive through b.
         */
        assertArrayEquals(
                shared,
                service.download("b")
        );

        assertEquals(
                1,
                fileStore.list(
                        "blobs/"
                ).size()
        );

        assertTrue(
                fileStore.exists(
                        "blobs/"
                                + ContentHash.hash(
                                        shared
                                )
                )
        );

        assertFalse(
                fileStore.exists(
                        "blobs/"
                                + ContentHash.hash(
                                        orphan
                                )
                )
        );
    }
}