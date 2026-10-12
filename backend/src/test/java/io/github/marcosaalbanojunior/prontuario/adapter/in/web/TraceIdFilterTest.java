package io.github.marcosaalbanojunior.prontuario.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("TraceIdFilter")
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicReference<String> traceIdDuringRequest = new AtomicReference<>();

    @Test
    @DisplayName("reuses a well-formed X-Request-Id, logs with it and echoes it back")
    void should_reuseHeader_when_wellFormed() throws Exception {
        var request = request("abc12345-req");

        filter.doFilter(request, response, (req, res) -> traceIdDuringRequest.set(MDC.get("traceId")));

        assertThat(traceIdDuringRequest).hasValue("abc12345-req");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc12345-req");
        assertThat(MDC.get("traceId")).isNull();
    }

    @ParameterizedTest(name = "header \"{0}\" is replaced")
    @NullSource
    @ValueSource(strings = {"short", "has spaces in it", "line\nbreak-injected", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
    @DisplayName("creates a new trace id when the header is missing or malformed")
    void should_createTraceId_when_headerIsMissingOrMalformed(String header) throws Exception {
        filter.doFilter(request(header), response, (req, res) -> traceIdDuringRequest.set(MDC.get("traceId")));

        assertThat(traceIdDuringRequest.get()).matches("[0-9a-f-]{36}").isEqualTo(response.getHeader("X-Request-Id"));
    }

    @Test
    @DisplayName("clears the trace id even when the request fails")
    void should_clearMdc_when_requestFails() {
        assertThatThrownBy(() -> filter.doFilter(request(null), response, (req, res) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.get("traceId")).isNull();
    }

    private static MockHttpServletRequest request(String requestId) {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        if (requestId != null) {
            request.addHeader("X-Request-Id", requestId);
        }
        return request;
    }
}
