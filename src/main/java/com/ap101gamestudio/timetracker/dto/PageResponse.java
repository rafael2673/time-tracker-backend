package com.ap101gamestudio.timetracker.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int number
) {}