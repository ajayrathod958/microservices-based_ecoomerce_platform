package com.microshop.user.service;

import com.microshop.common.exception.ConflictException;
import com.microshop.common.exception.UnauthorizedException;
import com.microshop.user.config.JwtProperties;
import com.microshop.user.dto.AuthResponse;
import com.microshop.user.dto.LoginRequest;
import com.microshop.user.dto.RegisterRequest;
import com.microshop.user.dto.UserResponse;
import com.microshop.user.entity.RefreshToken;
import com.microshop.user.entity.Role;
import com.microshop.user.entity.User;
import com.microshop.user.repository.RefreshTokenRepository;
import com.microshop.user.repository.UserRepository;
import com.microshop.user.util.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock JwtService jwtService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtProperties props =
            new JwtProperties("x".repeat(32), Duration.ofMinutes(15), Duration.ofDays(7), "iss");

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, refreshTokens, encoder, jwtService, props);
    }

    private User user(String email, String hash) {
        return User.builder().id(UUID.randomUUID()).email(email).passwordHash(hash)
                .firstName("John").lastName("Doe").roles(new HashSet<>(Set.of(Role.USER))).build();
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndGrantsOnlyUserRole() {
        when(users.existsByEmail("john@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse res = service.register(
                new RegisterRequest("  John@Example.com ", "Passw0rdX", "John", "Doe", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getPasswordHash()).isNotEqualTo("Passw0rdX");
        assertThat(encoder.matches("Passw0rdX", saved.getPasswordHash())).isTrue();
        assertThat(saved.getRoles()).containsExactly(Role.USER);
        assertThat(res.email()).isEqualTo("john@example.com");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("john@example.com", "Passw0rdX", "John", "Doe", null)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void loginIssuesAccessAndHashedRefreshToken() {
        User user = user("john@example.com", encoder.encode("Passw0rdX"));
        when(users.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse res = service.login(new LoginRequest("John@example.com", "Passw0rdX"));

        assertThat(res.accessToken()).isEqualTo("access-token");
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.expiresIn()).isEqualTo(900);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(TokenHasher.sha256(res.refreshToken()));
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(res.refreshToken());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        User user = user("john@example.com", encoder.encode("Passw0rdX"));
        when(users.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("john@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void loginWithUnknownEmailGivesSameError() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@example.com", "whatever1")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void refreshRotatesToken() {
        User user = user("john@example.com", "hash");
        RefreshToken old = RefreshToken.builder().user(user).tokenHash(TokenHasher.sha256("raw"))
                .expiresAt(Instant.now().plusSeconds(60)).build();
        when(refreshTokens.findByTokenHash(TokenHasher.sha256("raw"))).thenReturn(Optional.of(old));
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse res = service.refresh("raw");

        assertThat(old.isRevoked()).isTrue();
        assertThat(res.accessToken()).isEqualTo("new-access");
        assertThat(res.refreshToken()).isNotEqualTo("raw");
    }

    @Test
    void reuseOfRevokedRefreshTokenRevokesAllSessions() {
        User user = user("john@example.com", "hash");
        RefreshToken used = RefreshToken.builder().user(user).tokenHash(TokenHasher.sha256("raw"))
                .expiresAt(Instant.now().plusSeconds(60)).revoked(true).build();
        when(refreshTokens.findByTokenHash(TokenHasher.sha256("raw"))).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(UnauthorizedException.class);

        verify(refreshTokens).revokeAllForUser(user.getId());
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        User user = user("john@example.com", "hash");
        RefreshToken expired = RefreshToken.builder().user(user).tokenHash(TokenHasher.sha256("raw"))
                .expiresAt(Instant.now().minusSeconds(5)).build();
        when(refreshTokens.findByTokenHash(TokenHasher.sha256("raw"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.refresh("raw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token expired");
    }
}
