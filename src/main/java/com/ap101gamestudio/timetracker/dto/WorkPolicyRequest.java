package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.OvertimeStrategy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkPolicyRequest(
        @NotBlank String name,
        @Min(0) int dailyMinutesLimit,
        @Min(0) int toleranceMinutes,
        @NotBlank String workingDays,
        @NotNull OvertimeStrategy overtimeStrategy,
        @Min(0) double maxBankHoursPerMonth,
        @Min(0) int bankExpirationMonths
) {}