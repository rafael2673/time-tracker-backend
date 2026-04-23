package com.ap101gamestudio.timetracker.dto;

public record CompanyBalanceSummaryResponse(
        double totalLaborRisk,
        double totalDeficit,
        long pendingActions
) {
}
