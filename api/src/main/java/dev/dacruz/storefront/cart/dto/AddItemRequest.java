package dev.dacruz.storefront.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * The same three rules the React form enforces: sku present, quantity 1..20.
 * Enforced here too, because a form is a convenience and not a boundary.
 */
public record AddItemRequest(
        @NotBlank(message = "Pick a product first.") String sku,
        @Min(value = 1, message = "Quantity has to be at least 1.") @Max(value = 20, message = "20 per order is the limit.") int quantity) {
}
