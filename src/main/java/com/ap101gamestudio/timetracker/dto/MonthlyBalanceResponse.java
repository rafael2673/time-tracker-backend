package com.ap101gamestudio.timetracker.dto;

public record MonthlyBalanceResponse(
        double workedHours,
        double expectedHours,
        double balance,
        int unjustifiedAbsences,
        double dsrDiscountHours
) {}