package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        String email
) {
}
