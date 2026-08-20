package dev.dacruz.storefront.cart.dto;

public record CartLine(
        String sku,
        String name,
        String imageUrl,
        int quantity,
        long unitPriceCents,
        long lineTotalCents,
        int availableQty) {
}
