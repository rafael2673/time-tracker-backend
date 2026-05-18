package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.CreateWorkspaceRequest;
import com.ap101gamestudio.timetracker.dto.SaasMetricsResponse;
import com.ap101gamestudio.timetracker.dto.WorkspaceSummaryResponse;
import com.ap101gamestudio.timetracker.service.SystemAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/system-admin")
public class SystemAdminController {

    private final SystemAdminService systemAdminService;

    public SystemAdminController(SystemAdminService systemAdminService) {
        this.systemAdminService = systemAdminService;
    }

    @GetMapping("/workspaces")
    public ResponseEntity<List<WorkspaceSummaryResponse>> listWorkspaces(Authentication authentication) {
        return ResponseEntity.ok(systemAdminService.listAllWorkspaces(authentication.getName()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<SaasMetricsResponse> getMetrics(Authentication authentication) {
        return ResponseEntity.ok(systemAdminService.getMetrics(authentication.getName()));
    }

    @PatchMapping("/workspaces/{id}/status")
    public ResponseEntity<Void> toggleWorkspaceStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam boolean active
    ) {
        systemAdminService.toggleWorkspaceStatus(authentication.getName(), id, active);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/workspaces")
    public ResponseEntity<Void> createWorkspace(
            Authentication authentication,
            @RequestBody @Valid CreateWorkspaceRequest request
    ) {
        systemAdminService.createWorkspaceWithAdmin(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}