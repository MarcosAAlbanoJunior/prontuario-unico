package io.github.marcosaalbanojunior.prontuario.common.tx;

import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate template;

    SpringTransactionRunner(TransactionTemplate template) {
        this.template = template;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
