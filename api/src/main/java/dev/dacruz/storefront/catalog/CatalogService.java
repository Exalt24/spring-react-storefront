package dev.dacruz.storefront.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dev.dacruz.storefront.catalog.dto.PageResponse;
import dev.dacruz.storefront.catalog.dto.ProductDetail;
import dev.dacruz.storefront.catalog.dto.ProductSummary;
import dev.dacruz.storefront.common.NotFoundException;

@Service
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 60;

    private final ProductRepository products;

    public CatalogService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummary> browse(String categorySlug, String query, int page, int size, String sort) {
        Page<Product> found = products.search(
                blankToEmpty(categorySlug),
                blankToEmpty(query),
                PageRequest.of(Math.max(page, 0), clampSize(size), sortOf(sort)));
        return PageResponse.from(found.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public ProductDetail detail(String sku) {
        return products.findBySkuAndActiveTrue(sku)
                .map(ProductDetail::from)
                .orElseThrow(() -> new NotFoundException("No active product with sku " + sku + "."));
    }

    private ProductSummary toSummary(Product product) {
        return new ProductSummary(
                product.getSku(),
                product.getName(),
                product.getPriceCents(),
                product.getCurrency(),
                product.getImageUrl(),
                product.getStockQty() > 0,
                product.getCategory().getSlug());
    }

    /**
     * Sort is whitelisted rather than passed through. An unchecked sort parameter
     * lets a caller order by any mapped column, which leaks column names through
     * error messages and can be used to probe the schema.
     */
    private Sort sortOf(String sort) {
        return switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "priceCents");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "priceCents");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 12;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** Empty string, never null. See the note on ProductRepository.search. */
    private String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
