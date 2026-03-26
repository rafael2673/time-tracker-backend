package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.annotation.CurrentWorkspaceId;
import com.ap101gamestudio.timetracker.dto.GenerateLinkCodeResponse;
import com.ap101gamestudio.timetracker.dto.SetPasswordRequest;
import com.ap101gamestudio.timetracker.dto.UpdateProfileRequest;
import com.ap101gamestudio.timetracker.dto.UserProfileResponse;
import com.ap101gamestudio.timetracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            Authentication authentication,
            @CurrentWorkspaceId UUID workspaceId
    ) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(authentication.getName(), workspaceId));
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            Authentication authentication,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            Authentication authentication,
            @RequestBody @Valid SetPasswordRequest request
    ) {
        userService.updatePassword(authentication.getName(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{employeeId}/generate-access-code")
    public ResponseEntity<GenerateLinkCodeResponse> generateAccessCodeForEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId,
            @CurrentWorkspaceId UUID workspaceId
    ) {
        return ResponseEntity.ok(userService.generateCodeForEmployee(authentication.getName(), employeeId, workspaceId));
    }
}