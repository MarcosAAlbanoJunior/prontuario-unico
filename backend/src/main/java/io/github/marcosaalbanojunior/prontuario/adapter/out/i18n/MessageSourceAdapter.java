package io.github.marcosaalbanojunior.prontuario.adapter.out.i18n;

import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;

@RequiredArgsConstructor
public final class MessageSourceAdapter implements MessagesPort {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final MessageSource messages;

    @Override
    public String get(String key, Object... args) {
        return messages.getMessage(key, args.length == 0 ? null : args, PT_BR);
    }
}
