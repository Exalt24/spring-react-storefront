package dev.dacruz.storefront.cart;

import dev.dacruz.storefront.catalog.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cart_item", uniqueConstraints = @UniqueConstraint(columnNames = { "cart_id", "product_id" }))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    /**
     * Price captured at add-to-cart time. A later catalog price change must not
     * silently re-price a cart the shopper is already looking at.
     */
    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    protected CartItem() {
    }

    CartItem(Cart cart, Product product, int quantity, long unitPriceCents) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    void increaseBy(int amount) {
        this.quantity += amount;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public long lineTotalCents() {
        return unitPriceCents * quantity;
    }
}
