package com.ap101gamestudio.timetracker.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        String role,
        UUID workspaceId,
        String workspaceName,
        boolean systemAdmin
) {}