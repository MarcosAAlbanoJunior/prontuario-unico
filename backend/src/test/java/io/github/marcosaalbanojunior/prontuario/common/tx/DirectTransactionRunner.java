package io.github.marcosaalbanojunior.prontuario.common.tx;

import java.util.function.Supplier;

/** Runs work inline; for unit tests with in-memory fakes. */
public final class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }
}
