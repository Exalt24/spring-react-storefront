package dev.dacruz.storefront.cart;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.dacruz.storefront.cart.dto.CartLine;
import dev.dacruz.storefront.cart.dto.CartView;
import dev.dacruz.storefront.catalog.Product;
import dev.dacruz.storefront.catalog.ProductRepository;
import dev.dacruz.storefront.common.NotFoundException;

@Service
public class CartService {

    /** Flat-rate shipping in minor units, waived above the threshold. */
    private static final long SHIPPING_CENTS = 799L;
    private static final long FREE_SHIPPING_THRESHOLD_CENTS = 7500L;
    private static final int TAX_BASIS_POINTS = 1200; // 12%

    private final CartRepository carts;
    private final ProductRepository products;

    public CartService(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    @Transactional
    public CartView createCart() {
        Cart cart = carts.save(new Cart(UUID.randomUUID().toString()));
        return view(cart);
    }

    @Transactional(readOnly = true)
    public CartView get(String cartToken) {
        return view(load(cartToken));
    }

    /**
     * Reserving stock and writing the cart line happen in one transaction. If the
     * reserve fails the version check, nothing is written, so a cart can never
     * hold a line that stock was not actually decremented for.
     */
    @Transactional
    public CartView addItem(String cartToken, String sku, int quantity) {
        Cart cart = load(cartToken);
        Product product = products.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new NotFoundException("No active product with sku " + sku + "."));
        product.reserve(quantity);
        cart.addOrIncrement(product, quantity);
        return view(cart);
    }

    @Transactional
    public CartView updateQuantity(String cartToken, String sku, int newQuantity) {
        Cart cart = load(cartToken);
        CartItem line = cart.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Cart has no line for sku " + sku + "."));
        int delta = newQuantity - line.getQuantity();
        if (delta > 0) {
            line.getProduct().reserve(delta);
        } else if (delta < 0) {
            line.getProduct().release(-delta);
        }
        if (newQuantity == 0) {
            cart.remove(line);
        } else {
            line.setQuantity(newQuantity);
        }
        return view(cart);
    }

    @Transactional
    public CartView removeItem(String cartToken, String sku) {
        Cart cart = load(cartToken);
        CartItem line = cart.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Cart has no line for sku " + sku + "."));
        line.getProduct().release(line.getQuantity());
        cart.remove(line);
        return view(cart);
    }

    private Cart load(String cartToken) {
        return carts.findByPublicToken(cartToken)
                .orElseThrow(() -> new NotFoundException("No cart for token " + cartToken + "."));
    }

    private CartView view(Cart cart) {
        List<CartLine> lines = cart.getItems().stream()
                .map(item -> new CartLine(
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getProduct().getImageUrl(),
                        item.getQuantity(),
                        item.getUnitPriceCents(),
                        item.lineTotalCents(),
                        item.getProduct().getStockQty()))
                .toList();

        long subtotal = cart.subtotalCents();
        long shipping = shippingFor(subtotal);
        long tax = Math.round(subtotal * (TAX_BASIS_POINTS / 10000.0));

        return new CartView(
                cart.getPublicToken(),
                lines,
                cart.totalQuantity(),
                subtotal,
                shipping,
                tax,
                subtotal + shipping + tax,
                "USD");
    }

    private long shippingFor(long subtotalCents) {
        if (subtotalCents == 0 || subtotalCents >= FREE_SHIPPING_THRESHOLD_CENTS) {
            return 0L;
        }
        return SHIPPING_CENTS;
    }
}
