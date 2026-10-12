package io.github.marcosaalbanojunior.prontuario.application.port.in;

/** Fetches pending Telegram updates (long poll) and handles them in order. */
public interface PollUpdatesUseCase {

    void pollOnce();
}
