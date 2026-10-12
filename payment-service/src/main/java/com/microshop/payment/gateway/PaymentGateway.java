package com.microshop.payment.gateway;

import java.math.BigDecimal;

/** Port to the "bank". Swap the mock for a real PSP adapter without touching the saga code. */
public interface PaymentGateway {
    ChargeResult charge(String paymentToken, BigDecimal amount, String currency);
}
