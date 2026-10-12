package io.github.marcosaalbanojunior.prontuario.telegram.application.port.in;

/** Fetches pending Telegram updates (long poll) and handles them in order. */
public interface PollUpdates {

    void pollOnce();
}
