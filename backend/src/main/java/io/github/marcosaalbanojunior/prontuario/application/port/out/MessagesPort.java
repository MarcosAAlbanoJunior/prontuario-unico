package io.github.marcosaalbanojunior.prontuario.application.port.out;

/** User-facing texts in pt-BR, looked up by key (messages_pt_BR.properties). */
public interface MessagesPort {

    String get(String key, Object... args);
}
