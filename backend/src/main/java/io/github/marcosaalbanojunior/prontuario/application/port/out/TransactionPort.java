package io.github.marcosaalbanojunior.prontuario.application.port.out;

import java.util.function.Supplier;

/**
 * Runs work in a single database transaction. Application services depend on this
 * interface so they stay free of Spring; the implementation lives in the wiring.
 */
public interface TransactionPort {

    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
