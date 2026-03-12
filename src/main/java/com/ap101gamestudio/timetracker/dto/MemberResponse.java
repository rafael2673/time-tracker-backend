package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.UserRole;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role,
        String workPolicyName
) {}