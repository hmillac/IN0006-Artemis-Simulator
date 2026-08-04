package campusdrive.storage;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class DeduplicatingStorage
        implements StorageStrategy {

    private static final String BLOB_PREFIX =
            "blobs/";

    private static final String REF_PREFIX =
            "refs/";

    private final FileStore fileStore;

    public DeduplicatingStorage(
            FileStore fileStore
    ) {
        this.fileStore = fileStore;
    }

    private String blobPath(String hash) {
        return BLOB_PREFIX + hash;
    }

    private String refPath(String fileId) {
        return REF_PREFIX + fileId;
    }

    @Override
    public void store(
            String fileId,
            byte[] content
    ) {
        String hash =
                ContentHash.hash(content);

        String blobPath =
                blobPath(hash);

        String refPath =
                refPath(fileId);

        /*
         * Equal content is physically stored once.
         */
        if (!fileStore.exists(blobPath)) {
            fileStore.append(
                    blobPath,
                    content
            );
        }

        /*
         * A reference must be replaced,
         * not concatenated.
         */
        if (fileStore.exists(refPath)) {
            fileStore.delete(refPath);
        }

        fileStore.append(
                refPath,
                hash.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    @Override
    public byte[] load(String fileId)
            throws NotFoundException {

        String refPath =
                refPath(fileId);

        if (!fileStore.exists(refPath)) {
            throw new NotFoundException(
                    "not found"
            );
        }

        String hash =
                new String(
                        fileStore.readAll(refPath),
                        StandardCharsets.UTF_8
                );

        String blobPath =
                blobPath(hash);

        if (!fileStore.exists(blobPath)) {
            throw new NotFoundException(
                    "not found"
            );
        }

        return fileStore.readAll(blobPath);
    }

    @Override
    public void delete(String fileId) {
        String refPath =
                refPath(fileId);

        /*
         * Delete only the reference.
         * Garbage collection removes orphan blobs later.
         */
        if (fileStore.exists(refPath)) {
            fileStore.delete(refPath);
        }
    }

    public void garbageCollect(
            Set<String> liveHashes
    ) {
        for (String path :
                fileStore.list(BLOB_PREFIX)) {

            String hash =
                    path.substring(
                            BLOB_PREFIX.length()
                    );

            if (!liveHashes.contains(hash)) {
                fileStore.delete(path);
            }
        }
    }
}