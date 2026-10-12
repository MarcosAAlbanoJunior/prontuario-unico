package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryRequest;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.QuickEntryResult;
import io.github.marcosaalbanojunior.prontuario.application.port.in.entry.UndoResult;
import io.github.marcosaalbanojunior.prontuario.domain.model.goal.GoalProgress;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.MissingInfo;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.QuickCommand;
import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.RejectionReason;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import io.github.marcosaalbanojunior.prontuario.support.PtBrMessages;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HandleChatMessageService.handle(...)")
class HandleChatMessageServiceTest {

    static final Instant NOW = Instant.parse("2026-10-12T17:32:00Z");

    private final List<QuickEntryRequest> requests = new ArrayList<>();
    private final Deque<QuickEntryResult> results = new ArrayDeque<>();
    private final Deque<UndoResult> undoResults = new ArrayDeque<>();
    private List<GoalProgress> progress = List.of();

    private final HandleChatMessageService service = new HandleChatMessageService(
            request -> {
                requests.add(request);
                return results.pop();
            },
            undoResults::pop, () -> progress, PtBrMessages.load(),
            Clock.fixed(NOW, ZoneId.of("UTC")), ZoneId.of("America/Sao_Paulo"));

    @Nested
    @DisplayName("quick commands")
    class QuickCommands {

        @Test
        @DisplayName("confirms a recorded entry in Portuguese")
        void should_confirmInPortuguese_when_entryIsRecorded() {
            results.push(new QuickEntryResult.Recorded(Events.water(500)));

            var replies = service.handle(message("/agua 500", NOW));

            assertThat(replies).containsExactly(Reply.of("✅ Registrado. Água: 500 mL às 14:32"));
            assertThat(requests.getFirst().command()).isEqualTo(QuickCommand.WATER);
            assertThat(requests.getFirst().source().rawText()).isEqualTo("/agua 500");
        }

        @Test
        @DisplayName("explains a rejection with an example of the command")
        void should_explainWithExample_when_entryIsRejected() {
            results.push(new QuickEntryResult.Rejected(RejectionReason.OUT_OF_RANGE));

            assertThat(service.handle(message("/peso 900", NOW)))
                    .containsExactly(Reply.of("Esse valor parece fora do esperado. Ex.: /peso 82,4"));
        }

        @Test
        @DisplayName("stays silent when the message was already recorded")
        void should_replyNothing_when_alreadyRecorded() {
            results.push(new QuickEntryResult.AlreadyRecorded());

            assertThat(service.handle(message("/agua 500", NOW))).isEmpty();
        }
    }

    @Nested
    @DisplayName("follow-up questions")
    class FollowUpQuestions {

        @Test
        @DisplayName("asks for the missing data and completes the entry with the answer")
        void should_completeEntry_when_userAnswersTheQuestion() {
            results.add(new QuickEntryResult.NeedsMore(MissingInfo.BODY_REGION));
            results.add(new QuickEntryResult.Recorded(Events.water(1)));

            var question = service.handle(message("/dor 4", NOW));
            service.handle(message("joelho direito", NOW.plusSeconds(30)));

            assertThat(question).containsExactly(Reply.question(
                    "Onde é a dor? Ex.: joelho direito 4 (a intensidade de 0 a 10 é opcional)"));
            assertThat(requests).extracting(QuickEntryRequest::args).containsExactly("4", "4 joelho direito");
            assertThat(requests.getLast().source().rawText()).isEqualTo("/dor 4 joelho direito");
        }

        @Test
        @DisplayName("forgets the question after ten minutes")
        void should_treatAsFreeText_when_questionExpired() {
            results.add(new QuickEntryResult.NeedsMore(MissingInfo.WATER_AMOUNT));
            service.handle(message("/agua", NOW));

            var late = service.handle(message("500", NOW.plusSeconds(600)));

            assertThat(late.getFirst().text()).startsWith("Ainda não entendo texto livre");
            assertThat(requests).hasSize(1);
        }

        @Test
        @DisplayName("drops the question when another command arrives")
        void should_dropQuestion_when_anotherCommandArrives() {
            results.add(new QuickEntryResult.NeedsMore(MissingInfo.WATER_AMOUNT));
            service.handle(message("/agua", NOW));
            service.handle(message("/ajuda", NOW));

            assertThat(service.handle(message("500", NOW)).getFirst().text()).startsWith("Ainda não entendo texto livre");
        }
    }

    @Nested
    @DisplayName("other commands")
    class OtherCommands {

        @Test
        @DisplayName("/hoje shows the goals")
        void should_showGoals_when_hoje() {
            progress = List.of(Events.waterGoalProgress(1500));

            assertThat(service.handle(message("/hoje", NOW)).getFirst().text()).contains("Água: 1.500 mL de 2.000 mL (75%)");
        }

        @Test
        @DisplayName("/desfazer describes what was undone")
        void should_describeUndoneEntry_when_desfazer() {
            undoResults.push(new UndoResult.Undone(Events.water(300)));

            assertThat(service.handle(message("/desfazer", NOW))).containsExactly(Reply.of("↩️ Desfeito. Água: 300 mL às 14:32"));
        }

        @Test
        @DisplayName("/desfazer says when there is nothing to undo")
        void should_sayNothingToUndo_when_noEntries() {
            undoResults.push(new UndoResult.NothingToUndo());

            assertThat(service.handle(message("/desfazer", NOW))).containsExactly(Reply.of("Não há registro para desfazer."));
        }

        @ParameterizedTest(name = "/{0} lists the commands")
        @ValueSource(strings = {"start", "ajuda", "help"})
        @DisplayName("help commands list every command")
        void should_listCommands_when_askingForHelp(String command) {
            assertThat(service.handle(message("/" + command, NOW)).getFirst().text()).startsWith("Comandos:");
        }

        @Test
        @DisplayName("an unknown command gets guidance and the command list")
        void should_guide_when_commandIsUnknown() {
            var text = service.handle(message("/oi", NOW)).getFirst().text();

            assertThat(text).startsWith("Não conheço esse comando.").contains("Comandos:");
        }
    }

    @Test
    @DisplayName("non-text messages get guidance")
    void should_guide_when_messageHasNoText() {
        assertThat(service.handle(new IncomingMessage(42, 1, Optional.empty(), NOW)))
                .containsExactly(Reply.of("Por enquanto só entendo mensagens de texto."));
    }

    private static IncomingMessage message(String text, Instant sentAt) {
        return new IncomingMessage(42, sentAt.getEpochSecond(), Optional.of(text), sentAt);
    }
}
