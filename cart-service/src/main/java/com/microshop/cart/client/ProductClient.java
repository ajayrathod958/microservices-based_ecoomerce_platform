package com.microshop.cart.client;

import com.microshop.common.exception.ResourceNotFoundException;
import com.microshop.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/** Calls product-service's public GET /api/products/{id}; inactive/deleted products come back as 404. */
@Slf4j
@Component
public class ProductClient {

    private final RestClient client;

    public ProductClient(@LoadBalanced RestClient.Builder builder,
                         @Value("${app.product-service.url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public ProductInfo fetch(UUID productId) {
        try {
            ProductEnvelope envelope = client.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductEnvelope.class);
            if (envelope == null || envelope.data() == null) {
                throw new ServiceUnavailableException("Product service returned an empty response");
            }
            return envelope.data();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Product not found");
        } catch (RestClientException | IllegalStateException e) {
            // IllegalStateException: load balancer found no registered product-service instance
            log.warn("Product lookup failed for {}: {}", productId, e.getMessage());
            throw new ServiceUnavailableException("Product service is unavailable, please try again shortly");
        }
    }
}
