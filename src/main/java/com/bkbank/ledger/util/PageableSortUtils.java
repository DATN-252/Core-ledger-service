package com.bkbank.ledger.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

public final class PageableSortUtils {

    private PageableSortUtils() {
    }

    public static Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String defaultSortProperty,
            Map<String, String> sortMappings
    ) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String resolvedSortProperty = resolveSortProperty(sortBy, defaultSortProperty, sortMappings);
        return PageRequest.of(page, size, Sort.by(direction, resolvedSortProperty));
    }

    private static String resolveSortProperty(String sortBy, String defaultSortProperty, Map<String, String> sortMappings) {
        if (sortBy == null || sortBy.isBlank()) {
            return defaultSortProperty;
        }
        return sortMappings.getOrDefault(sortBy.trim().toLowerCase(), defaultSortProperty);
    }
}
