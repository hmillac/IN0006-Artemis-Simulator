package campusdrive.infrastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryFileStore implements FileStore {

    private final Map<String, byte[]> files =
            new HashMap<>();

    @Override
    public synchronized boolean exists(String path) {
        return files.containsKey(path);
    }

    @Override
    public synchronized void append(
            String path,
            byte[] content
    ) {
        byte[] existing =
                files.getOrDefault(
                        path,
                        new byte[0]
                );

        byte[] combined =
                Arrays.copyOf(
                        existing,
                        existing.length
                                + content.length
                );

        System.arraycopy(
                content,
                0,
                combined,
                existing.length,
                content.length
        );

        files.put(path, combined);
    }

    @Override
    public synchronized byte[] readAll(String path) {
        byte[] content = files.get(path);

        if (content == null) {
            return null;
        }

        return Arrays.copyOf(
                content,
                content.length
        );
    }

    @Override
    public synchronized void delete(String path) {
        files.remove(path);
    }

    @Override
    public synchronized List<String> list(
            String prefix
    ) {
        List<String> result =
                new ArrayList<>();

        for (String path : files.keySet()) {
            if (path.startsWith(prefix)) {
                result.add(path);
            }
        }

        result.sort(
                Comparator.naturalOrder()
        );

        return result;
    }
}