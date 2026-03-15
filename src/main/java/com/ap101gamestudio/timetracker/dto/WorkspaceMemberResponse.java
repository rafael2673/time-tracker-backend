package com.ap101gamestudio.timetracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID id,
        String fullName,
        String email,
        String role,
        String workPolicyName,
        UUID workPolicyId,
        LocalDateTime joinedAt,
        boolean active
) {}