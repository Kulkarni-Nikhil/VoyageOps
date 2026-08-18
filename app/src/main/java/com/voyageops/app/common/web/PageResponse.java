package com.voyageops.app.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(int page, int size, long totalElements, int totalPages, List<T> content) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent());
    }
}