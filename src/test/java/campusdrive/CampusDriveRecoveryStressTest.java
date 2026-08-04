package campusdrive;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;
import campusdrive.infrastructure.InMemoryFileStore;
import campusdrive.service.CampusDriveService;
import campusdrive.storage.ContentHash;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CampusDriveRecoveryStressTest {

    @Test
    void recoveryReconstructsLatestStateAfterLongSequence()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        false
                );

        service.upload(
                "a",
                "version-1".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        service.upload(
                "b",
                "file-b".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        service.upload(
                "a",
                "version-2".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        service.delete("b");

        CampusDriveService recovered =
                CampusDriveService.recover(
                        fileStore
                );

        assertArrayEquals(
                "version-2".getBytes(
                        StandardCharsets.UTF_8
                ),
                recovered.download("a")
        );

        assertThrows(
                NotFoundException.class,
                () -> recovered.download("b")
        );
    }

    @Test
    void recoveredServiceAcceptsNewUploadsAndSecondRecovery()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService original =
                new CampusDriveService(
                        fileStore,
                        false
                );

        original.upload(
                "a",
                "first".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        CampusDriveService firstRecovery =
                CampusDriveService.recover(
                        fileStore
                );

        firstRecovery.upload(
                "b",
                "second".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        CampusDriveService secondRecovery =
                CampusDriveService.recover(
                        fileStore
                );

        assertArrayEquals(
                "first".getBytes(
                        StandardCharsets.UTF_8
                ),
                secondRecovery.download("a")
        );

        assertArrayEquals(
                "second".getBytes(
                        StandardCharsets.UTF_8
                ),
                secondRecovery.download("b")
        );
    }

    @Test
    void recoveryPreservesDeduplicatedSharedContent()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        byte[] shared =
                "shared-content".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("a", shared);
        service.upload("b", shared);

        CampusDriveService recovered =
                CampusDriveService.recover(
                        fileStore
                );

        assertEquals(
                1,
                fileStore.list("blobs/").size()
        );

        assertEquals(
                2,
                fileStore.list("refs/").size()
        );

        assertArrayEquals(
                shared,
                recovered.download("a")
        );

        assertArrayEquals(
                shared,
                recovered.download("b")
        );
    }

    @Test
    void deleteThenReuploadSameIdSurvivesRecovery()
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
                "old-content".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        service.delete("a");

        service.upload(
                "a",
                "new-content".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        CampusDriveService recovered =
                CampusDriveService.recover(
                        fileStore
                );

        assertArrayEquals(
                "new-content".getBytes(
                        StandardCharsets.UTF_8
                ),
                recovered.download("a")
        );
    }

    @Test
    void garbageCollectionAfterRecoveryUsesRebuiltIndex()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        byte[] liveContent =
                "live-content".getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] orphanContent =
                "orphan-content".getBytes(
                        StandardCharsets.UTF_8
                );

        service.upload("live", liveContent);
        service.upload("orphan", orphanContent);

        service.delete("orphan");

        CampusDriveService recovered =
                CampusDriveService.recover(
                        fileStore
                );

        recovered.garbageCollect();

        String liveHash =
                ContentHash.hash(liveContent);

        String orphanHash =
                ContentHash.hash(orphanContent);

        assertArrayEquals(
                liveContent,
                recovered.download("live")
        );

        assertTrue(
                fileStore.exists(
                        "blobs/" + liveHash
                )
        );

        assertFalse(
                fileStore.exists(
                        "blobs/" + orphanHash
                )
        );

        assertEquals(
                1,
                fileStore.list("blobs/").size()
        );
    }
}