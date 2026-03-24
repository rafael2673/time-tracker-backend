package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.annotation.CurrentWorkspaceId;
import com.ap101gamestudio.timetracker.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings/api-key")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getActiveKey(
            Authentication authentication,
            @CurrentWorkspaceId UUID workspaceId
    ) {
        String key = apiKeyService.getActiveKey(authentication.getName(), workspaceId);
        return ResponseEntity.ok(Map.of("apiKey", key != null ? key : ""));
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateNewKey(
            Authentication authentication,
            @CurrentWorkspaceId UUID workspaceId
    ) {
        String newKey = apiKeyService.generateNewKey(authentication.getName(), workspaceId);
        return ResponseEntity.ok(Map.of("apiKey", newKey));
    }
}