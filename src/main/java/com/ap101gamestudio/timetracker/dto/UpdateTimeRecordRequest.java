package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateTimeRecordRequest(
        @NotNull(message = "O horário é obrigatório")
        LocalDateTime registeredAt,

        @NotBlank(message = "A justificativa é obrigatória para edições")
        String justification
) {
}
