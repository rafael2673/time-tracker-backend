package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.annotation.CurrentWorkspaceId;
import com.ap101gamestudio.timetracker.dto.VacationRightResponse;
import com.ap101gamestudio.timetracker.service.HrRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr-rules")
@RequiredArgsConstructor
public class HrRuleController {

    private final HrRuleService hrRuleService;

    @GetMapping("/vacations")
    public ResponseEntity<VacationRightResponse> getVacationRights(
            @CurrentWorkspaceId UUID workspaceId,
            @RequestParam UUID userId,
            @RequestParam int year
    ) {
        VacationRightResponse response = hrRuleService.calculateVacationRights(workspaceId, userId, year);
        return ResponseEntity.ok(response);
    }
}