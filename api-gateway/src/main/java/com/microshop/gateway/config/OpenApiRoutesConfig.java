package com.microshop.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Exposes every service's /v3/api-docs under /docs/{service}/v3/api-docs so the gateway's
 * Swagger UI (configured in application.yml) can aggregate them behind a single origin.
 */
@Configuration
public class OpenApiRoutesConfig {

    static final List<String> SERVICES = List.of(
            "user-service", "product-service", "cart-service", "order-service",
            "payment-service", "inventory-service", "notification-service");

    @Bean
    public RouteLocator openApiRoutes(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        for (String service : SERVICES) {
            routes.route(service + "-openapi", r -> r
                    .path("/docs/" + service + "/v3/api-docs/**")
                    .filters(f -> f.rewritePath("/docs/" + service + "/(?<segment>.*)", "/${segment}"))
                    .uri("lb://" + service));
        }
        return routes.build();
    }
}
