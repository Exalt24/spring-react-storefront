package dev.dacruz.storefront.catalog.dto;

import dev.dacruz.storefront.catalog.Product;

/** The detail payload. Adds description and a bounded stock signal. */
public record ProductDetail(
        String sku,
        String name,
        String description,
        long priceCents,
        String currency,
        String imageUrl,
        String categorySlug,
        String categoryName,
        int availableQty,
        boolean lowStock) {

    private static final int LOW_STOCK_THRESHOLD = 5;

    public static ProductDetail from(Product product) {
        return new ProductDetail(
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPriceCents(),
                product.getCurrency(),
                product.getImageUrl(),
                product.getCategory().getSlug(),
                product.getCategory().getName(),
                product.getStockQty(),
                product.getStockQty() > 0 && product.getStockQty() <= LOW_STOCK_THRESHOLD);
    }
}
