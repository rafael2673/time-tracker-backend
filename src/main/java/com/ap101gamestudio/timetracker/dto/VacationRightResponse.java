package com.ap101gamestudio.timetracker.dto;

public record VacationRightResponse(
        int totalUnjustifiedAbsences,
        int earnedVacationDays,
        int referenceYear
) {}