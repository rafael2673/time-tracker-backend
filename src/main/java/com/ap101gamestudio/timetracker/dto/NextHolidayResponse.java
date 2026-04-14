package com.ap101gamestudio.timetracker.dto;

import java.time.LocalDate;

public record NextHolidayResponse(
        String name,
        LocalDate date,
        double multiplier
) {}