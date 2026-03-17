package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.RecordType;
import java.time.LocalDateTime;
import java.util.UUID;

public record PendingRecordResponse(
        UUID id,
        String employeeName,
        RecordType recordType,
        LocalDateTime registeredAt,
        String justification
) {}