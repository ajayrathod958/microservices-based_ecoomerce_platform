package com.microshop.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {"com.microshop.inventory", "com.microshop.common"},
        exclude = UserDetailsServiceAutoConfiguration.class)
// common.messaging brings the outbox + processed_events entities/repositories
@EntityScan(basePackages = {"com.microshop.inventory", "com.microshop.common.messaging"})
@EnableJpaRepositories(basePackages = {"com.microshop.inventory", "com.microshop.common.messaging"})
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
