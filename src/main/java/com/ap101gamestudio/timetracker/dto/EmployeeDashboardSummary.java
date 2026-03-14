package com.ap101gamestudio.timetracker.dto;

public record EmployeeDashboardSummary(
        double workedHours,
        double balance,
        long pendingJustifications
) {}