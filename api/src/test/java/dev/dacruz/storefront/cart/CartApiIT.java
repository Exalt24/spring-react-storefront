package dev.dacruz.storefront.cart;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.dacruz.storefront.support.PostgresIntegrationTest;

/**
 * The HTTP contract the React client codes against: status codes, the single
 * error envelope, and field-level validation messages.
 */
@AutoConfigureMockMvc
class CartApiIT extends PostgresIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String newCartToken() throws Exception {
        String body = mvc.perform(post("/api/cart"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.cartToken");
    }

    @Test
    void browseReturnsThePagingEnvelope() throws Exception {
        mvc.perform(get("/api/catalog/products").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void listPayloadDoesNotShipDescriptionsOrStockCounts() throws Exception {
        mvc.perform(get("/api/catalog/products").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").exists())
                .andExpect(jsonPath("$.items[0].description").doesNotExist())
                .andExpect(jsonPath("$.items[0].stockQty").doesNotExist())
                .andExpect(jsonPath("$.items[0].version").doesNotExist());
    }

    @Test
    void detailReturns404WithTheSharedErrorShape() throws Exception {
        mvc.perform(get("/api/catalog/products/NOPE-0000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void addItemReturnsTheWholeCartSoTheUiNeedsNoSecondCall() throws Exception {
        String token = newCartToken();

        mvc.perform(post("/api/cart/{t}/items", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"KEY-3001\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.totalCents").value(greaterThan(0)));
    }

    @Test
    void quantityBelowOneIsRejectedWithAFieldLevelMessage() throws Exception {
        String token = newCartToken();

        mvc.perform(post("/api/cart/{t}/items", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"KEY-3001\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void quantityAboveTheOrderLimitIsRejected() throws Exception {
        String token = newCartToken();

        mvc.perform(post("/api/cart/{t}/items", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"KEY-3001\",\"quantity\":21}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void blankSkuIsRejected() throws Exception {
        String token = newCartToken();

        mvc.perform(post("/api/cart/{t}/items", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"  \",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.sku").exists());
    }

    /** Out of stock is a 409, distinct from a 404 and from a validation 400. */
    @Test
    void oversellIsAConflictNotAServerError() throws Exception {
        String token = newCartToken();

        mvc.perform(post("/api/cart/{t}/items", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"DSK-2002\",\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void patchThenDeleteWalkTheLineDownAndOut() throws Exception {
        String token = newCartToken();
        mvc.perform(post("/api/cart/{t}/items", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"KEY-3001\",\"quantity\":3}"))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/cart/{t}/items/{sku}", token, "KEY-3001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].quantity").value(1));

        mvc.perform(delete("/api/cart/{t}/items/{sku}", token, "KEY-3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    void unknownCartTokenIs404() throws Exception {
        mvc.perform(get("/api/cart/{t}", "no-such-cart"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void healthProbeIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
