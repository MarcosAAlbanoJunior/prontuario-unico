package io.github.marcosaalbanojunior.prontuario.support;

import io.github.marcosaalbanojunior.prontuario.adapter.out.i18n.MessageSourceAdapter;
import io.github.marcosaalbanojunior.prontuario.application.port.out.MessagesPort;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.support.ResourceBundleMessageSource;

/** The real messages_pt_BR.properties, without a Spring context. */
public final class PtBrMessages {

    private PtBrMessages() {}

    public static MessagesPort load() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setDefaultLocale(Locale.forLanguageTag("pt-BR"));
        source.setFallbackToSystemLocale(false);
        return new MessageSourceAdapter(source);
    }
}
