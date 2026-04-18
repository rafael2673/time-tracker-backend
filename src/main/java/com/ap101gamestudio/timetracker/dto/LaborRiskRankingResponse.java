package com.ap101gamestudio.timetracker.dto;

import java.util.List;

public record LaborRiskRankingResponse(
        List<EmployeeBalanceDTO> topPositive,
        List<EmployeeBalanceDTO> topNegative
) {}
