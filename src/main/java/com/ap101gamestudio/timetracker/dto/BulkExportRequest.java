package com.ap101gamestudio.timetracker.dto;

public record BulkExportRequest(
        int year,
        int month,
        String locale
) {}