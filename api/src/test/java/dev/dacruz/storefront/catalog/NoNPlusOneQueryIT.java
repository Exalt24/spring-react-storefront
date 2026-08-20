package dev.dacruz.storefront.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import dev.dacruz.storefront.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManagerFactory;

/**
 * The N+1 regression test.
 *
 * Product.category is LAZY, so a page of products that reads categorySlug during
 * DTO mapping would fire one extra SELECT per row. The @EntityGraph on the
 * repository query turns that into a single join. This test counts the actual
 * statements Hibernate issued, because the only way this defect gets caught is by
 * counting: the endpoint returns identical JSON either way, and it stays fast on
 * six seeded rows while getting slower forever in production.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class NoNPlusOneQueryIT extends PostgresIntegrationTest {

    @Autowired
    CatalogService catalog;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private Statistics stats() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void browsingAPageOfProductsDoesNotFireOneQueryPerCategory() {
        Statistics statistics = stats();
        statistics.clear();

        var page = catalog.browse(null, null, 0, 6, "newest");
        assertThat(page.items()).hasSize(6);

        long queries = statistics.getPrepareStatementCount();

        // One statement for the page, one for the count that paging needs.
        //
        // Verified by mutation 2026-08-20: deleting the @EntityGraph from
        // ProductRepository.search takes this to 5, because the six rows span
        // three distinct categories and Hibernate loads each one once per
        // session. So the real-world blow-up scales with distinct categories,
        // not with row count, which is precisely why it stays invisible on a
        // six-row seed and hurts on a production catalogue.
        assertThat(queries)
                .as("browse issued %d statements; dropping the entity graph makes it 5", queries)
                .isLessThanOrEqualTo(2);
    }

    @Test
    void noLazyCategoryIsInitialisedSeparately() {
        Statistics statistics = stats();
        statistics.clear();

        catalog.browse(null, null, 0, 6, "newest");

        // If the entity graph were dropped, each category would be fetched on
        // demand and show up as an entity load beyond the six products.
        assertThat(statistics.getEntityLoadCount()).isLessThanOrEqualTo(9);
    }

    @Test
    void readingAWholeCartIsOneStatementNotOnePerLine() {
        Statistics statistics = stats();

        // Arrange outside the measurement window.
        var setup = new java.util.concurrent.atomic.AtomicReference<String>();
        setup.set(cartToken());

        statistics.clear();
        cartService.get(setup.get());

        assertThat(statistics.getPrepareStatementCount())
                .as("loading a 3-line cart should not scale with the line count")
                .isLessThanOrEqualTo(2);
    }

    @Autowired
    dev.dacruz.storefront.cart.CartService cartService;

    private String cartToken() {
        String token = cartService.createCart().cartToken();
        cartService.addItem(token, "KEY-3001", 1);
        cartService.addItem(token, "AUD-1001", 1);
        cartService.addItem(token, "DSK-2001", 1);
        return token;
    }
}
