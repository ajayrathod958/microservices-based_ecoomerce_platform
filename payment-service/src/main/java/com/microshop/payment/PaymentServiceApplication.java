package com.microshop.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {"com.microshop.payment", "com.microshop.common"},
        exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackages = {"com.microshop.payment", "com.microshop.common.messaging"})
@EnableJpaRepositories(basePackages = {"com.microshop.payment", "com.microshop.common.messaging"})
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
