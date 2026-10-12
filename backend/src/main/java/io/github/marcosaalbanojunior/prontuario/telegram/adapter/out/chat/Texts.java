package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.chat;

import java.util.Locale;
import org.springframework.context.MessageSource;

/** User-facing texts from messages_pt_BR.properties. */
public final class Texts {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final MessageSource messages;

    public Texts(MessageSource messages) {
        this.messages = messages;
    }

    String get(String key, Object... args) {
        return messages.getMessage(key, args.length == 0 ? null : args, PT_BR);
    }
}
