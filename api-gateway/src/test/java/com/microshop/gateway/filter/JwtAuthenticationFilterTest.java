package com.microshop.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microshop.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";
    private static final String ISSUER = "test-issuer";
    private static final String INTERNAL = "internal-token";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    private JwtAuthenticationFilter filter;
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = e -> {
        forwarded.set(e);
        return Mono.empty();
    };

    @BeforeEach
    void setUp() {
        GatewaySecurityProperties props = new GatewaySecurityProperties();
        props.setJwtSecret(SECRET);
        props.setJwtIssuer(ISSUER);
        props.setInternalToken(INTERNAL);
        props.setPublicPaths(List.of("POST /api/auth/login", "GET /api/products/**"));
        filter = new JwtAuthenticationFilter(props, new ObjectMapper());
    }

    private String token(String type, Instant expiry, String secret) {
        return Jwts.builder().issuer(ISSUER).subject(USER_ID)
                .claim("userId", USER_ID).claim("email", "a@b.com")
                .claim("roles", List.of("USER", "ADMIN")).claim("type", type)
                .expiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String validToken() {
        return token("access", Instant.now().plusSeconds(300), SECRET);
    }

    @Test
    void validTokenForwardsIdentityHeadersAndHidesAuthorization() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header("Authorization", "Bearer " + validToken()).build());

        filter.filter(ex, chain).block();

        var headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo(USER_ID);
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("a@b.com");
        assertThat(headers.getFirst("X-User-Roles")).isEqualTo("USER,ADMIN");
        assertThat(headers.getFirst("X-Internal-Token")).isEqualTo(INTERNAL);
        assertThat(headers.getFirst("Authorization")).isNull();
    }

    @Test
    void missingTokenOnProtectedRouteIs401() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());

        filter.filter(ex, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getResponse().getBodyAsString().block()).contains("Authentication required");
    }

    @Test
    void expiredTokenIs401() {
        String expired = token("access", Instant.now().minusSeconds(60), SECRET);
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header("Authorization", "Bearer " + expired).build());

        filter.filter(ex, chain).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getResponse().getBodyAsString().block()).contains("Invalid or expired token");
    }

    @Test
    void tokenSignedWithWrongKeyIs401() {
        String forged = token("access", Instant.now().plusSeconds(300), "another-secret-another-secret-another-1234");
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header("Authorization", "Bearer " + forged).build());

        filter.filter(ex, chain).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshTypeTokenIsNotAcceptedAsAccessToken() {
        String wrongType = token("refresh", Instant.now().plusSeconds(300), SECRET);
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header("Authorization", "Bearer " + wrongType).build());

        filter.filter(ex, chain).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicRouteWorksAnonymouslyAndStripsSpoofedHeaders() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
                .header("X-User-Id", "evil").header("X-User-Roles", "ADMIN").build());

        filter.filter(ex, chain).block();

        var headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isNull();
        assertThat(headers.getFirst("X-User-Roles")).isNull();
        assertThat(headers.getFirst("X-Internal-Token")).isEqualTo(INTERNAL);
    }

    @Test
    void publicRouteIgnoresInvalidTokenInsteadOfFailing() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
                .header("Authorization", "Bearer garbage").build());

        filter.filter(ex, chain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    void publicRuleIsMethodSpecific() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.post("/api/products").build());

        filter.filter(ex, chain).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
