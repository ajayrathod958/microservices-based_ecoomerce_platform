package com.microshop.payment.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    private ChargeResult charge(String token) {
        return gateway.charge(token, new BigDecimal("10.00"), "USD");
    }

    @Test
    void successTokenIsApprovedWithAUniqueReference() {
        ChargeResult a = charge("tok_success");
        ChargeResult b = charge("tok_success");

        assertThat(a.success()).isTrue();
        assertThat(a.transactionRef()).startsWith("mock_").isNotEqualTo(b.transactionRef());
    }

    @Test
    void failureTokensGiveReadableReasons() {
        assertThat(charge("tok_decline").failureReason()).isEqualTo("Card was declined");
        assertThat(charge("tok_insufficient_funds").failureReason()).isEqualTo("Insufficient funds");
        assertThat(charge("tok_gateway_error").failureReason()).isEqualTo("Payment gateway unavailable");
        assertThat(charge("tok_decline").success()).isFalse();
    }

    @Test
    void unknownOrMissingTokenIsRejected() {
        assertThat(charge("4111111111111111").failureReason()).isEqualTo("Invalid payment method");
        assertThat(charge(null).success()).isFalse();
    }
}
