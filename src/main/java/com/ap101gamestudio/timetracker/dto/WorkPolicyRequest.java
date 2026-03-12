package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkPolicyRequest(
        @NotBlank String name,
        @Min(0) int dailyMinutesLimit,
        @Min(0) int toleranceMinutes,
        @NotBlank String workingDays
) {}