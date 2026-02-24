package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.service.TimeTrackingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/time-records")
public class TimeRecordController {

    private final TimeTrackingService service;

    public TimeRecordController(TimeTrackingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TimeRecordResponse> create(@Valid @RequestBody CreateTimeRecordRequest request) {
        TimeRecordResponse response = service.registerRecord(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<String> getDailyBalance(
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date
    ) {
        Duration balance = service.calculateDailyHours(userId, date);
        long hours = balance.toHours();
        long minutes = balance.toMinutesPart();
        return ResponseEntity.ok(String.format("%d hours and %d minutes overtime", hours, minutes));
    }
}