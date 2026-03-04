package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.TokenResponse;
import com.ap101gamestudio.timetracker.dto.WidgetLoginRequest;
import com.ap101gamestudio.timetracker.service.WidgetAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class WidgetAuthController {

    private final WidgetAuthService widgetAuthService;

    @PostMapping("/widget-login")
    public ResponseEntity<TokenResponse> widgetLogin(@RequestBody @Valid WidgetLoginRequest request) {
        String token = widgetAuthService.authenticateFromWidget(request);
        return ResponseEntity.ok(new TokenResponse(token));
    }
}