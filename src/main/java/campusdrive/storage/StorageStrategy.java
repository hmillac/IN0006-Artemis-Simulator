package campusdrive.storage;

import campusdrive.exception.NotFoundException;

public interface StorageStrategy {

    void store(String fileId, byte[] content);

    byte[] load(String fileId)
            throws NotFoundException;

    void delete(String fileId);
}