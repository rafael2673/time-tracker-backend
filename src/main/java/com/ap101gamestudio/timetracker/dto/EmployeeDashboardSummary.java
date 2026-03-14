package com.ap101gamestudio.timetracker.dto;

public record EmployeeDashboardSummary(
        String workedHours,
        String balance,
        int pendingJustifications
) {}