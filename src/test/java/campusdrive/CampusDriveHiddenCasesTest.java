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

class CampusDriveHiddenCasesTest {

    @Test
    void repeatedOverwriteKeepsOnlyLatestBasicContent()
            throws Exception {

        CampusDriveService service =
                new CampusDriveService(
                        new InMemoryFileStore(),
                        false
                );

        service.upload(
                "a",
                "v1".getBytes(StandardCharsets.UTF_8)
        );

        service.upload(
                "a",
                "v2".getBytes(StandardCharsets.UTF_8)
        );

        service.upload(
                "a",
                "v3".getBytes(StandardCharsets.UTF_8)
        );

        assertArrayEquals(
                "v3".getBytes(StandardCharsets.UTF_8),
                service.download("a")
        );
    }

    @Test
    void repeatedOverwriteKeepsOnlyLatestDeduplicatedReference()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        service.upload(
                "a",
                "first".getBytes(StandardCharsets.UTF_8)
        );

        service.upload(
                "a",
                "second".getBytes(StandardCharsets.UTF_8)
        );

        service.upload(
                "a",
                "third".getBytes(StandardCharsets.UTF_8)
        );

        assertArrayEquals(
                "third".getBytes(StandardCharsets.UTF_8),
                service.download("a")
        );

        assertEquals(
                1,
                fileStore.list("refs/").size()
        );
    }

    @Test
    void recoveryAfterMultipleOverwritesReturnsLatestVersion()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        service.upload(
                "a",
                "old".getBytes(StandardCharsets.UTF_8)
        );

        service.upload(
                "a",
                "new".getBytes(StandardCharsets.UTF_8)
        );

        CampusDriveService recovered =
                CampusDriveService.recover(fileStore);

        assertArrayEquals(
                "new".getBytes(StandardCharsets.UTF_8),
                recovered.download("a")
        );
    }

    @Test
    void deleteUnknownFileIsLoggedButRemainsAbsent()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        false
                );

        service.delete("missing");

        CampusDriveService recovered =
                CampusDriveService.recover(fileStore);

        assertThrows(
                NotFoundException.class,
                () -> recovered.download("missing")
        );

        assertEquals(
                List.of("DEL missing"),
                new MetaLog(fileStore).readAllLines()
        );
    }

    @Test
    void deleteThenRecoverThenUploadSameIdWorks()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        service.upload(
                "a",
                "old".getBytes(StandardCharsets.UTF_8)
        );

        service.delete("a");

        CampusDriveService recovered =
                CampusDriveService.recover(fileStore);

        recovered.upload(
                "a",
                "new".getBytes(StandardCharsets.UTF_8)
        );

        assertArrayEquals(
                "new".getBytes(StandardCharsets.UTF_8),
                recovered.download("a")
        );
    }

    @Test
    void garbageCollectionDoesNothingForBasicStorage()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        false
                );

        byte[] content =
                "basic-content".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", content);

        assertDoesNotThrow(
                service::garbageCollect
        );

        assertArrayEquals(
                content,
                service.download("a")
        );
    }

    @Test
    void sharedBlobSurvivesWhenOneReferenceIsDeleted()
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

        service.upload("a", shared);
        service.upload("b", shared);

        service.delete("a");
        service.garbageCollect();

        assertArrayEquals(
                shared,
                service.download("b")
        );

        assertEquals(
                1,
                fileStore.list("blobs/").size()
        );
    }

    @Test
    void blobIsDeletedAfterLastReferenceDisappears()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        byte[] content =
                "temporary".getBytes(
                        StandardCharsets.UTF_8
                );

        String hash =
                ContentHash.hash(content);

        service.upload("a", content);
        service.upload("b", content);

        service.delete("a");
        service.delete("b");

        service.garbageCollect();

        assertFalse(
                fileStore.exists("blobs/" + hash)
        );

        assertEquals(
                0,
                fileStore.list("blobs/").size()
        );
    }

    @Test
    void recoveryAfterGarbageCollectionPreservesLiveFiles()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        service.upload(
                "live",
                "live-data".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        service.upload(
                "dead",
                "dead-data".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        service.delete("dead");
        service.garbageCollect();

        CampusDriveService recovered =
                CampusDriveService.recover(fileStore);

        assertArrayEquals(
                "live-data".getBytes(
                        StandardCharsets.UTF_8
                ),
                recovered.download("live")
        );

        assertThrows(
                NotFoundException.class,
                () -> recovered.download("dead")
        );
    }

    @Test
    void metadataLogKeepsCompleteOperationOrder()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        false
                );

        byte[] first =
                "first".getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] second =
                "second".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", first);
        service.upload("b", second);
        service.delete("a");
        service.upload("a", second);

        assertEquals(
                List.of(
                        "PUT a " + ContentHash.hash(first),
                        "PUT b " + ContentHash.hash(second),
                        "DEL a",
                        "PUT a " + ContentHash.hash(second)
                ),
                new MetaLog(fileStore).readAllLines()
        );
    }
}