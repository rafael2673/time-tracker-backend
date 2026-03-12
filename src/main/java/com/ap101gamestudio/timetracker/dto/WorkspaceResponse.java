package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.UserRole;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        UserRole role
) {}