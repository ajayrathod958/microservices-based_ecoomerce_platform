package com.microshop.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    /** HS256 secret shared with user-service (>= 32 bytes). */
    private String jwtSecret;
    private String jwtIssuer = "microshop-user-service";
    /** Sent to downstream services so they can trust the identity headers. */
    private String internalToken;
    /** Entries are "METHOD /path/pattern" or just "/path/pattern" (any method). */
    private List<String> publicPaths = new ArrayList<>();
}
