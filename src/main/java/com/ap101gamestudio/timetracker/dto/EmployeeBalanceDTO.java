package com.ap101gamestudio.timetracker.dto;

import java.util.UUID;

public record EmployeeBalanceDTO(
        UUID employeeId,
        String fullName,
        double balance
) {}
