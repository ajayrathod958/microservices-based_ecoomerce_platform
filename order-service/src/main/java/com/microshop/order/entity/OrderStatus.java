package com.microshop.order.entity;

public enum OrderStatus {
    PENDING,         // saved, waiting for inventory
    STOCK_RESERVED,  // inventory held the stock, waiting for payment
    CONFIRMED,       // paid: final
    CANCELLED;       // stock or payment failed: final

    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELLED;
    }
}
