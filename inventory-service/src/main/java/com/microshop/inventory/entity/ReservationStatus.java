package com.microshop.inventory.entity;

public enum ReservationStatus {
    RESERVED,    // stock held for a pending order
    RELEASED,    // order failed/cancelled: stock returned (compensating action)
    COMMITTED    // order confirmed: stock permanently deducted
}
