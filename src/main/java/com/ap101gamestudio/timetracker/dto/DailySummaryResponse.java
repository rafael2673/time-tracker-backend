package com.ap101gamestudio.timetracker.dto;

public record DailySummaryResponse(
        String day,
        double hours,
        double expectedHours,
        String date
) {
}
