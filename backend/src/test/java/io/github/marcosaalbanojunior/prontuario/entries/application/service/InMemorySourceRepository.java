package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.out.SourceRepository;
import io.github.marcosaalbanojunior.prontuario.entries.domain.Source;
import java.util.ArrayList;
import java.util.List;

final class InMemorySourceRepository implements SourceRepository {

    private final List<Source> sources = new ArrayList<>();

    @Override
    public void add(Source source) {
        sources.add(source);
    }

    @Override
    public boolean existsByTelegramMessageId(long telegramMessageId) {
        return sources.stream().anyMatch(source -> source.telegramMessageId().filter(id -> id == telegramMessageId).isPresent());
    }

    List<Source> all() {
        return List.copyOf(sources);
    }
}
