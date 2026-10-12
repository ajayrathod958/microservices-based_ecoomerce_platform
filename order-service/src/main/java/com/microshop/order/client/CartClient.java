package com.microshop.order.client;

import com.microshop.common.exception.ServiceUnavailableException;
import com.microshop.common.exception.UnauthorizedException;
import com.microshop.common.security.AuthenticatedUser;
import com.microshop.common.security.SecurityHeaders;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.stream.Collectors;

/**
 * Calls cart-service ON BEHALF OF the current user: the identity headers the gateway gave us are re-issued
 * together with the shared internal token, so cart-service sees exactly the same caller.
 */
@Slf4j
@Component
public class CartClient {

    private final RestClient client;
    private final String internalToken;

    public CartClient(@LoadBalanced RestClient.Builder builder,
                      @Value("${app.cart-service.url}") String baseUrl,
                      @Value("${security.internal-token}") String internalToken) {
        this.client = builder.baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }

    public CartSnapshot fetchCart() {
        try {
            CartEnvelope envelope = client.get().uri("/api/cart")
                    .headers(this::identity).retrieve().body(CartEnvelope.class);
            if (envelope == null || envelope.data() == null || envelope.data().items() == null) {
                throw new ServiceUnavailableException("Cart service returned an empty response");
            }
            return envelope.data();
        } catch (RestClientException | IllegalStateException e) {
            log.warn("Cart lookup failed: {}", e.getMessage());
            throw new ServiceUnavailableException("Cart service is unavailable, please try again shortly");
        }
    }

    public void clear() {
        try {
            client.delete().uri("/api/cart").headers(this::identity).retrieve().toBodilessEntity();
        } catch (RestClientException | IllegalStateException e) {
            log.warn("Could not clear cart after checkout: {}", e.getMessage());
            throw new ServiceUnavailableException("Cart service is unavailable");
        }
    }

    private void identity(HttpHeaders headers) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Authentication required");
        }
        headers.set(SecurityHeaders.USER_ID, user.userId().toString());
        headers.set(SecurityHeaders.USER_EMAIL, String.valueOf(user.email()));
        headers.set(SecurityHeaders.USER_ROLES, auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.joining(",")));
        headers.set(SecurityHeaders.INTERNAL_TOKEN, internalToken);
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            headers.set(SecurityHeaders.CORRELATION_ID, correlationId);
        }
    }
}
