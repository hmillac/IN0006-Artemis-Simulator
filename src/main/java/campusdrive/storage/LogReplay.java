package campusdrive.storage;

public class LogReplay {

    public void replay(
            MetaLog log,
            DriveIndex index
    ) {
        for (String line :
                log.readAllLines()) {

            String[] parts =
                    line.split(" ");

            if (parts.length == 3
                    && "PUT".equals(parts[0])) {

                index.put(
                        parts[1],
                        parts[2]
                );

            } else if (parts.length == 2
                    && "DEL".equals(parts[0])) {

                index.delete(parts[1]);
            }
        }
    }
}