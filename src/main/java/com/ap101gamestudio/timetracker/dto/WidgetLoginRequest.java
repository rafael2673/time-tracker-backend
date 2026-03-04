package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record WidgetLoginRequest(
        @NotBlank(message = "API Key é obrigatória")
        String apiKey,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Formato de email inválido")
        String email,

        String name
) {
}
