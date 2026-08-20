package dev.dacruz.storefront.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateItemRequest(
        @Min(value = 0, message = "Quantity cannot be negative.") @Max(value = 20, message = "20 per order is the limit.") int quantity) {
}
