package com.microshop.order.entity;

import com.microshop.events.OrderLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerOrderTest {

    private CustomerOrder newOrder() {
        return CustomerOrder.create(UUID.randomUUID(), "a@b.com",
                List.of(new OrderLine(UUID.randomUUID(), "Phone", 2, new BigDecimal("10.00")),
                        new OrderLine(UUID.randomUUID(), "Case", 3, new BigDecimal("2.55"))),
                new ShippingAddress("Jane Doe", "1 Main St", null, "Pune", "411001", "IN"),
                "USD", null);
    }

    @Test
    void startsPendingWithTotalAndFirstTimelineEntry() {
        CustomerOrder order = newOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("27.65");
        assertThat(order.getHistory()).extracting(OrderStatusHistory::getStatus).containsExactly(OrderStatus.PENDING);
        assertThat(order.toLines()).hasSize(2);
    }

    @Test
    void happyPathWalksTheTimeline() {
        CustomerOrder order = newOrder();

        assertThat(order.markStockReserved()).isTrue();
        assertThat(order.confirm()).isTrue();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getHistory()).extracting(OrderStatusHistory::getStatus)
                .containsExactly(OrderStatus.PENDING, OrderStatus.STOCK_RESERVED, OrderStatus.CONFIRMED);
    }

    @Test
    void paymentMayArriveBeforeTheStockReservedEvent() {
        CustomerOrder order = newOrder();

        assertThat(order.confirm()).isTrue();             // payment-completed overtook inventory-reserved
        assertThat(order.markStockReserved()).isFalse();  // the late event is ignored

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelRecordsReasonAndFinalStatesNeverChange() {
        CustomerOrder order = newOrder();

        assertThat(order.cancel("Payment failed: Card was declined")).isTrue();
        assertThat(order.getFailureReason()).isEqualTo("Payment failed: Card was declined");

        assertThat(order.confirm()).isFalse();
        assertThat(order.cancel("again")).isFalse();
        assertThat(order.markStockReserved()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getFailureReason()).isEqualTo("Payment failed: Card was declined");
    }
}
