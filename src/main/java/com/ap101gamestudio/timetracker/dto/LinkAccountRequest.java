package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkAccountRequest(
        @NotBlank
        String code,

        @NotBlank
        @Size(min = 6)
        String newPassword
) {}