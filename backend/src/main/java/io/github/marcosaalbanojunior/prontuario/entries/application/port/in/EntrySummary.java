package io.github.marcosaalbanojunior.prontuario.entries.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A recorded event as other modules see it. Enum values are their stored codes
 * ({@code water}, {@code right_knee}, {@code ml}) so callers can map them to labels.
 */
public record EntrySummary(
        UUID eventId,
        String type,
        Optional<String> concept,
        Optional<String> bodyRegion,
        Optional<BigDecimal> value,
        Optional<String> unit,
        Optional<Integer> intensity,
        Instant occurredStart,
        Optional<Instant> occurredEnd) {}
