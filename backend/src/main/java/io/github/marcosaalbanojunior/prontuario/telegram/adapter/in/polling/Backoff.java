package io.github.marcosaalbanojunior.prontuario.telegram.adapter.in.polling;

import java.time.Duration;

/** Exponential wait after consecutive failures: 1 s, 2 s, 4 s ... up to 60 s. */
final class Backoff {

    static final Duration INITIAL = Duration.ofSeconds(1);
    static final Duration MAX = Duration.ofSeconds(60);

    private Duration next = INITIAL;

    Duration next() {
        var current = next;
        next = next.multipliedBy(2).compareTo(MAX) > 0 ? MAX : next.multipliedBy(2);
        return current;
    }

    void reset() {
        next = INITIAL;
    }
}
