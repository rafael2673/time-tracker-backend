package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.annotation.CurrentWorkspaceId;
import com.ap101gamestudio.timetracker.dto.BulkExportRequest;
import com.ap101gamestudio.timetracker.dto.ExportJobResponse;
import com.ap101gamestudio.timetracker.service.ReportQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/export")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportQueueService queueService;

    @PostMapping("/bulk")
    public ResponseEntity<ExportJobResponse> startBulkExport(
            Authentication authentication,
            @CurrentWorkspaceId UUID workspaceId,
            @RequestBody BulkExportRequest request
    ) {
        ExportJobResponse response = queueService.enqueueExportJob(
                authentication.getName(),
                workspaceId,
                request.year(),
                request.month(),
                request.locale()
        );
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ExportJobResponse> getJobStatus(
            @CurrentWorkspaceId UUID workspaceId,
            @PathVariable UUID jobId
    ) {
        return ResponseEntity.ok(queueService.getJobStatus(jobId, workspaceId));
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<Resource> downloadExport(
            @CurrentWorkspaceId UUID workspaceId,
            @PathVariable UUID jobId
    ) {
        String filePath = queueService.getJobFilePath(jobId, workspaceId);
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fechamento_lote.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }
}