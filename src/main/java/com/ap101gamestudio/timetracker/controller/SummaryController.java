package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.DailySummaryResponse;
import com.ap101gamestudio.timetracker.security.JwtService;
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
    private final JwtService jwtService;

    public SummaryController(TimeTrackingService timeTrackingService, JwtService jwtService) {
        this.timeTrackingService = timeTrackingService;
        this.jwtService = jwtService;
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<DailySummaryResponse>> getWeeklySummary(
            @RequestParam String date,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication
    ) {
        String email = authentication.getName();
        String token = authHeader.substring(7);
        UUID workspaceId = UUID.fromString(jwtService.extractWorkspaceId(token));
        LocalDate referenceDate = LocalDate.parse(date);

        List<DailySummaryResponse> response = timeTrackingService.getWeeklySummary(email, referenceDate, workspaceId);
        return ResponseEntity.ok(response);
    }
}