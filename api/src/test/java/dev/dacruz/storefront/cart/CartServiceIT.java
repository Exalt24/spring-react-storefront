package dev.dacruz.storefront.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.dacruz.storefront.cart.dto.CartView;
import dev.dacruz.storefront.catalog.CatalogService;
import dev.dacruz.storefront.common.NotFoundException;
import dev.dacruz.storefront.support.PostgresIntegrationTest;

class CartServiceIT extends PostgresIntegrationTest {

    @Autowired
    CartService cart;

    @Autowired
    CatalogService catalog;

    @Test
    void addingAnItemComputesTotalsServerSide() {
        String token = cart.createCart().cartToken();

        CartView view = cart.addItem(token, "KEY-3001", 2);

        assertThat(view.lines()).hasSize(1);
        assertThat(view.totalQuantity()).isEqualTo(2);
        assertThat(view.subtotalCents()).isEqualTo(29800L);  // 14900 * 2
        assertThat(view.shippingCents()).isZero();           // over the free threshold
        assertThat(view.taxCents()).isEqualTo(3576L);        // 12% of 29800
        assertThat(view.totalCents()).isEqualTo(33376L);
    }

    @Test
    void emptyCartHasNoShippingAndNoTotal() {
        CartView view = cart.createCart();

        assertThat(view.lines()).isEmpty();
        assertThat(view.subtotalCents()).isZero();
        assertThat(view.shippingCents()).isZero();
        assertThat(view.totalCents()).isZero();
    }

    @Test
    void addingTheSameSkuTwiceIncrementsOneLine() {
        String token = cart.createCart().cartToken();

        cart.addItem(token, "KEY-3001", 1);
        CartView view = cart.addItem(token, "KEY-3001", 2);

        assertThat(view.lines()).hasSize(1);
        assertThat(view.lines().getFirst().quantity()).isEqualTo(3);
    }

    /**
     * The oversell guard. DSK-2002 is seeded at zero stock, so a reserve must be
     * refused up front rather than producing a negative row that the database
     * check constraint would only catch at flush time.
     */
    @Test
    void cannotAddMoreThanAvailableStock() {
        String token = cart.createCart().cartToken();

        assertThatThrownBy(() -> cart.addItem(token, "DSK-2002", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DSK-2002");
    }

    @Test
    void reservingDecrementsCatalogStockByExactlyTheQuantityTaken() {
        int before = catalog.detail("KEY-3002").availableQty();
        String token = cart.createCart().cartToken();

        cart.addItem(token, "KEY-3002", 3);

        assertThat(catalog.detail("KEY-3002").availableQty()).isEqualTo(before - 3);
    }

    @Test
    void removingALineReleasesTheStockItHeld() {
        int before = catalog.detail("AUD-1001").availableQty();
        String token = cart.createCart().cartToken();
        cart.addItem(token, "AUD-1001", 2);

        cart.removeItem(token, "AUD-1001");

        assertThat(catalog.detail("AUD-1001").availableQty()).isEqualTo(before);
    }

    @Test
    void loweringQuantityReturnsTheDifferenceToStock() {
        int before = catalog.detail("AUD-1001").availableQty();
        String token = cart.createCart().cartToken();
        cart.addItem(token, "AUD-1001", 4);

        CartView view = cart.updateQuantity(token, "AUD-1001", 1);

        assertThat(view.lines().getFirst().quantity()).isEqualTo(1);
        assertThat(catalog.detail("AUD-1001").availableQty()).isEqualTo(before - 1);
    }

    @Test
    void updatingToZeroDropsTheLineAndReleasesEverything() {
        int before = catalog.detail("AUD-1001").availableQty();
        String token = cart.createCart().cartToken();
        cart.addItem(token, "AUD-1001", 2);

        CartView view = cart.updateQuantity(token, "AUD-1001", 0);

        assertThat(view.lines()).isEmpty();
        assertThat(catalog.detail("AUD-1001").availableQty()).isEqualTo(before);
    }

    @Test
    void raisingQuantityBeyondStockIsRefusedAndChangesNothing() {
        String token = cart.createCart().cartToken();
        cart.addItem(token, "AUD-1002", 1);
        int afterAdd = catalog.detail("AUD-1002").availableQty();

        assertThatThrownBy(() -> cart.updateQuantity(token, "AUD-1002", 20))
                .isInstanceOf(IllegalStateException.class);

        assertThat(catalog.detail("AUD-1002").availableQty()).isEqualTo(afterAdd);
    }

    @Test
    void unknownCartTokenRaisesNotFound() {
        assertThatThrownBy(() -> cart.get("not-a-real-token"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownSkuOnAddRaisesNotFound() {
        String token = cart.createCart().cartToken();

        assertThatThrownBy(() -> cart.addItem(token, "NOPE-0000", 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removingALineThatIsNotInTheCartRaisesNotFound() {
        String token = cart.createCart().cartToken();

        assertThatThrownBy(() -> cart.removeItem(token, "KEY-3001"))
                .isInstanceOf(NotFoundException.class);
    }

    /** The public handle must not be the primary key. */
    @Test
    void cartTokenIsOpaqueAndNotTheDatabaseId() {
        String token = cart.createCart().cartToken();

        assertThat(token).hasSizeGreaterThan(20).contains("-");
        assertThat(token).containsPattern("[a-f0-9]{8}-[a-f0-9]{4}");
    }
}
