package io.github.marcosaalbanojunior.prontuario.domain.model.goal;

public class InvalidGoalException extends RuntimeException {

    public InvalidGoalException(String message) {
        super(message);
    }
}
