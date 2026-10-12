package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.TelegramUpdate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** The subset of the Telegram Bot API (https://core.telegram.org/bots/api) this app uses. */
final class TelegramApi {

    static final List<String> MESSAGE_UPDATES_ONLY = List.of("message");

    private TelegramApi() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response<T>(boolean ok, T result, String description, Parameters parameters) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parameters(@JsonProperty("retry_after") Integer retryAfter) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Update(@JsonProperty("update_id") long updateId, Message message) {

        TelegramUpdate toDomain() {
            return new TelegramUpdate(updateId, Optional.ofNullable(message).map(Message::toDomain));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(@JsonProperty("message_id") long messageId, long date, Chat chat, String text) {

        IncomingMessage toDomain() {
            return new IncomingMessage(chat.id(), messageId, Optional.ofNullable(text), Instant.ofEpochSecond(date));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Chat(long id) {}

    record GetUpdatesRequest(long offset, int timeout, @JsonProperty("allowed_updates") List<String> allowedUpdates) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SendMessageRequest(@JsonProperty("chat_id") long chatId, String text,
                              @JsonProperty("reply_markup") ForceReply replyMarkup) {

        static SendMessageRequest of(long chatId, Reply reply) {
            return new SendMessageRequest(chatId, reply.text(), reply.expectsAnswer() ? new ForceReply(true) : null);
        }
    }

    record ForceReply(@JsonProperty("force_reply") boolean forceReply) {}
}
