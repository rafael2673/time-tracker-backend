package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.ExportJobStatus;
import java.util.UUID;

public record ExportJobResponse(
        UUID id,
        ExportJobStatus status,
        int progress
) {}