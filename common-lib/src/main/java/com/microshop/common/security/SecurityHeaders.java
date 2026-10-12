package com.microshop.common.security;

/** Headers the API gateway sets after validating a JWT. Downstream services trust them. */
public final class SecurityHeaders {
    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String INTERNAL_TOKEN = "X-Internal-Token";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private SecurityHeaders() {
    }
}
