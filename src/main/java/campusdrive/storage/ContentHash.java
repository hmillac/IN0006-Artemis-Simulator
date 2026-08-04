package campusdrive.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ContentHash {

    private ContentHash() {
        // Utility class
    }

    public static String hash(byte[] content) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] result =
                    digest.digest(content);

            return HexFormat.of()
                    .formatHex(result);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
    }
}