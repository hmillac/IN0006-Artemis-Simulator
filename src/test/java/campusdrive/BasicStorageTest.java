package campusdrive;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;
import campusdrive.infrastructure.InMemoryFileStore;
import campusdrive.storage.BasicStorage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BasicStorageTest {

    @Test
    void storesAndLoadsContent()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        BasicStorage storage =
                new BasicStorage(fileStore);

        byte[] content =
                "hello".getBytes(
                        StandardCharsets.UTF_8
                );

        storage.store("a", content);

        assertArrayEquals(
                content,
                storage.load("a")
        );
    }

    @Test
    void overwritesExistingContent()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        BasicStorage storage =
                new BasicStorage(fileStore);

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
    }

    @Test
    void overwriteDoesNotConcatenate()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        BasicStorage storage =
                new BasicStorage(fileStore);

        storage.store(
                "a",
                "hello".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        storage.store(
                "a",
                "bye".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(
                3,
                storage.load("a").length
        );
    }

    @Test
    void loadUnknownThrowsException() {

        BasicStorage storage =
                new BasicStorage(
                        new InMemoryFileStore()
                );

        assertThrows(
                NotFoundException.class,
                () -> storage.load("missing")
        );
    }

    @Test
    void deleteRemovesContent()
            throws Exception {

        BasicStorage storage =
                new BasicStorage(
                        new InMemoryFileStore()
                );

        storage.store(
                "a",
                new byte[]{1, 2, 3}
        );

        storage.delete("a");

        assertThrows(
                NotFoundException.class,
                () -> storage.load("a")
        );
    }

    @Test
    void deletingUnknownFileDoesNotFail() {

        BasicStorage storage =
                new BasicStorage(
                        new InMemoryFileStore()
                );

        assertDoesNotThrow(
                () -> storage.delete("unknown")
        );
    }
}