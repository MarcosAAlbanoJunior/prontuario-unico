package io.github.marcosaalbanojunior.prontuario.support;

import io.github.marcosaalbanojunior.prontuario.application.port.out.TransactionPort;
import java.util.function.Supplier;

/** Runs work inline; for unit tests with in-memory fakes. */
public final class DirectTransactions implements TransactionPort {

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }
}
