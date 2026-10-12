package io.github.marcosaalbanojunior.prontuario.application.usecase.telegram;

import java.util.Locale;

/** Message keys are kebab-case: RIGHT_KNEE becomes "right-knee". */
final class MessageKeys {

    private MessageKeys() {}

    static String of(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
