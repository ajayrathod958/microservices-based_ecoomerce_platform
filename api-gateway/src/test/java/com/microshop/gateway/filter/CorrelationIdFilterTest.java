package com.microshop.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static com.microshop.gateway.filter.CorrelationIdFilter.HEADER;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private String run(MockServerWebExchange exchange, AtomicReference<ServerWebExchange> seen) {
        GatewayFilterChain chain = e -> {
            seen.set(e);
            return Mono.empty();
        };
        filter.filter(exchange, chain).block();
        return seen.get().getRequest().getHeaders().getFirst(HEADER);
    }

    @Test
    void generatesIdWhenAbsent() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products").build());
        String id = run(ex, new AtomicReference<>());

        assertThat(id).isNotBlank();
        assertThat(ex.getResponse().getHeaders().getFirst(HEADER)).isEqualTo(id);
    }

    @Test
    void keepsValidIncomingId() {
        MockServerWebExchange ex = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").header(HEADER, "abc-123").build());

        assertThat(run(ex, new AtomicReference<>())).isEqualTo("abc-123");
    }

    @Test
    void replacesUnsafeIncomingId() {
        MockServerWebExchange ex = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").header(HEADER, "bad value!<script>").build());

        assertThat(run(ex, new AtomicReference<>())).isNotEqualTo("bad value!<script>").matches("^[A-Za-z0-9-]{1,64}$");
    }
}
