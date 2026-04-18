package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import com.ap101gamestudio.timetracker.model.enums.SpecialDateType;

public record SpecialDateRequest(
        @NotNull LocalDate date,
        @NotBlank String description,
        @NotNull Double workloadMultiplier,
        @NotNull Boolean isRecurring,
        SpecialDateType type
) {}