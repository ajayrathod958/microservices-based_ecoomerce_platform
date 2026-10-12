package com.microshop.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microshop.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validates the Bearer JWT and forwards the caller's identity to downstream services as headers.
 *
 * - Identity headers sent by the client are ALWAYS stripped (anti-spoofing).
 * - Protected routes without a valid access token get a JSON 401.
 * - Public routes work anonymously; a valid token is still honoured so handlers can personalise.
 * - The Authorization header is not forwarded; services trust the identity headers + internal token.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_USER_EMAIL = "X-User-Email";
    public static final String H_USER_ROLES = "X-User-Roles";
    public static final String H_INTERNAL_TOKEN = "X-Internal-Token";

    private record PublicRule(String method, PathPattern pattern) {
        boolean matches(String requestMethod, PathContainer path) {
            return ("*".equals(method) || method.equalsIgnoreCase(requestMethod)) && pattern.matches(path);
        }
    }

    private final SecretKey key;
    private final String issuer;
    private final String internalToken;
    private final List<PublicRule> publicRules;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(GatewaySecurityProperties props, ObjectMapper objectMapper) {
        byte[] secret = props.getJwtSecret() == null ? new byte[0] : props.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("gateway.security.jwt-secret must be at least 32 bytes (256 bits)");
        }
        if (props.getInternalToken() == null || props.getInternalToken().isBlank()) {
            throw new IllegalStateException("gateway.security.internal-token must be set");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.issuer = props.getJwtIssuer();
        this.internalToken = props.getInternalToken();
        this.objectMapper = objectMapper;

        PathPatternParser parser = new PathPatternParser();
        this.publicRules = props.getPublicPaths().stream().map(entry -> {
            String[] parts = entry.trim().split("\\s+", 2);
            return parts.length == 2
                    ? new PublicRule(parts[0].toUpperCase(), parser.parse(parts[1]))
                    : new PublicRule("*", parser.parse(parts[0]));
        }).toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        boolean isPublic = isPublic(request);
        Optional<Claims> claims = extractClaims(request);

        if (!isPublic && claims.isEmpty()) {
            boolean hadToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) != null;
            return unauthorized(exchange, hadToken ? "Invalid or expired token" : "Authentication required");
        }

        ServerHttpRequest mutated = request.mutate().headers(h -> {
            h.remove(H_USER_ID);
            h.remove(H_USER_EMAIL);
            h.remove(H_USER_ROLES);
            h.remove(HttpHeaders.AUTHORIZATION);
            h.set(H_INTERNAL_TOKEN, internalToken);
            claims.ifPresent(c -> {
                h.set(H_USER_ID, c.get("userId", String.class));
                h.set(H_USER_EMAIL, String.valueOf(c.get("email")));
                h.set(H_USER_ROLES, rolesOf(c));
            });
        }).build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublic(ServerHttpRequest request) {
        PathContainer path = request.getPath().pathWithinApplication();
        String method = request.getMethod().name();
        return publicRules.stream().anyMatch(r -> r.matches(method, path));
    }

    private Optional<Claims> extractClaims(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(header.substring(7).trim())
                    .getPayload();
            boolean valid = "access".equals(claims.get("type", String.class))
                    && claims.get("userId", String.class) != null;
            return valid ? Optional.of(claims) : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String rolesOf(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return "";
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("status", 401);
        body.put("message", message);
        body.put("path", exchange.getRequest().getURI().getPath());
        body.put("timestamp", Instant.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"success\":false,\"status\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;   // right after CorrelationIdFilter
    }
}
