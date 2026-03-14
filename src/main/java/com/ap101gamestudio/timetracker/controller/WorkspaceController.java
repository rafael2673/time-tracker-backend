package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.AddMemberRequest;
import com.ap101gamestudio.timetracker.dto.MemberResponse;
import com.ap101gamestudio.timetracker.dto.WorkspaceResponse;
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

    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/my")
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(Authentication authentication) {
        return ResponseEntity.ok(service.getUserWorkspaces(authentication.getName()));
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        return ResponseEntity.ok(service.getWorkspaceMembers(authentication.getName(), workspaceId));
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<MemberResponse> addMember(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestBody @Valid AddMemberRequest request
    ) {
        return ResponseEntity.ok(service.addMember(authentication.getName(), workspaceId, request));
    }
}