package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotNull UserRole role,
        @NotNull UUID workPolicyId
) {}