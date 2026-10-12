package io.github.marcosaalbanojunior.prontuario.telegram.application.port.out;

import io.github.marcosaalbanojunior.prontuario.telegram.domain.IncomingMessage;
import io.github.marcosaalbanojunior.prontuario.telegram.domain.Reply;
import java.util.List;

/** Turns an allowed chat message into actions on the other modules and the replies to send. */
public interface ChatResponder {

    List<Reply> respond(IncomingMessage message);

    /** Reply used when handling a message failed unexpectedly. */
    Reply failureReply();
}
