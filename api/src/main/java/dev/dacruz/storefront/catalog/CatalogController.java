package dev.dacruz.storefront.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.dacruz.storefront.catalog.dto.PageResponse;
import dev.dacruz.storefront.catalog.dto.ProductDetail;
import dev.dacruz.storefront.catalog.dto.ProductSummary;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    public PageResponse<ProductSummary> browse(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        return catalog.browse(category, q, page, size, sort);
    }

    @GetMapping("/products/{sku}")
    public ProductDetail detail(@PathVariable String sku) {
        return catalog.detail(sku);
    }
}
