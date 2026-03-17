package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.*;
import com.ap101gamestudio.timetracker.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/my")
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(Authentication authentication) {
        return ResponseEntity.ok(workspaceService.getUserWorkspaces(authentication.getName()));
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<PageResponse<WorkspaceMemberResponse>> getWorkspaceMembers(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(workspaceService.getWorkspaceMembers(workspaceId, search, role, page, size));
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<MemberResponse> addMember(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestBody @Valid AddMemberRequest request
    ) {
        return ResponseEntity.ok(workspaceService.addMember(authentication.getName(), workspaceId, request));
    }

    @PutMapping("/{workspaceId}/members/{employeeId}")
    public ResponseEntity<Void> updateMember(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateMemberRequest request
    ) {
        workspaceService.updateMember(authentication.getName(), workspaceId, employeeId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workspaceId}/members/{employeeId}/status")
    public ResponseEntity<Void> changeMemberStatus(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID employeeId, @RequestParam boolean active) {
        workspaceService.changeMemberStatus(authentication.getName(), workspaceId, employeeId, active);
        return ResponseEntity.noContent().build();
    }
}