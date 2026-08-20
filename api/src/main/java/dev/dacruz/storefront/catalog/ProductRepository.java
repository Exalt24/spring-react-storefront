package dev.dacruz.storefront.catalog;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * The N+1 fix. Product.category is LAZY, so without this entity graph a page
     * of 20 rows issues 1 query for the page plus 20 more to resolve each
     * category when the DTO reads its slug. The graph makes it one join.
     */
    /**
     * Filters are passed as empty strings rather than nulls on purpose. A null
     * String parameter arrives at Postgres with no inferred type, which turned
     * `lower(:query)` into `lower(bytea)` and failed with a grammar error at
     * runtime while compiling and starting up perfectly happily. Measured
     * 2026-08-20 on the first real run of this query.
     */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.active = true
              and (:categorySlug = '' or p.category.slug = :categorySlug)
              and (:query = '' or lower(p.name) like lower(concat('%', :query, '%')))
            """)
    Page<Product> search(@Param("categorySlug") String categorySlug,
            @Param("query") String query,
            Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySkuAndActiveTrue(String sku);

    Optional<Product> findBySku(String sku);
}
