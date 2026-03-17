package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.MonthlyClosureResponse;
import com.ap101gamestudio.timetracker.service.TimesheetClosureService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/closures")
public class TimesheetClosureController {

    private final TimesheetClosureService closureService;

    public TimesheetClosureController(TimesheetClosureService closureService) {
        this.closureService = closureService;
    }

    @PostMapping("/{year}/{month}")
    public ResponseEntity<List<MonthlyClosureResponse>> closeMonth(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable int year,
            @PathVariable int month,
            Authentication authentication
    ) {
        return ResponseEntity.ok(closureService.closeWorkspaceMonth(authentication.getName(), workspaceId, year, month));
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<List<MonthlyClosureResponse>> getClosures(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable int year,
            @PathVariable int month,
            Authentication authentication
    ) {
        return ResponseEntity.ok(closureService.getClosures(authentication.getName(), workspaceId, year, month));
    }
}