package campusdrive;

import campusdrive.storage.DriveIndex;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DriveIndexTest {

    @Test
    void putAndLookupStoreMapping() {
        DriveIndex index =
                new DriveIndex();

        index.put("a", "hash-a");

        assertEquals(
                "hash-a",
                index.lookup("a")
        );
    }

    @Test
    void putReplacesExistingMapping() {
        DriveIndex index =
                new DriveIndex();

        index.put("a", "old-hash");
        index.put("a", "new-hash");

        assertEquals(
                "new-hash",
                index.lookup("a")
        );
    }

    @Test
    void deleteAndLiveHashesReflectCurrentState() {
        DriveIndex index =
                new DriveIndex();

        index.put("a", "hash-1");
        index.put("b", "hash-2");
        index.put("c", "hash-2");

        index.delete("a");

        assertNull(index.lookup("a"));

        assertEquals(
                Set.of("hash-2"),
                index.allLiveBlobHashes()
        );
    }
}