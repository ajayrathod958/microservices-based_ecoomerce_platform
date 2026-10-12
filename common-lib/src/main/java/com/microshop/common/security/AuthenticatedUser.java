package com.microshop.common.security;

import java.util.UUID;

/** Principal placed in the SecurityContext; inject with @AuthenticationPrincipal. */
public record AuthenticatedUser(UUID userId, String email) {
}
