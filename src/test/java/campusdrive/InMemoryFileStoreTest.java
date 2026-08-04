package campusdrive;

import campusdrive.infrastructure.FileStore;
import campusdrive.infrastructure.InMemoryFileStore;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFileStoreTest {

    @Test
    void unknownPathDoesNotExist() {
        FileStore store =
                new InMemoryFileStore();

        assertFalse(
                store.exists("files/a")
        );
    }

    @Test
    void appendCreatesNewEntry() {
        FileStore store =
                new InMemoryFileStore();

        store.append(
                "files/a",
                "hello".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        assertTrue(
                store.exists("files/a")
        );

        assertArrayEquals(
                "hello".getBytes(
                        StandardCharsets.UTF_8
                ),
                store.readAll("files/a")
        );
    }

    @Test
    void appendConcatenatesExistingEntry() {
        FileStore store =
                new InMemoryFileStore();

        store.append(
                "files/a",
                "hello".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        store.append(
                "files/a",
                " world".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        assertArrayEquals(
                "hello world".getBytes(
                        StandardCharsets.UTF_8
                ),
                store.readAll("files/a")
        );
    }

    @Test
    void deleteRemovesEntry() {
        FileStore store =
                new InMemoryFileStore();

        store.append(
                "files/a",
                new byte[]{1, 2, 3}
        );

        store.delete("files/a");

        assertFalse(
                store.exists("files/a")
        );

        assertNull(
                store.readAll("files/a")
        );
    }

    @Test
    void listReturnsOnlyMatchingPrefix() {
        FileStore store =
                new InMemoryFileStore();

        store.append(
                "blobs/h1",
                new byte[]{1}
        );

        store.append(
                "blobs/h2",
                new byte[]{2}
        );

        store.append(
                "refs/a",
                new byte[]{3}
        );

        assertEquals(
                List.of(
                        "blobs/h1",
                        "blobs/h2"
                ),
                store.list("blobs/")
        );
    }

    @Test
    void readAllReturnsDefensiveCopy() {
        FileStore store =
                new InMemoryFileStore();

        store.append(
                "files/a",
                new byte[]{1, 2, 3}
        );

        byte[] returned =
                store.readAll("files/a");

        returned[0] = 99;

        assertArrayEquals(
                new byte[]{1, 2, 3},
                store.readAll("files/a")
        );
    }
}