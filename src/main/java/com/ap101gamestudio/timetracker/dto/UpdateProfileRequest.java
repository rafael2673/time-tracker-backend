package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(
        String fullName,
        @Email String recoveryEmail
) {}