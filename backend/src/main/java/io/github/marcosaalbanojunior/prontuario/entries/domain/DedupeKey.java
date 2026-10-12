package io.github.marcosaalbanojunior.prontuario.entries.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Hash of type, concept, start rounded to the minute and source: the same fact from the same source is stored once. */
public record DedupeKey(String value) {

    public DedupeKey {
        Objects.requireNonNull(value);
    }

    public static DedupeKey of(EventDetails details, UUID sourceId) {
        var material = String.join("|",
                details.type().name(),
                details.concept().orElse(""),
                details.occurredStart().truncatedTo(ChronoUnit.MINUTES).toString(),
                sourceId.toString());
        return new DedupeKey(sha256(material));
    }

    private static String sha256(String text) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}
