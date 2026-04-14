package com.ap101gamestudio.timetracker.dto;

public record AbsencePieChartResponse(
        int totalExpectedDays,
        int totalAbsences,
        double absencePercentage
) {}