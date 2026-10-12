package io.github.marcosaalbanojunior.prontuario.goals.domain;

public class InvalidGoalException extends RuntimeException {

    public InvalidGoalException(String message) {
        super(message);
    }
}
