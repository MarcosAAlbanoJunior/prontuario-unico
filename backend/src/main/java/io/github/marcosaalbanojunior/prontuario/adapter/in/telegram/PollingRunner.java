package io.github.marcosaalbanojunior.prontuario.adapter.in.telegram;

import io.github.marcosaalbanojunior.prontuario.application.port.in.PollUpdatesUseCase;
import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.TelegramUnavailableException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

/** Long-polls Telegram on a virtual thread for as long as the app runs; no inbound port is needed. */
@Slf4j
public final class PollingRunner implements SmartLifecycle {

    /** How the runner waits before retrying; tests replace it to avoid real sleeps. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final PollUpdatesUseCase pollUpdates;
    private final boolean enabled;
    private final Sleeper sleeper;
    private final Backoff backoff = new Backoff();
    private volatile Thread thread;

    public PollingRunner(PollUpdatesUseCase pollUpdates, boolean enabled) {
        this(pollUpdates, enabled, Thread::sleep);
    }

    PollingRunner(PollUpdatesUseCase pollUpdates, boolean enabled, Sleeper sleeper) {
        this.pollUpdates = pollUpdates;
        this.enabled = enabled;
        this.sleeper = sleeper;
    }

    // Decided at runtime, not with @Conditional, so the native image honours the setting.
    @Override
    public boolean isAutoStartup() {
        return enabled;
    }

    @Override
    public void start() {
        thread = Thread.ofVirtual().name("telegram-polling").start(this::run);
        log.info("Telegram polling started");
    }

    @Override
    public void stop() {
        var running = thread;
        thread = null;
        if (running != null) {
            running.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return thread != null;
    }

    private void run() {
        while (isRunning()) {
            try {
                pollUpdates.pollOnce();
                backoff.reset();
            } catch (TelegramUnavailableException e) {
                waitBeforeRetry(e.retryAfter().orElseGet(backoff::next), e.getMessage());
            } catch (RuntimeException e) {
                log.error("Telegram polling failed", e);
                waitBeforeRetry(backoff.next(), e.getClass().getSimpleName());
            }
        }
    }

    private void waitBeforeRetry(Duration wait, String reason) {
        log.warn("Telegram polling paused. seconds={} reason={}", wait.toSeconds(), reason);
        try {
            sleeper.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            thread = null;
        }
    }
}
