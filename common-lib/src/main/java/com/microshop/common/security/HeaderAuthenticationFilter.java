package com.microshop.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Builds the Spring Security Authentication from the identity headers set by the gateway.
 * The headers are honoured ONLY when X-Internal-Token matches the shared secret, so a caller
 * that reaches a service directly cannot impersonate a user by forging headers.
 *
 * Not a @Component on purpose: each service adds it to its own SecurityFilterChain.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Pattern ROLE_NAME = Pattern.compile("^[A-Z][A-Z_]{0,30}$");

    private final byte[] expectedToken;

    public HeaderAuthenticationFilter(String internalToken) {
        this.expectedToken = internalToken == null ? new byte[0] : internalToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userId = request.getHeader(SecurityHeaders.USER_ID);
        if (userId != null && trusted(request.getHeader(SecurityHeaders.INTERNAL_TOKEN))) {
            try {
                AuthenticatedUser principal = new AuthenticatedUser(
                        UUID.fromString(userId), request.getHeader(SecurityHeaders.USER_EMAIL));
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, parseRoles(request.getHeader(SecurityHeaders.USER_ROLES)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException ignored) {
                // malformed user id -> stay anonymous
            }
        }
        chain.doFilter(request, response);
    }

    private boolean trusted(String presented) {
        if (presented == null || expectedToken.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(expectedToken, presented.getBytes(StandardCharsets.UTF_8));
    }

    private List<GrantedAuthority> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(r -> ROLE_NAME.matcher(r).matches())
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }
}
