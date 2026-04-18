package com.ap101gamestudio.timetracker.dto;

public record TimeDistributionResponse(
        double regularHours,
        double overtimeHours,
        double absenceHours,
        double totalExpectedHours
) {}
