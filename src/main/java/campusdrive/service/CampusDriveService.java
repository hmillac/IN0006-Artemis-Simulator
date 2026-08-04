package campusdrive.service;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;
import campusdrive.storage.BasicStorage;
import campusdrive.storage.ContentHash;
import campusdrive.storage.DeduplicatingStorage;
import campusdrive.storage.DriveIndex;
import campusdrive.storage.LogReplay;
import campusdrive.storage.MetaLog;
import campusdrive.storage.StorageStrategy;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CampusDriveService {

    private static final String CONFIG_PATH =
            "config/storage.txt";

    private static final String BASIC_CONFIG =
            "basic";

    private static final String DEDUPLICATING_CONFIG =
            "deduplicating";

    private final FileStore fileStore;
    private final StorageStrategy storage;
    private final DriveIndex index;
    private final MetaLog metaLog;

    /*
     * The same lock protects upload,
     * download, delete and garbage collection.
     */
    private final Lock lock =
            new ReentrantLock();

    public CampusDriveService(
            FileStore fileStore,
            boolean deduplicating
    ) {
        this.fileStore = fileStore;

        this.storage =
                deduplicating
                        ? new DeduplicatingStorage(
                                fileStore
                        )
                        : new BasicStorage(
                                fileStore
                        );

        this.index =
                new DriveIndex();

        this.metaLog =
                new MetaLog(fileStore);

        /*
         * Do not append the configuration again
         * if it already exists.
         */
        if (!fileStore.exists(CONFIG_PATH)) {
            String value =
                    deduplicating
                            ? DEDUPLICATING_CONFIG
                            : BASIC_CONFIG;

            fileStore.append(
                    CONFIG_PATH,
                    value.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }
    }

    /*
     * Private constructor used by recover().
     * It avoids overwriting or appending
     * the persisted configuration.
     */
    private CampusDriveService(
            FileStore fileStore,
            StorageStrategy storage,
            DriveIndex index,
            MetaLog metaLog
    ) {
        this.fileStore = fileStore;
        this.storage = storage;
        this.index = index;
        this.metaLog = metaLog;
    }

    public void upload(
            String fileId,
            byte[] content
    ) {
        lock.lock();

        try {
            storage.store(
                    fileId,
                    content
            );

            String hash =
                    ContentHash.hash(content);

            index.put(
                    fileId,
                    hash
            );

            metaLog.appendPut(
                    fileId,
                    hash
            );

        } finally {
            lock.unlock();
        }
    }

    public byte[] download(
            String fileId
    ) throws NotFoundException {

        lock.lock();

        try {
            return storage.load(fileId);

        } finally {
            lock.unlock();
        }
    }

    public void delete(String fileId) {
        lock.lock();

        try {
            storage.delete(fileId);

            index.delete(fileId);

            metaLog.appendDel(fileId);

        } finally {
            lock.unlock();
        }
    }

    public void garbageCollect() {
        lock.lock();

        try {
            if (storage instanceof
                    DeduplicatingStorage
                    deduplicatingStorage) {

                deduplicatingStorage
                        .garbageCollect(
                                index
                                    .allLiveBlobHashes()
                        );
            }

            /*
             * BasicStorage:
             * garbage collection is a no-op.
             */

        } finally {
            lock.unlock();
        }
    }

    public static CampusDriveService recover(
            FileStore fileStore
    ) {
        if (!fileStore.exists(CONFIG_PATH)) {
            throw new IllegalStateException(
                    "Missing storage configuration."
            );
        }

        String configuration =
                new String(
                        fileStore.readAll(
                                CONFIG_PATH
                        ),
                        StandardCharsets.UTF_8
                ).trim();

        StorageStrategy storage;

        if (DEDUPLICATING_CONFIG.equals(
                configuration
        )) {
            storage =
                    new DeduplicatingStorage(
                            fileStore
                    );

        } else if (BASIC_CONFIG.equals(
                configuration
        )) {
            storage =
                    new BasicStorage(
                            fileStore
                    );

        } else {
            throw new IllegalStateException(
                    "Unknown storage configuration: "
                            + configuration
            );
        }

        MetaLog metaLog =
                new MetaLog(fileStore);

        DriveIndex index =
                new DriveIndex();

        new LogReplay().replay(
                metaLog,
                index
        );

        return new CampusDriveService(
                fileStore,
                storage,
                index,
                metaLog
        );
    }
}