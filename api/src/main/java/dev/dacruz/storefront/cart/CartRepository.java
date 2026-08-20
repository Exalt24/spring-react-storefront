package dev.dacruz.storefront.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Fetches the cart, its lines, and each line's product in one query. Without
     * the graph, rendering a five-line cart costs eleven queries.
     */
    @EntityGraph(attributePaths = { "items", "items.product", "items.product.category" })
    Optional<Cart> findByPublicToken(String publicToken);
}
