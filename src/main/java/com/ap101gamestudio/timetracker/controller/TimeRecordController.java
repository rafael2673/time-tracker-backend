package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.dto.UpdateTimeRecordRequest;
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

    public TimeRecordController(TimeTrackingService timeTrackingService) {
        this.timeTrackingService = timeTrackingService;
    }

    @PostMapping
    public ResponseEntity<TimeRecordResponse> createRecord(
            @RequestBody @Valid CreateTimeRecordRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        TimeRecordResponse response = timeTrackingService.registerPoint(email, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeRecordResponse> updateRecord(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateTimeRecordRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        TimeRecordResponse response = timeTrackingService.updateRecord(id, email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<List<TimeRecordResponse>> getTodayRecords(Authentication authentication) {
        String email = authentication.getName();
        LocalDate today = LocalDate.now();
        List<TimeRecordResponse> response = timeTrackingService.getRecordsByDate(email, today);
        return ResponseEntity.ok(response);
    }
}