package io.github.marcosaalbanojunior.prontuario.domain.model.entry;

public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
