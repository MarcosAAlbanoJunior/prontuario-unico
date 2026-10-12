package io.github.marcosaalbanojunior.prontuario.adapter.in.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.application.port.out.telegram.TelegramUnavailableException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PollingRunner")
class PollingRunnerTest {

    private final List<Duration> waits = new CopyOnWriteArrayList<>();

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @ParameterizedTest(name = "polling={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("starts automatically only when polling is enabled")
        void should_followSetting_when_askedForAutoStartup(boolean enabled) {
            assertThat(new PollingRunner(() -> {}, enabled).isAutoStartup()).isEqualTo(enabled);
        }

        @Test
        @DisplayName("can be stopped before it was ever started")
        void should_doNothing_when_stoppedBeforeStart() {
            var runner = new PollingRunner(() -> {}, true);

            runner.stop();

            assertThat(runner.isRunning()).isFalse();
        }
    }

    @Nested
    @DisplayName("polling loop")
    class Loop {

        @Test
        @DisplayName("waits as Telegram asks, backs off on other failures and resets after a success")
        void should_waitAndBackOff_when_pollingFails() throws InterruptedException {
            var calls = new AtomicInteger();
            var done = new CountDownLatch(1);
            var runner = new PollingRunner[1];
            runner[0] = new PollingRunner(() -> {
                switch (calls.incrementAndGet()) {
                    case 1 -> throw new TelegramUnavailableException("429", Optional.of(Duration.ofSeconds(17)), null);
                    case 2 -> throw new TelegramUnavailableException("down", Optional.empty(), null);
                    case 3 -> throw new IllegalStateException("bug");
                    case 4 -> { }
                    case 5 -> throw new IllegalStateException("bug");
                    default -> {
                        runner[0].stop();
                        done.countDown();
                    }
                }
            }, true, waits::add);

            runner[0].start();

            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(waits).containsExactly(Duration.ofSeconds(17), Duration.ofSeconds(1), Duration.ofSeconds(2),
                    Duration.ofSeconds(1));
            assertThat(runner[0].isRunning()).isFalse();
        }

        @Test
        @DisplayName("stops when interrupted while waiting")
        void should_stop_when_interruptedWhileWaiting() throws InterruptedException {
            var stopped = new CountDownLatch(1);
            var runner = new PollingRunner(() -> {
                throw new IllegalStateException("bug");
            }, true, duration -> {
                stopped.countDown();
                throw new InterruptedException();
            });

            runner.start();

            assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
            awaitStopped(runner);
            assertThat(runner.isRunning()).isFalse();
        }
    }

    private static void awaitStopped(PollingRunner runner) throws InterruptedException {
        for (int i = 0; i < 100 && runner.isRunning(); i++) {
            Thread.sleep(10);
        }
    }
}
