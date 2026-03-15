package com.ap101gamestudio.timetracker.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EmployeeLeaveRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String reason
) {}