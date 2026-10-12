package io.github.marcosaalbanojunior.prontuario.domain.model.telegram;

import java.util.Locale;
import java.util.regex.Pattern;

/** A message is either a bot command ("/agua 500", "/agua@MeuBot 500") or plain text. */
public sealed interface ChatInput {

    record Command(String name, String args) implements ChatInput {}

    record PlainText(String text) implements ChatInput {}

    Pattern COMMAND = Pattern.compile("^/([a-zA-Z_]+)(?:@\\w+)?(?:\\s+(.*))?$", Pattern.DOTALL);

    static ChatInput parse(String text) {
        var trimmed = text.strip();
        var matcher = COMMAND.matcher(trimmed);
        if (!matcher.matches()) {
            return new PlainText(trimmed);
        }
        var args = matcher.group(2) == null ? "" : matcher.group(2).strip();
        return new Command(matcher.group(1).toLowerCase(Locale.ROOT), args);
    }
}
