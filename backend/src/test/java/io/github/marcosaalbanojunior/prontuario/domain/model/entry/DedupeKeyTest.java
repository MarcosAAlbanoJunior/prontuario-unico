package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DedupeKey")
class DedupeKeyTest {

    private static final Instant START = Instant.parse("2026-10-12T11:00:30Z");
    private static final UUID SOURCE = UUID.fromString("0199d6a0-0000-7000-8000-000000000001");

    @Nested
    @DisplayName("of(...)")
    class Of {

        @Test
        @DisplayName("is a SHA-256 hex digest")
        void should_beSha256Hex_when_created() {
            assertThat(DedupeKey.of(EventTest.details(START, Optional.empty()), SOURCE).value()).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("is the same for the same fact within the same minute and source")
        void should_beEqual_when_sameMinuteAndSource() {
            var first = DedupeKey.of(EventTest.details(START, Optional.empty()), SOURCE);

            assertThat(DedupeKey.of(EventTest.details(START.plusSeconds(20), Optional.empty()), SOURCE)).isEqualTo(first);
        }

        @Test
        @DisplayName("differs for another source")
        void should_differ_when_sourceDiffers() {
            var first = DedupeKey.of(EventTest.details(START, Optional.empty()), SOURCE);

            assertThat(DedupeKey.of(EventTest.details(START, Optional.empty()), UUID.randomUUID())).isNotEqualTo(first);
        }

        @Test
        @DisplayName("ignores a missing concept")
        void should_hash_when_conceptIsMissing() {
            var details = new EventDetails(EventType.WATER, Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), START, Optional.empty(), TimePrecision.EXACT);

            assertThat(DedupeKey.of(details, SOURCE).value()).hasSize(64);
        }
    }

    @Nested
    @DisplayName("hash(...)")
    class Hash {

        @Test
        @DisplayName("fails clearly when the algorithm is not available")
        void should_throwIllegalState_when_algorithmIsUnknown() {
            assertThatThrownBy(() -> DedupeKey.hash("NOPE-256", "x"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("NOPE-256 is not available");
        }
    }
}
