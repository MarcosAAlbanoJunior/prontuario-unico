package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Hash of type, concept, start rounded to the minute and source: the same fact from the same source is stored once. */
public record DedupeKey(String value) {

    private static final String SHA_256 = "SHA-256";

    public DedupeKey {
        Objects.requireNonNull(value);
    }

    public static DedupeKey of(EventDetails details, UUID sourceId) {
        var material = String.join("|",
                details.type().name(),
                details.concept().orElse(""),
                details.occurredStart().truncatedTo(ChronoUnit.MINUTES).toString(),
                sourceId.toString());
        return new DedupeKey(hash(SHA_256, material));
    }

    /** Package-private with the algorithm as a parameter so the failure path can be tested. */
    static String hash(String algorithm, String text) {
        try {
            var digest = MessageDigest.getInstance(algorithm).digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is not available", e);
        }
    }
}
