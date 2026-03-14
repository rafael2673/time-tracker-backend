package com.ap101gamestudio.timetracker.dto;

import java.time.LocalDate;
import java.util.UUID;

public record SpecialDateResponse(
        UUID id,
        LocalDate date,
        String description,
        Double workloadMultiplier,
        boolean isRecurring
) {}