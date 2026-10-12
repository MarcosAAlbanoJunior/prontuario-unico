package io.github.marcosaalbanojunior.prontuario.telegram;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** A local stand-in for api.telegram.org: queued updates are served once; sent messages are recorded. */
final class FakeTelegramServer implements AutoCloseable {

    static final String TOKEN = "123456:e2e-token";

    private final HttpServer server;
    private final ConcurrentLinkedQueue<String> pendingUpdates = new ConcurrentLinkedQueue<>();
    private final List<String> sentMessages = new ArrayList<>();

    FakeTelegramServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bot" + TOKEN + "/getUpdates", this::getUpdates);
        server.createContext("/bot" + TOKEN + "/sendMessage", this::sendMessage);
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void queueMessage(long updateId, long chatId, long messageId, long date, String text) {
        pendingUpdates.add("""
                {"update_id":%d,"message":{"message_id":%d,"date":%d,"chat":{"id":%d},"text":"%s"}}"""
                .formatted(updateId, messageId, date, chatId, text));
    }

    synchronized List<String> sentMessages() {
        return List.copyOf(sentMessages);
    }

    synchronized void clear() {
        pendingUpdates.clear();
        sentMessages.clear();
    }

    private void getUpdates(HttpExchange exchange) throws IOException {
        var updates = new ArrayList<String>();
        for (String update; (update = pendingUpdates.poll()) != null; ) {
            updates.add(update);
        }
        respond(exchange, "{\"ok\":true,\"result\":[" + String.join(",", updates) + "]}");
    }

    private void sendMessage(HttpExchange exchange) throws IOException {
        synchronized (this) {
            sentMessages.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        }
        respond(exchange, "{\"ok\":true,\"result\":{}}");
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        var body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
