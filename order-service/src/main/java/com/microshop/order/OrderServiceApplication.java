package com.microshop.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {"com.microshop.order", "com.microshop.common"},
        exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackages = {"com.microshop.order", "com.microshop.common.messaging"})
@EnableJpaRepositories(basePackages = {"com.microshop.order", "com.microshop.common.messaging"})
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
