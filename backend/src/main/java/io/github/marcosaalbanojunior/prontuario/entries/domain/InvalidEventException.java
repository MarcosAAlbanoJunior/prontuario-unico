package io.github.marcosaalbanojunior.prontuario.entries.domain;

public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }
}
