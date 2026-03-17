package com.ap101gamestudio.timetracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonthlyClosureResponse(
        UUID id,
        String employeeName,
        int year,
        int month,
        double workedHours,
        double expectedHours,
        double rawBalance,
        double paidOvertimeHours,
        double bankedHoursDelta,
        LocalDateTime closedAt
) {}