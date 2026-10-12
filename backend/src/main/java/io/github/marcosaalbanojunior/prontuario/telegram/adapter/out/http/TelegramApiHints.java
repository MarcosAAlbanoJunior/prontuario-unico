package io.github.marcosaalbanojunior.prontuario.telegram.adapter.out.http;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** Jackson reads and writes these records by reflection, which the native image must know about. */
public final class TelegramApiHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(),
                TelegramApi.Response.class, TelegramApi.Parameters.class, TelegramApi.Update.class,
                TelegramApi.Message.class, TelegramApi.Chat.class, TelegramApi.GetUpdatesRequest.class,
                TelegramApi.SendMessageRequest.class, TelegramApi.ForceReply.class);
    }
}
