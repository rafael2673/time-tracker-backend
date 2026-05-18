package com.ap101gamestudio.timetracker.dto;

public record SaasMetricsResponse(
    long totalWorkspaces,
    long totalActiveUsers,
    long totalRecordsLast24h,
    long workspacesLast30Days,
    long usersLast30Days
) {}