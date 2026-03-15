package com.ap101gamestudio.timetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateMemberRequest(
        @NotBlank String fullName,
        @NotBlank String role,
        @NotNull UUID workPolicyId
) {}