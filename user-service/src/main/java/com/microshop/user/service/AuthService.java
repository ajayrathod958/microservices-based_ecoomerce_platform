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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final JwtProperties props;
    /** Used to burn the same CPU time when the email is unknown (blunts user enumeration by timing). */
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       JwtService jwtService, JwtProperties props) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.props = props;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User saved = users.save(User.builder()
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .phone(req.phone())
                .roles(new HashSet<>(Set.of(Role.USER)))   // never client-controlled
                .build());
        log.info("Registered user {}", saved.getId());
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email())).orElse(null);
        if (user == null) {
            encoder.matches(req.password(), dummyHash);
            throw invalidCredentials();
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }
        log.info("User {} logged in", user.getId());
        return issueTokens(user);
    }

    /**
     * Refresh-token rotation: each token is single-use. Presenting an already-used token signals theft,
     * so every session of that user is revoked. noRollbackFor keeps that revocation even though we throw.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked()) {
            refreshTokens.revokeAllForUser(token.getUser().getId());
            log.warn("Refresh token reuse detected for user {}; all sessions revoked", token.getUser().getId());
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }
        User user = token.getUser();
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }
        token.setRevoked(true);
        refreshTokens.save(token);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(TokenHasher.sha256(rawToken)).ifPresent(t -> t.setRevoked(true));
    }

    private AuthResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokens.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(rawRefresh))
                .expiresAt(Instant.now().plus(props.refreshTokenTtl()))
                .build());

        return new AuthResponse(jwtService.generateAccessToken(user), rawRefresh, "Bearer",
                jwtService.accessTokenTtlSeconds(), UserMapper.toResponse(user));
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid email or password");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
