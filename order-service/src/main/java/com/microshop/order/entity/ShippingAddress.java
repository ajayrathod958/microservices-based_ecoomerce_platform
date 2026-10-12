package com.microshop.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    @Column(name = "ship_full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "ship_line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "ship_line2", length = 200)
    private String line2;

    @Column(name = "ship_city", nullable = false, length = 100)
    private String city;

    @Column(name = "ship_postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "ship_country", nullable = false, length = 2)
    private String country;
}
