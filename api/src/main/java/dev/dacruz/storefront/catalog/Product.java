package dev.dacruz.storefront.catalog;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    /** Long text, deliberately never sent on list endpoints. */
    @Column(columnDefinition = "text")
    private String description;

    /** Money as integer minor units. Never a double. */
    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "stock_qty", nullable = false)
    private int stockQty;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * LAZY on purpose. The list endpoint would otherwise fire one category query
     * per row; the fix is an entity graph on the query, not eager fetching here.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Optimistic lock. Two concurrent checkouts reading the same stock row will
     * see one commit win and the other fail rather than both decrementing.
     */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Product() {
    }

    public Product(String sku, String name, String description, long priceCents,
            int stockQty, String imageUrl, Category category) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.stockQty = stockQty;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    /**
     * Domain rule lives on the entity, so no caller can reserve stock that is not
     * there by writing the field directly.
     */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        if (quantity > stockQty) {
            throw new IllegalStateException(
                    "Only " + stockQty + " left of " + sku + ", cannot reserve " + quantity + ".");
        }
        this.stockQty -= quantity;
    }

    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        this.stockQty += quantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public int getStockQty() {
        return stockQty;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public Category getCategory() {
        return category;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
