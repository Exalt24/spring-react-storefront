package dev.dacruz.storefront.cart;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.dacruz.storefront.cart.dto.AddItemRequest;
import dev.dacruz.storefront.cart.dto.CartView;
import dev.dacruz.storefront.cart.dto.UpdateItemRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartView create() {
        return cart.createCart();
    }

    @GetMapping("/{cartToken}")
    public CartView get(@PathVariable String cartToken) {
        return cart.get(cartToken);
    }

    @PostMapping("/{cartToken}/items")
    public CartView addItem(@PathVariable String cartToken, @Valid @RequestBody AddItemRequest request) {
        return cart.addItem(cartToken, request.sku(), request.quantity());
    }

    @PatchMapping("/{cartToken}/items/{sku}")
    public CartView updateItem(@PathVariable String cartToken, @PathVariable String sku,
            @Valid @RequestBody UpdateItemRequest request) {
        return cart.updateQuantity(cartToken, sku, request.quantity());
    }

    @DeleteMapping("/{cartToken}/items/{sku}")
    public CartView removeItem(@PathVariable String cartToken, @PathVariable String sku) {
        return cart.removeItem(cartToken, sku);
    }
}
