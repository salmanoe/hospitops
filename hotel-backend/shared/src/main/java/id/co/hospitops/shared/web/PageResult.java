package id.co.hospitops.shared.web;

import java.util.List;

/**
 * S-2 FIX: Added {@code first} and {@code last} boolean fields to match
 * ARCHITECTURE.md spec: {@code {content, page, size, totalElements, totalPages, first, last}}.
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResult<T> of(List<T> content, int page, int size, long total) {
        int pages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        boolean isFirst = page == 0;
        boolean isLast = pages == 0 || page >= pages - 1;
        return new PageResult<>(content, page, size, total, pages, isFirst, isLast);
    }
}
