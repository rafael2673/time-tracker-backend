package com.ap101gamestudio.timetracker.dto;

import java.util.List;

public record EmployeeDashboardSummary(
        double workedHours,
        double expectedHours,
        double balance,
        double monthlyBalance,
        int unjustifiedAbsences,
        long pendingJustifications,
        int vacationDays,
        List<DailySummaryResponse> weeklyActivity
) {}