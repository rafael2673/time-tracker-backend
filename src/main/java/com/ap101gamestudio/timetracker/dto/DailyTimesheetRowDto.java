package com.ap101gamestudio.timetracker.dto;

import java.time.LocalDate;

public record DailyTimesheetRowDto(
        LocalDate date,
        String dayOfWeek,
        String entry1,
        String exit1,
        String entry2,
        String exit2,
        String totalWorkedHours,
        String justification,
        boolean isHighlightDay
) {}