package com.microshop.cart.entity;

import com.microshop.cart.dto.CartResponse;
import com.microshop.common.exception.BadRequestException;
import com.microshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartEntityTest {

    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();
    private final CartEntity cart = CartEntity.newFor(UUID.randomUUID());

    @Test
    void addingSameProductIncrementsQuantityAndRefreshesPrice() {
        cart.addOrIncrease(p1, "Phone", null, new BigDecimal("10.00"), 2);
        cart.addOrIncrease(p1, "Phone v2", null, new BigDecimal("12.50"), 1);

        assertThat(cart.getItems()).hasSize(1);
        CartItemEntity item = cart.getItems().get(0);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(item.getProductName()).isEqualTo("Phone v2");
    }

    @Test
    void quantityPerItemIsCappedAt99() {
        cart.addOrIncrease(p1, "Phone", null, BigDecimal.TEN, 98);

        assertThatThrownBy(() -> cart.addOrIncrease(p1, "Phone", null, BigDecimal.TEN, 2))
                .isInstanceOf(BadRequestException.class);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(98);
    }

    @Test
    void setQuantityValidatesAndRequiresExistingItem() {
        cart.addOrIncrease(p1, "Phone", null, BigDecimal.TEN, 1);

        cart.setQuantity(p1, 5);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThatThrownBy(() -> cart.setQuantity(p1, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> cart.setQuantity(p2, 1)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeIsIdempotentAndClearEmptiesTheCart() {
        cart.addOrIncrease(p1, "A", null, BigDecimal.ONE, 1);
        cart.addOrIncrease(p2, "B", null, BigDecimal.ONE, 1);

        cart.removeItem(p1);
        cart.removeItem(p1);   // no error the second time
        assertThat(cart.getItems()).extracting(CartItemEntity::getProductId).containsExactly(p2);

        cart.clear();
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void responseComputesLineTotalsAndSubtotal() {
        cart.addOrIncrease(p1, "A", null, new BigDecimal("10.00"), 2);     // 20.00
        cart.addOrIncrease(p2, "B", null, new BigDecimal("2.55"), 3);      // 7.65

        CartResponse res = CartResponse.from(cart);

        assertThat(res.totalItems()).isEqualTo(5);
        assertThat(res.subtotal()).isEqualByComparingTo("27.65");
        assertThat(res.items().get(1).lineTotal()).isEqualByComparingTo("7.65");
    }

    @Test
    void mutationsBumpUpdatedAtSoTheVersionColumnChanges() throws InterruptedException {
        var before = cart.getUpdatedAt();
        Thread.sleep(5);

        cart.addOrIncrease(p1, "A", null, BigDecimal.ONE, 1);

        assertThat(cart.getUpdatedAt()).isAfter(before);
    }
}
