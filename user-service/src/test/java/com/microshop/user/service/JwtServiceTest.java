package com.microshop.user.service;

import com.microshop.user.config.JwtProperties;
import com.microshop.user.entity.Role;
import com.microshop.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef-secret";

    private JwtProperties props(String secret) {
        return new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7), "test-issuer");
    }

    @Test
    void accessTokenContainsExpectedClaims() {
        JwtService service = new JwtService(props(SECRET));
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).email("john@example.com")
                .roles(new HashSet<>(Set.of(Role.USER, Role.ADMIN))).build();

        String token = service.generateAccessToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .requireIssuer("test-issuer")
                .build().parseSignedClaims(token).getPayload();

        assertThat(claims.get("userId", String.class)).isEqualTo(id.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("john@example.com");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("roles", List.class)).containsExactly("ADMIN", "USER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isBetween(14 * 60_000L, 16 * 60_000L);
        assertThat(service.accessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService(props("too-short")))
                .isInstanceOf(IllegalStateException.class);
    }
}
