package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.WidgetLoginRequest;
import com.ap101gamestudio.timetracker.dto.WidgetLoginResponse;
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
    public ResponseEntity<WidgetLoginResponse> widgetLogin(@RequestBody @Valid WidgetLoginRequest request) {
        WidgetLoginResponse response = widgetAuthService.authenticateFromWidget(request);
        return ResponseEntity.ok(response);
    }
}