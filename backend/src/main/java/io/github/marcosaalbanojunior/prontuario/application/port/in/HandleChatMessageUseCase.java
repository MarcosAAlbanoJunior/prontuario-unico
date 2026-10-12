package io.github.marcosaalbanojunior.prontuario.application.port.in;

import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.domain.model.telegram.Reply;
import java.util.List;

/** Turns a message from the allowed chat into recorded data and the replies to send. */
public interface HandleChatMessageUseCase {

    List<Reply> handle(IncomingMessage message);
}
