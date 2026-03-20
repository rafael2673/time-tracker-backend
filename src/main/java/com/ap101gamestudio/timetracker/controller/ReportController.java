package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/timesheet")
    public ResponseEntity<byte[]> downloadTimesheet(
            Authentication authentication,
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @RequestParam UUID userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        byte[] report = reportService.generateMonthlyTimesheet(authentication.getName(), userId, workspaceId, year, month);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "timesheet.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(report);
    }
}