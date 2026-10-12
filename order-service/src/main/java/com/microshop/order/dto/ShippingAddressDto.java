package com.microshop.order.dto;

import com.microshop.order.entity.ShippingAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressDto(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a 2-letter ISO country code") String country) {

    public ShippingAddress toEntity() {
        return new ShippingAddress(fullName.trim(), line1.trim(), line2 == null ? null : line2.trim(),
                city.trim(), postalCode.trim(), country.toUpperCase());
    }

    public static ShippingAddressDto from(ShippingAddress a) {
        return new ShippingAddressDto(a.getFullName(), a.getLine1(), a.getLine2(), a.getCity(),
                a.getPostalCode(), a.getCountry());
    }
}
