package io.github.marcosaalbanojunior.prontuario.telegram.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("telegram")
public record TelegramProperties(
        @NotBlank(message = "is required: set TELEGRAM_BOT_TOKEN") String botToken,
        @NotNull(message = "is required: set TELEGRAM_ALLOWED_CHAT_ID") Long allowedChatId) {

    // Keeps the bot token out of logs and error messages.
    @Override
    public String toString() {
        return "TelegramProperties[botToken=***, allowedChatId=%d]".formatted(allowedChatId);
    }
}
