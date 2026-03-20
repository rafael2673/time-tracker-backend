package com.ap101gamestudio.timetracker.dto;

public record BankAccumulationDto(
        double previousAccumulated,
        double newAccumulated
) {
}
