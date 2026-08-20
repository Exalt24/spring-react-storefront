package dev.dacruz.storefront.catalog.dto;

/**
 * The list payload. No description, no stock internals, no audit columns, no
 * version. This is the "optimized JSON payload" half of a BFF: the card grid
 * needs six fields, so six fields cross the wire.
 */
public record ProductSummary(
        String sku,
        String name,
        long priceCents,
        String currency,
        String imageUrl,
        boolean inStock,
        String categorySlug) {
}
