package com.ap101gamestudio.timetracker.dto;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeLeaveResponse(
        UUID id,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {}