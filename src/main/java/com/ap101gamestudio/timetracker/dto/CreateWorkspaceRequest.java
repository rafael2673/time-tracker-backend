package com.ap101gamestudio.timetracker.dto;
import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
        @NotBlank String workspaceName,
        @NotBlank String adminName,
        @NotBlank String adminEmail
) {}