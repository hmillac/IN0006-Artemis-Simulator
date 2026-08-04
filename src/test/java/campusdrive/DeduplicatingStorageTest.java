package campusdrive;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;
import campusdrive.infrastructure.InMemoryFileStore;
import campusdrive.storage.ContentHash;
import campusdrive.storage.DeduplicatingStorage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicatingStorageTest {

    @Test
    void storesAndLoadsContent()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        DeduplicatingStorage storage =
                new DeduplicatingStorage(
                        fileStore
                );

        byte[] content =
                "medical record".getBytes(
                        StandardCharsets.UTF_8
                );

        storage.store("a", content);

        assertArrayEquals(
                content,
                storage.load("a")
        );
    }

    @Test
    void equalContentsCreateOnlyOneBlob() {
        FileStore fileStore =
                new InMemoryFileStore();

        DeduplicatingStorage storage =
                new DeduplicatingStorage(
                        fileStore
                );

        byte[] content =
                "same content".getBytes(
                        StandardCharsets.UTF_8
                );

        storage.store("a", content);
        storage.store("b", content);

        assertEquals(
                1,
                fileStore.list("blobs/").size()
        );

        assertEquals(
                2,
                fileStore.list("refs/").size()
        );
    }

    @Test
    void replacingExistingIdDoesNotConcatenateReference()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        DeduplicatingStorage storage =
                new DeduplicatingStorage(
                        fileStore
                );

        storage.store(
                "a",
                "first".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        storage.store(
                "a",
                "second".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        assertArrayEquals(
                "second".getBytes(
                        StandardCharsets.UTF_8
                ),
                storage.load("a")
        );

        assertEquals(
                ContentHash.hash(
                        "second".getBytes(
                                StandardCharsets.UTF_8
                        )
                ).length(),
                fileStore.readAll(
                        "refs/a"
                ).length
        );
    }

    @Test
    void deletingOneReferencePreservesSharedBlob()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        DeduplicatingStorage storage =
                new DeduplicatingStorage(
                        fileStore
                );

        byte[] content =
                "shared".getBytes(
                        StandardCharsets.UTF_8
                );

        storage.store("a", content);
        storage.store("b", content);

        storage.delete("a");

        assertThrows(
                NotFoundException.class,
                () -> storage.load("a")
        );

        assertArrayEquals(
                content,
                storage.load("b")
        );

        assertEquals(
                1,
                fileStore.list("blobs/").size()
        );
    }

    @Test
    void deleteUnknownDoesNotFail() {
        DeduplicatingStorage storage =
                new DeduplicatingStorage(
                        new InMemoryFileStore()
                );

        assertDoesNotThrow(
                () -> storage.delete("unknown")
        );
    }

    @Test
    void loadUnknownThrowsException() {
        DeduplicatingStorage storage =
                new DeduplicatingStorage(
                        new InMemoryFileStore()
                );

        assertThrows(
                NotFoundException.class,
                () -> storage.load("missing")
        );
    }

    @Test
    void garbageCollectionDeletesOnlyOrphanBlob()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        DeduplicatingStorage storage =
                new DeduplicatingStorage(
                        fileStore
                );

        byte[] liveContent =
                "live".getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] orphanContent =
                "orphan".getBytes(
                        StandardCharsets.UTF_8
                );

        storage.store("live-id", liveContent);
        storage.store("orphan-id", orphanContent);

        String liveHash =
                ContentHash.hash(liveContent);

        storage.delete("orphan-id");

        storage.garbageCollect(
                Set.of(liveHash)
        );

        assertArrayEquals(
                liveContent,
                storage.load("live-id")
        );

        assertEquals(
                1,
                fileStore.list("blobs/").size()
        );

        assertTrue(
                fileStore.exists(
                        "blobs/" + liveHash
                )
        );
    }
}