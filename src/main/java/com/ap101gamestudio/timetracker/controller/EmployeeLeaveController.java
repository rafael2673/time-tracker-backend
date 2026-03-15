package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.EmployeeLeaveRequest;
import com.ap101gamestudio.timetracker.dto.EmployeeLeaveResponse;
import com.ap101gamestudio.timetracker.service.EmployeeLeaveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/leaves")
public class EmployeeLeaveController {

    private final EmployeeLeaveService leaveService;

    public EmployeeLeaveController(EmployeeLeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/{employeeId}")
    public ResponseEntity<EmployeeLeaveResponse> create(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID employeeId, @RequestBody @Valid EmployeeLeaveRequest request) {
        return ResponseEntity.ok(leaveService.create(authentication.getName(), workspaceId, employeeId, request));
    }
    @PutMapping("/{leaveId}")
    public ResponseEntity<EmployeeLeaveResponse> update(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID leaveId,
            @RequestBody @Valid EmployeeLeaveRequest request
    ) {
        return ResponseEntity.ok(leaveService.update(authentication.getName(), workspaceId, leaveId, request));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<List<EmployeeLeaveResponse>> getByEmployee(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID employeeId) {
        return ResponseEntity.ok(leaveService.getByEmployee(authentication.getName(), workspaceId, employeeId));
    }

    @DeleteMapping("/{leaveId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID leaveId) {
        leaveService.delete(authentication.getName(), workspaceId, leaveId);
        return ResponseEntity.noContent().build();
    }
}