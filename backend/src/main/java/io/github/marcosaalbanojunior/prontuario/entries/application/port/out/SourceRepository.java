package io.github.marcosaalbanojunior.prontuario.entries.application.port.out;

import io.github.marcosaalbanojunior.prontuario.entries.domain.Source;

public interface SourceRepository {

    void add(Source source);

    boolean existsByTelegramMessageId(long telegramMessageId);
}
