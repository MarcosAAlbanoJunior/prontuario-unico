package io.github.marcosaalbanojunior.prontuario.application.port.out;

import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Source;

public interface SourceRepositoryPort {

    void add(Source source);

    boolean existsByTelegramMessageId(long telegramMessageId);
}
