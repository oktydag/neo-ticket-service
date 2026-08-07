package com.neo.ticket.shared.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final int MAX_PAGE_SIZE = 100;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    public static Pageable toPageable(Integer page, Integer size, Sort sort) {
        int safePage = page == null || page < 0 ? 0 : page;
        int requestedSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : size;
        return PageRequest.of(safePage, Math.min(requestedSize, MAX_PAGE_SIZE), sort);
    }
}
