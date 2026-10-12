package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.EntrySummary;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.MissingField;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryType;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RejectionCause;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.UndoResult;
import io.github.marcosaalbanojunior.prontuario.goals.application.port.in.GoalProgressView;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class CommandRouterTest {

    private static final Instant NOW = Instant.parse("2026-10-12T17:32:00Z");

    private final List<QuickEntryRequest> requests = new ArrayList<>();
    private final Deque<QuickEntryResult> results = new ArrayDeque<>();
    private final Deque<UndoResult> undoResults = new ArrayDeque<>();
    private List<GoalProgressView> progress = List.of();

    private final CommandRouter router = new CommandRouter(
            new ChatPorts(request -> { requests.add(request); return results.pop(); }, undoResults::pop, () -> progress),
            new Texts(messages()),
            new PtBrFormat(Clock.fixed(NOW, ZoneId.of("UTC")), ZoneId.of("America/Sao_Paulo")));

    @Test
    void shouldConfirmARecordedEntryInPortuguese() {
        results.push(new QuickEntryResult.Recorded(water(500)));

        var replies = router.respond(message("/agua 500", NOW));

        assertThat(replies).containsExactly(Reply.of("✅ Registrado. Água: 500 mL às 14:32"));
        assertThat(requests.getFirst().type()).isEqualTo(QuickEntryType.WATER);
        assertThat(requests.getFirst().source().rawText()).isEqualTo("/agua 500");
    }

    @Test
    void shouldAskForMissingDataAndUseTheAnswer() {
        results.add(new QuickEntryResult.NeedsMore(MissingField.BODY_REGION));
        results.add(new QuickEntryResult.Recorded(water(1)));

        var question = router.respond(message("/dor 4", NOW));
        router.respond(message("joelho direito", NOW.plusSeconds(30)));

        assertThat(question).containsExactly(Reply.question(
                "Onde é a dor? Ex.: joelho direito 4 (a intensidade de 0 a 10 é opcional)"));
        assertThat(requests).extracting(QuickEntryRequest::args).containsExactly("4", "4 joelho direito");
        assertThat(requests.getLast().source().rawText()).isEqualTo("/dor 4 joelho direito");
    }

    @Test
    void shouldForgetTheQuestionAfterTenMinutes() {
        results.add(new QuickEntryResult.NeedsMore(MissingField.WATER_AMOUNT));

        router.respond(message("/agua", NOW));
        var late = router.respond(message("500", NOW.plusSeconds(600)));

        assertThat(late.getFirst().text()).startsWith("Ainda não entendo texto livre");
        assertThat(requests).hasSize(1);
    }

    @Test
    void shouldDropTheQuestionWhenAnotherCommandArrives() {
        results.add(new QuickEntryResult.NeedsMore(MissingField.WATER_AMOUNT));
        router.respond(message("/agua", NOW));
        router.respond(message("/hoje", NOW));

        var reply = router.respond(message("500", NOW));

        assertThat(reply.getFirst().text()).startsWith("Ainda não entendo texto livre");
    }

    @Test
    void shouldExplainARejectionWithAnExample() {
        results.push(new QuickEntryResult.Rejected(RejectionCause.OUT_OF_RANGE));

        var replies = router.respond(message("/peso 900", NOW));

        assertThat(replies).containsExactly(Reply.of("Esse valor parece fora do esperado. Ex.: /peso 82,4"));
    }

    @Test
    void shouldStaySilentWhenTheMessageWasAlreadyRecorded() {
        results.push(new QuickEntryResult.AlreadyRecorded());

        assertThat(router.respond(message("/agua 500", NOW))).isEmpty();
    }

    @Test
    void shouldShowTodayAndThisWeekGoals() {
        progress = List.of(
                new GoalProgressView("water", "day", BigDecimal.valueOf(2000), BigDecimal.valueOf(1500), "ml", 75, false),
                new GoalProgressView("exercise", "week", BigDecimal.valueOf(150), BigDecimal.valueOf(160), "min", 106, true));

        var reply = router.respond(message("/hoje", NOW)).getFirst().text();

        assertThat(reply).isEqualTo("""
                Hoje
                Água: 1.500 mL de 2.000 mL (75%)

                Nesta semana
                Treino: 2 h 40 min de 2 h 30 min (106%) ✅""");
    }

    @Test
    void shouldDescribeWhatWasUndone() {
        undoResults.push(new UndoResult.Undone(water(300)));

        assertThat(router.respond(message("/desfazer", NOW))).containsExactly(Reply.of("↩️ Desfeito. Água: 300 mL às 14:32"));
    }

    @Test
    void shouldAnswerUnknownCommandsAndNonTextMessagesWithGuidance() {
        assertThat(router.respond(message("/oi", NOW)).getFirst().text()).startsWith("Não conheço esse comando.");
        assertThat(router.respond(new IncomingMessage(42, 1, Optional.empty(), NOW)))
                .containsExactly(Reply.of("Por enquanto só entendo mensagens de texto."));
    }

    private static IncomingMessage message(String text, Instant sentAt) {
        return new IncomingMessage(42, sentAt.getEpochSecond(), Optional.of(text), sentAt);
    }

    private static EntrySummary water(int milliliters) {
        return new EntrySummary(UUID.randomUUID(), "water", Optional.empty(), Optional.empty(),
                Optional.of(BigDecimal.valueOf(milliliters)), Optional.of("ml"), Optional.empty(), NOW, Optional.empty());
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setDefaultLocale(Locale.forLanguageTag("pt-BR"));
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
