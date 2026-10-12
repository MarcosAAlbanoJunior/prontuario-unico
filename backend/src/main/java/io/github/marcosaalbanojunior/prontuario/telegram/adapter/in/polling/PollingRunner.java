package io.github.marcosaalbanojunior.prontuario.telegram.adapter.in.polling;

import io.github.marcosaalbanojunior.prontuario.telegram.application.port.in.PollUpdates;
import io.github.marcosaalbanojunior.prontuario.telegram.application.port.out.TelegramUnavailableException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/** Long-polls Telegram on a virtual thread for as long as the app runs; no inbound port is needed. */
public final class PollingRunner implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PollingRunner.class);

    private final PollUpdates pollUpdates;
    private final boolean enabled;
    private final Backoff backoff = new Backoff();
    private volatile Thread thread;

    public PollingRunner(PollUpdates pollUpdates, boolean enabled) {
        this.pollUpdates = pollUpdates;
        this.enabled = enabled;
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
        log.warn("Telegram polling paused for {} s: {}", wait.toSeconds(), reason);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            thread = null;
        }
    }
}
