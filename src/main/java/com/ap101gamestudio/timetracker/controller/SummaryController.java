package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.annotation.CurrentWorkspaceId;
import com.ap101gamestudio.timetracker.dto.DailySummaryResponse;
import com.ap101gamestudio.timetracker.dto.EmployeeDashboardSummary;
import com.ap101gamestudio.timetracker.dto.MonthSummaryResponse;
import com.ap101gamestudio.timetracker.dto.MonthlyBalanceResponse;
import com.ap101gamestudio.timetracker.dto.NextHolidayResponse;
import com.ap101gamestudio.timetracker.dto.AbsencePieChartResponse;
import com.ap101gamestudio.timetracker.dto.LaborRiskRankingResponse;
import com.ap101gamestudio.timetracker.dto.TimeDistributionResponse;
import com.ap101gamestudio.timetracker.service.SummaryService;
import com.ap101gamestudio.timetracker.service.TimeTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summary")
public class SummaryController {

    private final TimeTrackingService timeTrackingService;
    private final SummaryService summaryService;

    public SummaryController(
            TimeTrackingService timeTrackingService,
            SummaryService summaryService
    ) {
        this.timeTrackingService = timeTrackingService;
        this.summaryService = summaryService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<DailySummaryResponse>> getWeeklySummary(
            @RequestParam String date,
            @RequestHeader("Accept-Language") String locale,
            @CurrentWorkspaceId UUID workspaceId,
            Authentication authentication
    ) {
        LocalDate referenceDate = LocalDate.parse(date);
        List<DailySummaryResponse> response = timeTrackingService.getWeeklySummary(authentication.getName(), referenceDate, workspaceId, locale);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAvailableYears(
            @CurrentWorkspaceId UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getAvailableYears(authentication.getName(), workspaceId));
    }

    @GetMapping("/yearly")
    public ResponseEntity<List<MonthSummaryResponse>> getYearlySummary(
            @RequestParam int year,
            @RequestHeader("Accept-Language") String locale,
            @CurrentWorkspaceId UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getYearlySummary(authentication.getName(), year, workspaceId, locale));
    }

    @GetMapping("/monthly-balance")
    public ResponseEntity<MonthlyBalanceResponse> getMonthlyBalance(
            @RequestParam int year,
            @RequestParam int month,
            @CurrentWorkspaceId UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(summaryService.getMonthlyBalance(authentication.getName(), year, month, workspaceId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<EmployeeDashboardSummary> getEmployeeSummary(
            @CurrentWorkspaceId UUID workspaceId,
            @PathVariable UUID employeeId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(summaryService.getEmployeeSummary(authentication.getName(), employeeId, workspaceId));
    }

    @GetMapping("/quarterly-balance")
    public ResponseEntity<MonthlyBalanceResponse> getQuarterlyBalance(
            @RequestParam int year,
            @RequestParam int quarter,
            @CurrentWorkspaceId UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getQuarterlyBalance(authentication.getName(), year, quarter, workspaceId));
    }

    @GetMapping("/next-holiday")
    public ResponseEntity<NextHolidayResponse> getNextHoliday(
            @CurrentWorkspaceId UUID workspaceId,
            @RequestParam(required = false) UUID employeeId,
            org.springframework.security.core.Authentication authentication
    ) {
        return ResponseEntity.ok(summaryService.getNextSpecialDate(workspaceId, employeeId, authentication.getName()));
    }

    @GetMapping("/employee/{employeeId}/time-distribution")
    public ResponseEntity<TimeDistributionResponse> getEmployeeTimeDistribution(
            @CurrentWorkspaceId UUID workspaceId,
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication
    ) {
        return ResponseEntity.ok(summaryService.getEmployeeTimeDistribution(authentication.getName(), employeeId, workspaceId, year, month));
    }

    @GetMapping("/company/absences")
    public ResponseEntity<AbsencePieChartResponse> getCompanyAbsences(
            @CurrentWorkspaceId UUID workspaceId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(summaryService.getCompanyAbsences(workspaceId, year, month));
    }

    @GetMapping("/company/yearly")
    public ResponseEntity<List<MonthSummaryResponse>> getCompanyYearlyAverage(
            @CurrentWorkspaceId UUID workspaceId,
            @RequestParam int year,
            @RequestParam(required = false) UUID policyId,
            @RequestHeader("Accept-Language") String locale
    ) {
        return ResponseEntity.ok(summaryService.getCompanyYearlyAverage(workspaceId, year, policyId, locale));
    }

    @GetMapping("/company/labor-risk")
    public ResponseEntity<LaborRiskRankingResponse> getLaborRiskRanking(
            @CurrentWorkspaceId UUID workspaceId
    ) {
        return ResponseEntity.ok(summaryService.getLaborRiskRanking(workspaceId));
    }

    @GetMapping("/company/time-distribution")
    public ResponseEntity<TimeDistributionResponse> getTimeDistribution(
            @CurrentWorkspaceId UUID workspaceId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(summaryService.getTimeDistribution(workspaceId, year, month));
    }
}