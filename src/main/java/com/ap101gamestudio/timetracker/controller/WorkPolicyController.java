package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.WorkPolicyRequest;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.service.WorkPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-policies")
public class WorkPolicyController {

    private final WorkPolicyService service;

    public WorkPolicyController(WorkPolicyService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<WorkPolicy>> listAll(
            Authentication authentication,
            @RequestHeader("X-Workspace-Id") UUID workspaceId
    ) {
        return ResponseEntity.ok(service.listPolicies(authentication.getName(), workspaceId));
    }

    @PostMapping
    public ResponseEntity<WorkPolicy> create(
            Authentication authentication,
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @RequestBody @Valid WorkPolicyRequest request
    ) {
        WorkPolicy saved = service.createPolicy(authentication.getName(), workspaceId, request);
        return ResponseEntity.created(URI.create("/api/v1/work-policies/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkPolicy> update(
            Authentication authentication,
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable UUID id,
            @RequestBody @Valid WorkPolicyRequest request
    ) {
        return ResponseEntity.ok(service.updatePolicy(authentication.getName(), workspaceId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable UUID id
    ) {
        service.deletePolicy(authentication.getName(), workspaceId, id);
        return ResponseEntity.noContent().build();
    }
}