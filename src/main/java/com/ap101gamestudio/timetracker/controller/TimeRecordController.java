package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.dto.UpdateTimeRecordRequest;
import com.ap101gamestudio.timetracker.security.JwtService;
import com.ap101gamestudio.timetracker.service.TimeTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/records")
public class TimeRecordController {

    private final TimeTrackingService timeTrackingService;
    private final JwtService jwtService;

    public TimeRecordController(TimeTrackingService timeTrackingService, JwtService jwtService) {
        this.timeTrackingService = timeTrackingService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<TimeRecordResponse> createRecord(
            @RequestBody @Valid CreateTimeRecordRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication
    ) {
        String email = authentication.getName();
        String token = authHeader.substring(7);
        UUID workspaceId = UUID.fromString(jwtService.extractWorkspaceId(token));

        TimeRecordResponse response = timeTrackingService.registerPoint(email, request, workspaceId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeRecordResponse> updateRecord(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateTimeRecordRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication
    ) {
        String email = authentication.getName();
        String token = authHeader.substring(7);
        UUID workspaceId = UUID.fromString(jwtService.extractWorkspaceId(token));

        TimeRecordResponse response = timeTrackingService.updateRecord(id, email, request, workspaceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TimeRecordResponse>> getRecordsByDate(
            @RequestParam(required = false) LocalDate date,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication
    ) {
        String email = authentication.getName();
        String token = authHeader.substring(7);
        UUID workspaceId = UUID.fromString(jwtService.extractWorkspaceId(token));
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        List<TimeRecordResponse> response = timeTrackingService.getRecordsByDate(email, targetDate, workspaceId);
        return ResponseEntity.ok(response);
    }
}