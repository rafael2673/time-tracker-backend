package com.ap101gamestudio.timetracker.dto;

import java.util.UUID;

public record WorkspaceSummaryResponse(
    UUID id,
    String name,
    long memberCount,
    boolean active
) {}