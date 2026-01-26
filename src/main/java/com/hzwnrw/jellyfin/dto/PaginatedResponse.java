package com.hzwnrw.jellyfin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response DTO for REST APIs
 */
@Data
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> content;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private String sortBy;
    private String direction;

    /**
     * Factory method to create PaginatedResponse from a Spring Data Page
     */
    public static <T> PaginatedResponse<T> from(Page<T> page, String sortBy, String direction) {
        return new PaginatedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious(),
            sortBy,
            direction
        );
    }
}
