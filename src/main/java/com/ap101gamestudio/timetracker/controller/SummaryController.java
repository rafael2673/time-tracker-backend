package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.DailySummaryResponse;
import com.ap101gamestudio.timetracker.service.TimeTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/summary")
public class SummaryController {

    private final TimeTrackingService timeTrackingService;

    public SummaryController(TimeTrackingService timeTrackingService) {
        this.timeTrackingService = timeTrackingService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<DailySummaryResponse>> getWeeklySummary(
            @RequestParam String date,
            Authentication authentication
    ) {
        String email = authentication.getName();
        LocalDate referenceDate = LocalDate.parse(date);
        List<DailySummaryResponse> response = timeTrackingService.getWeeklySummary(email, referenceDate);
        return ResponseEntity.ok(response);
    }
}