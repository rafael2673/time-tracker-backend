package com.ap101gamestudio.timetracker.dto;

public record OvertimeCalculationDto(
        double paidOvertimeHours,
        double bankedDelta
) {
}
