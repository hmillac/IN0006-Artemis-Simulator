package campusdrive.storage;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;

public class BasicStorage
        implements StorageStrategy {

    private static final String PREFIX =
            "files/";

    private final FileStore fileStore;

    public BasicStorage(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    private String pathFor(String fileId) {
        return PREFIX + fileId;
    }

    @Override
    public void store(
            String fileId,
            byte[] content
    ) {
        String path = pathFor(fileId);

        /*
         * FileStore.append() concatena.
         * Para reemplazar:
         * primero delete, luego append.
         */
        if (fileStore.exists(path)) {
            fileStore.delete(path);
        }

        fileStore.append(path, content);
    }

    @Override
    public byte[] load(String fileId)
            throws NotFoundException {

        String path = pathFor(fileId);

        if (!fileStore.exists(path)) {
            throw new NotFoundException(
                    "not found"
            );
        }

        return fileStore.readAll(path);
    }

    @Override
    public void delete(String fileId) {
        String path = pathFor(fileId);

        if (fileStore.exists(path)) {
            fileStore.delete(path);
        }
    }
}