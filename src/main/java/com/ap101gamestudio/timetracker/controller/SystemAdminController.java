package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.CreateWorkspaceRequest;
import com.ap101gamestudio.timetracker.service.SystemAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system-admin")
public class SystemAdminController {

    private final SystemAdminService systemAdminService;

    public SystemAdminController(SystemAdminService systemAdminService) {
        this.systemAdminService = systemAdminService;
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