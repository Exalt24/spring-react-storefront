package dev.dacruz.storefront.cart.dto;

import java.util.List;

/**
 * Totals are computed server side and shipped ready to render. The UI should not
 * be doing money arithmetic, and two clients doing it separately is how a cart
 * total and a checkout total end up disagreeing.
 */
public record CartView(
        String cartToken,
        List<CartLine> lines,
        int totalQuantity,
        long subtotalCents,
        long shippingCents,
        long taxCents,
        long totalCents,
        String currency) {
}
