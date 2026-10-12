package io.github.marcosaalbanojunior.prontuario.support;

import io.github.marcosaalbanojunior.prontuario.application.port.out.SourceRepositoryPort;
import io.github.marcosaalbanojunior.prontuario.domain.model.entry.Source;
import java.util.ArrayList;
import java.util.List;

public final class InMemorySourceRepository implements SourceRepositoryPort {

    private final List<Source> sources = new ArrayList<>();

    @Override
    public void add(Source source) {
        sources.add(source);
    }

    @Override
    public boolean existsByTelegramMessageId(long telegramMessageId) {
        return sources.stream().anyMatch(source -> source.telegramMessageId().filter(id -> id == telegramMessageId).isPresent());
    }

    public List<Source> all() {
        return List.copyOf(sources);
    }
}
