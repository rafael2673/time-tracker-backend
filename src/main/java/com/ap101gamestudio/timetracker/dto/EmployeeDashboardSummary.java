package com.ap101gamestudio.timetracker.dto;

public record EmployeeDashboardSummary(
        double workedHours,
        double expectedHours,
        double balance,
        double monthlyBalance,
        int unjustifiedAbsences,
        long pendingJustifications,
        int vacationDays
) {}