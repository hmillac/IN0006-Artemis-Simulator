package campusdrive.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DriveIndex {

    private final Map<String, String> index =
            new HashMap<>();

    public void put(
            String fileId,
            String hash
    ) {
        index.put(fileId, hash);
    }

    public void delete(String fileId) {
        index.remove(fileId);
    }

    public String lookup(String fileId) {
        return index.get(fileId);
    }

    public Set<String> allLiveBlobHashes() {
        return new HashSet<>(
                index.values()
        );
    }
}