package com.ap101gamestudio.timetracker.dto;

import java.time.LocalDate;
import java.util.UUID;
import com.ap101gamestudio.timetracker.model.enums.SpecialDateType;

public record SpecialDateResponse(
        UUID id,
        LocalDate date,
        String description,
        Double workloadMultiplier,
        boolean isRecurring,
        SpecialDateType type
) {}