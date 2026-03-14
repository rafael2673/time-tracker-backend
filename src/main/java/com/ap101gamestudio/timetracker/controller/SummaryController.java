package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.DailySummaryResponse;
import com.ap101gamestudio.timetracker.dto.EmployeeDashboardSummary;
import com.ap101gamestudio.timetracker.dto.MonthSummaryResponse;
import com.ap101gamestudio.timetracker.dto.MonthlyBalanceResponse;
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
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            Authentication authentication
    ) {
        LocalDate referenceDate = LocalDate.parse(date);
        List<DailySummaryResponse> response = timeTrackingService.getWeeklySummary(authentication.getName(), referenceDate, workspaceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAvailableYears(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getAvailableYears(authentication.getName(), workspaceId));
    }

    @GetMapping("/yearly")
    public ResponseEntity<List<MonthSummaryResponse>> getYearlySummary(
            @RequestParam int year,
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getYearlySummary(authentication.getName(), year, workspaceId));
    }

    @GetMapping("/monthly-balance")
    public ResponseEntity<MonthlyBalanceResponse> getMonthlyBalance(
            @RequestParam int year,
            @RequestParam int month,
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getMonthlyBalance(authentication.getName(), year, month, workspaceId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<EmployeeDashboardSummary> getEmployeeSummary(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable UUID employeeId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(summaryService.getEmployeeSummary(authentication.getName(), workspaceId, employeeId));
    }
}