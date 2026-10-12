package io.github.marcosaalbanojunior.prontuario.adapter.out.persistence;

import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionAdapter implements TransactionPort {

    private final TransactionTemplate template;

    public TransactionAdapter(TransactionTemplate template) {
        this.template = template;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
