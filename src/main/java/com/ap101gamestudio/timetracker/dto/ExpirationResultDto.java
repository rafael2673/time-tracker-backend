package com.ap101gamestudio.timetracker.dto;

public record ExpirationResultDto(
        OvertimeCalculationDto overtime,
        BankAccumulationDto accumulation
) {
}
