package io.github.marcosaalbanojunior.prontuario.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * @param apiUrl  Bot API base URL; only tests point it elsewhere
 * @param polling whether to long-poll Telegram; disabled in tests
 */
@Validated
@ConfigurationProperties("telegram")
public record TelegramProperties(
        @NotBlank(message = "is required: set TELEGRAM_BOT_TOKEN") String botToken,
        @NotNull(message = "is required: set TELEGRAM_ALLOWED_CHAT_ID") Long allowedChatId,
        @DefaultValue("https://api.telegram.org") @NotBlank String apiUrl,
        @DefaultValue("true") boolean polling) {

    // Keeps the bot token out of logs and error messages.
    @Override
    public String toString() {
        return "TelegramProperties[botToken=***, allowedChatId=%d, apiUrl=%s, polling=%s]"
                .formatted(allowedChatId, apiUrl, polling);
    }
}
