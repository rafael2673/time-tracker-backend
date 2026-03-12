package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.NotBlank;

public record SetPasswordRequest(
        @NotBlank String newPassword
) {}