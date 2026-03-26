package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.ExportJobResponse;
import java.util.UUID;

public interface ReportQueueService {
    ExportJobResponse enqueueExportJob(String requesterEmail, UUID workspaceId, int year, int month, String locale);
    ExportJobResponse getJobStatus(UUID jobId, UUID workspaceId);
    String getJobFilePath(UUID jobId, UUID workspaceId);
}