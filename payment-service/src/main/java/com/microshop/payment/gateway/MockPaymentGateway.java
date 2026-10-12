package com.microshop.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic fake payment provider, driven by the token the client sends at checkout:
 *   tok_success            approved
 *   tok_decline            "Card was declined"
 *   tok_insufficient_funds "Insufficient funds"
 *   tok_gateway_error      "Payment gateway unavailable"
 * Anything else is rejected as an invalid payment method. No card data ever reaches the platform.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public ChargeResult charge(String paymentToken, BigDecimal amount, String currency) {
        if (paymentToken == null) {
            return ChargeResult.declined("Invalid payment method");
        }
        return switch (paymentToken) {
            case "tok_success" -> ChargeResult.approved("mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            case "tok_decline" -> ChargeResult.declined("Card was declined");
            case "tok_insufficient_funds" -> ChargeResult.declined("Insufficient funds");
            case "tok_gateway_error" -> ChargeResult.declined("Payment gateway unavailable");
            default -> ChargeResult.declined("Invalid payment method");
        };
    }
}
