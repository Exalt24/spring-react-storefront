package dev.dacruz.storefront.catalog.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * A stable pagination envelope. Spring's own Page serialization is noisy and its
 * shape has changed across versions, so the contract the UI codes against is
 * declared here instead of being inherited from the framework.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
