package dev.dacruz.storefront.cart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.dacruz.storefront.catalog.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opaque public handle. The numeric id never leaves the server. */
    @Column(name = "public_token", nullable = false, unique = true)
    private String publicToken;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Cart() {
    }

    public Cart(String publicToken) {
        this.publicToken = publicToken;
    }

    /**
     * Adding the same sku twice bumps the existing line instead of creating a
     * duplicate, which is what a shopper expects and what keeps the totals honest.
     */
    public CartItem addOrIncrement(Product product, int quantity) {
        Optional<CartItem> existing = items.stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.increaseBy(quantity);
            return item;
        }
        CartItem item = new CartItem(this, product, quantity, product.getPriceCents());
        items.add(item);
        return item;
    }

    public Optional<CartItem> findBySku(String sku) {
        return items.stream()
                .filter(item -> item.getProduct().getSku().equals(sku))
                .findFirst();
    }

    public void remove(CartItem item) {
        items.remove(item);
    }

    /** Sum of captured line prices, not current catalog prices. */
    public long subtotalCents() {
        return items.stream()
                .mapToLong(item -> item.getUnitPriceCents() * item.getQuantity())
                .sum();
    }

    public int totalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public Long getId() {
        return id;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
