package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.ExportJobResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.ExportJob;
import com.ap101gamestudio.timetracker.model.enums.ExportJobStatus;
import com.ap101gamestudio.timetracker.repository.ExportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncReportQueueServiceImpl implements ReportQueueService {

    private final ExportJobRepository jobRepository;
    private final ReportWorkerService workerService;

    @Override
    public ExportJobResponse enqueueExportJob(String requesterEmail, UUID workspaceId, int year, int month, String locale) {
        ExportJob job = new ExportJob();
        job.setWorkspaceId(workspaceId);
        job.setRequesterEmail(requesterEmail);
        ExportJob savedJob = jobRepository.save(job);

        executeAsyncWorker(savedJob, year, month, locale);

        return new ExportJobResponse(savedJob.getId(), savedJob.getStatus(), savedJob.getProgress());
    }

    @Async
    protected void executeAsyncWorker(ExportJob job, int year, int month, String locale) {
        workerService.processBulkExport(job, year, month, locale);
    }

    @Override
    public ExportJobResponse getJobStatus(UUID jobId, UUID workspaceId) {
        ExportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new DomainException("error.job.not_found"));

        if (!job.getWorkspaceId().equals(workspaceId)) {
            throw new DomainException("error.permission.denied");
        }

        return new ExportJobResponse(job.getId(), job.getStatus(), job.getProgress());
    }

    @Override
    public String getJobFilePath(UUID jobId, UUID workspaceId) {
        ExportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new DomainException("error.job.not_found"));

        if (!job.getWorkspaceId().equals(workspaceId)) {
            throw new DomainException("error.permission.denied");
        }

        if (job.getStatus() != ExportJobStatus.COMPLETED || job.getFilePath() == null) {
            throw new DomainException("error.job.not_ready");
        }

        return job.getFilePath();
    }
}