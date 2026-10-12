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

    
    private String jwtSecret;
    private String jwtIssuer = "microshop-user-service";
   
    private String internalToken;
   
    private List<String> publicPaths = new ArrayList<>();
}
