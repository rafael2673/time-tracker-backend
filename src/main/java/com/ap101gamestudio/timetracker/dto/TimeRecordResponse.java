package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.model.enums.RecordType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimeRecordResponse(
        UUID id,
        String userName,
        String workspaceName,
        RecordType recordType,
        RecordSource source,
        LocalDateTime registeredAt,
        String justification
) {
}
