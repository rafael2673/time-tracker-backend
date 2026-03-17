package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.*;
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

    @GetMapping("/daily")
    public ResponseEntity<List<TimeRecordResponse>> getDailyRecords(
            @RequestParam UUID userId,
            @RequestParam LocalDate date,
            @RequestHeader("X-Workspace-Id") UUID workspaceId
    ) {
        List<TimeRecordResponse> response = timeTrackingService.getRecordsByUserIdAndDate(userId, date, workspaceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<com.ap101gamestudio.timetracker.dto.PageResponse<com.ap101gamestudio.timetracker.dto.PendingRecordResponse>> getPendingRecords(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getPendingRecords(authentication.getName(), workspaceId, search, page, size));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveRecord(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable UUID id,
            Authentication authentication
    ) {
        timeTrackingService.approveRecord(authentication.getName(), workspaceId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRecord(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable UUID id,
            Authentication authentication
    ) {
        timeTrackingService.rejectRecord(authentication.getName(), workspaceId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<PendingRecordResponse>> getApprovalHistory(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(timeTrackingService.getApprovalHistory(authentication.getName(), workspaceId, search, date, page, size));
    }
}