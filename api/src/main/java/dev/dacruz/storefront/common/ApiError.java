package dev.dacruz.storefront.common;

import java.time.Instant;
import java.util.Map;

/**
 * One error shape for every failure the UI can see, so the frontend has a single
 * branch to render instead of guessing at Spring's default payloads.
 */
public record ApiError(
        String code,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of(), Instant.now());
    }

    public static ApiError validation(Map<String, String> fieldErrors) {
        return new ApiError("VALIDATION_FAILED", "One or more fields are invalid.", fieldErrors, Instant.now());
    }
}
