package io.github.marcosaalbanojunior.prontuario.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every HTTP request a trace id: reuses a well-formed X-Request-Id or creates one, puts it in
 * the MDC so every log line carries it, and echoes it in the response so a reported error can be found.
 */
public final class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "traceId";

    // Anything else is replaced, so a client cannot inject arbitrary text into the logs.
    private static final Pattern WELL_FORMED = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var traceId = traceIdOf(request);
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String traceIdOf(HttpServletRequest request) {
        var received = request.getHeader(HEADER);
        return received != null && WELL_FORMED.matcher(received).matches() ? received : UUID.randomUUID().toString();
    }
}
