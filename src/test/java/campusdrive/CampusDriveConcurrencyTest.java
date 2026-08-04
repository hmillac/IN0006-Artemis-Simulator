package campusdrive;

import campusdrive.exception.NotFoundException;
import campusdrive.infrastructure.FileStore;
import campusdrive.infrastructure.InMemoryFileStore;
import campusdrive.service.CampusDriveService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class CampusDriveConcurrencyTest {

    @Test
    void concurrentUploadsOfDifferentFilesRemainAvailable()
            throws Exception {

        CampusDriveService service =
                new CampusDriveService(
                        new InMemoryFileStore(),
                        false
                );

        int numberOfFiles = 20;

        ExecutorService executor =
                Executors.newFixedThreadPool(8);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<?>> futures =
                new ArrayList<>();

        for (int i = 0; i < numberOfFiles; i++) {
            int fileNumber = i;

            futures.add(
                    executor.submit(() -> {
                        start.await();

                        service.upload(
                                "file-" + fileNumber,
                                ("content-" + fileNumber)
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        );

                        return null;
                    })
            );
        }

        start.countDown();

        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();

        for (int i = 0; i < numberOfFiles; i++) {
            assertArrayEquals(
                    ("content-" + i)
                            .getBytes(
                                    StandardCharsets.UTF_8
                            ),
                    service.download(
                            "file-" + i
                    )
            );
        }
    }

    @Test
    void concurrentUploadsToSameIdProduceOneCompleteVersion()
            throws Exception {

        CampusDriveService service =
                new CampusDriveService(
                        new InMemoryFileStore(),
                        false
                );

        byte[] versionA =
                "version-A".getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] versionB =
                "version-B-longer".getBytes(
                        StandardCharsets.UTF_8
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start =
                new CountDownLatch(1);

        Future<?> first =
                executor.submit(() -> {
                    start.await();
                    service.upload("shared", versionA);
                    return null;
                });

        Future<?> second =
                executor.submit(() -> {
                    start.await();
                    service.upload("shared", versionB);
                    return null;
                });

        start.countDown();

        first.get();
        second.get();

        executor.shutdown();

        byte[] stored =
                service.download("shared");

        boolean isVersionA =
                java.util.Arrays.equals(
                        stored,
                        versionA
                );

        boolean isVersionB =
                java.util.Arrays.equals(
                        stored,
                        versionB
                );

        assertTrue(
                isVersionA || isVersionB,
                "The final file must equal one complete uploaded version."
        );
    }

    @Test
    void concurrentUploadAndDeleteDoNotCorruptStorage()
            throws Exception {

        CampusDriveService service =
                new CampusDriveService(
                        new InMemoryFileStore(),
                        false
                );

        service.upload(
                "a",
                "initial".getBytes(
                        StandardCharsets.UTF_8
                )
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch start =
                new CountDownLatch(1);

        Future<?> upload =
                executor.submit(() -> {
                    start.await();

                    service.upload(
                            "a",
                            "replacement".getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

                    return null;
                });

        Future<?> delete =
                executor.submit(() -> {
                    start.await();
                    service.delete("a");
                    return null;
                });

        start.countDown();

        upload.get();
        delete.get();

        executor.shutdown();

        try {
            byte[] content =
                    service.download("a");

            assertArrayEquals(
                    "replacement".getBytes(
                            StandardCharsets.UTF_8
                    ),
                    content
            );

        } catch (NotFoundException exception) {
            /*
             * Also valid:
             * delete may be the last serialized operation.
             */
        }
    }

    @Test
    void deduplicatingConcurrentUploadsKeepSingleBlob()
            throws Exception {

        FileStore fileStore =
                new InMemoryFileStore();

        CampusDriveService service =
                new CampusDriveService(
                        fileStore,
                        true
                );

        byte[] sharedContent =
                "shared-content".getBytes(
                        StandardCharsets.UTF_8
                );

        int uploads = 25;

        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<?>> futures =
                new ArrayList<>();

        for (int i = 0; i < uploads; i++) {
            int fileNumber = i;

            futures.add(
                    executor.submit(() -> {
                        start.await();

                        service.upload(
                                "file-" + fileNumber,
                                sharedContent
                        );

                        return null;
                    })
            );
        }

        start.countDown();

        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();

        assertEquals(
                1,
                fileStore.list("blobs/").size()
        );

        assertEquals(
                uploads,
                fileStore.list("refs/").size()
        );

        for (int i = 0; i < uploads; i++) {
            assertArrayEquals(
                    sharedContent,
                    service.download(
                            "file-" + i
                    )
            );
        }
    }

    @Test
    void concurrentOperationsFinishWithoutDeadlock() {

        assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> {

                    CampusDriveService service =
                            new CampusDriveService(
                                    new InMemoryFileStore(),
                                    true
                            );

                    ExecutorService executor =
                            Executors.newFixedThreadPool(8);

                    CountDownLatch start =
                            new CountDownLatch(1);

                    List<Future<?>> futures =
                            new ArrayList<>();

                    for (int i = 0; i < 50; i++) {
                        int operation = i;

                        futures.add(
                                executor.submit(() -> {
                                    start.await();

                                    String id =
                                            "file-"
                                            + (operation % 10);

                                    if (operation % 3 == 0) {
                                        service.upload(
                                                id,
                                                ("data-" + operation)
                                                        .getBytes(
                                                                StandardCharsets.UTF_8
                                                        )
                                        );

                                    } else if (operation % 3 == 1) {
                                        try {
                                            service.download(id);
                                        } catch (
                                                NotFoundException ignored
                                        ) {
                                            // Allowed during concurrent access.
                                        }

                                    } else {
                                        service.delete(id);
                                    }

                                    return null;
                                })
                        );
                    }

                    start.countDown();

                    for (Future<?> future : futures) {
                        future.get();
                    }

                    executor.shutdown();
                }
        );
    }
}