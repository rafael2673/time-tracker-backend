package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.dto.PageResponse;
import com.ap101gamestudio.timetracker.dto.SpecialDateRequest;
import com.ap101gamestudio.timetracker.dto.SpecialDateResponse;
import com.ap101gamestudio.timetracker.service.SpecialDateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/special-dates")
public class SpecialDateController {

    private final SpecialDateService specialDateService;

    public SpecialDateController(SpecialDateService specialDateService) {
        this.specialDateService = specialDateService;
    }

    @PostMapping
    public ResponseEntity<SpecialDateResponse> create(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @RequestBody @Valid SpecialDateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(specialDateService.create(authentication.getName(), workspaceId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpecialDateResponse> update(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable UUID id,
            @RequestBody @Valid SpecialDateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(specialDateService.update(authentication.getName(), workspaceId, id, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SpecialDateResponse>> getByYear(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @RequestParam int year,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String exactDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(specialDateService.getByYear(workspaceId, year, search, exactDate, page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @PathVariable UUID id,
            Authentication authentication
    ) {
        specialDateService.delete(authentication.getName(), workspaceId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-national")
    public ResponseEntity<Void> importNationalHolidays(
            @RequestHeader("X-Workspace-Id") UUID workspaceId,
            @RequestParam int year,
            Authentication authentication
    ) {
        specialDateService.importNationalHolidays(authentication.getName(), workspaceId, year);
        return ResponseEntity.ok().build();
    }
}