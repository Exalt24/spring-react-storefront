package dev.dacruz.storefront.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.dacruz.storefront.catalog.dto.PageResponse;
import dev.dacruz.storefront.catalog.dto.ProductDetail;
import dev.dacruz.storefront.catalog.dto.ProductSummary;
import dev.dacruz.storefront.common.NotFoundException;
import dev.dacruz.storefront.support.PostgresIntegrationTest;

class CatalogServiceIT extends PostgresIntegrationTest {

    @Autowired
    CatalogService catalog;

    @Test
    void browseReturnsSeededProductsWithPagingEnvelope() {
        PageResponse<ProductSummary> page = catalog.browse(null, null, 0, 4, "newest");

        assertThat(page.items()).hasSize(4);
        assertThat(page.totalItems()).isEqualTo(6);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.page()).isZero();
    }

    @Test
    void browseFiltersByCategorySlug() {
        PageResponse<ProductSummary> page = catalog.browse("keyboards", null, 0, 12, "newest");

        assertThat(page.items())
                .isNotEmpty()
                .allSatisfy(item -> assertThat(item.categorySlug()).isEqualTo("keyboards"));
    }

    @Test
    void browseSearchesNameCaseInsensitively() {
        PageResponse<ProductSummary> page = catalog.browse(null, "KEYBOARD", 0, 12, "newest");

        assertThat(page.items()).isNotEmpty();
        assertThat(page.items()).allSatisfy(
                item -> assertThat(item.name().toLowerCase()).contains("keyboard"));
    }

    @Test
    void browseSortsByPriceAscendingWhenAsked() {
        PageResponse<ProductSummary> page = catalog.browse(null, null, 0, 12, "price_asc");

        assertThat(page.items()).extracting(ProductSummary::priceCents).isSorted();
    }

    /**
     * An unknown sort key must fall back to the default rather than reaching the
     * query, which is what stops a caller ordering by an arbitrary column.
     */
    @Test
    void unknownSortKeyFallsBackInsteadOfFailing() {
        PageResponse<ProductSummary> page = catalog.browse(null, null, 0, 12, "priceCents; drop table product");

        assertThat(page.items()).isNotEmpty();
    }

    @Test
    void summaryPayloadOmitsDescriptionButDetailIncludesIt() {
        ProductSummary summary = catalog.browse(null, "Studio Monitor", 0, 1, "newest").items().getFirst();
        ProductDetail detail = catalog.detail(summary.sku());

        // The record has no description component at all, which is the point.
        assertThat(ProductSummary.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("description");
        assertThat(detail.description()).isNotBlank();
        assertThat(detail.categoryName()).isEqualTo("Audio");
    }

    @Test
    void inStockFlagReflectsZeroStock() {
        PageResponse<ProductSummary> page = catalog.browse("desk", null, 0, 12, "newest");

        assertThat(page.items())
                .filteredOn(item -> item.sku().equals("DSK-2002"))
                .singleElement()
                .satisfies(item -> assertThat(item.inStock()).isFalse());
    }

    @Test
    void lowStockFlagTripsUnderThreshold() {
        assertThat(catalog.detail("AUD-1002").lowStock()).isTrue();   // 3 left
        assertThat(catalog.detail("KEY-3001").lowStock()).isFalse();  // 20 left
    }

    @Test
    void pageSizeIsClampedSoACallerCannotAskForEverything() {
        PageResponse<ProductSummary> page = catalog.browse(null, null, 0, 5000, "newest");

        assertThat(page.size()).isEqualTo(60);
    }

    @Test
    void unknownSkuRaisesNotFound() {
        assertThatThrownBy(() -> catalog.detail("NOPE-0000"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("NOPE-0000");
    }
}
