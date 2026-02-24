package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.model.enums.RecordType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateTimeRecordRequest(
        @NotNull(message = "Record type is required")
        RecordType recordType,

        @NotNull(message = "Source is required")
        RecordSource source,

        @NotNull(message = "Timestamp is required")
        LocalDateTime registeredAt,

        Double latitude,
        Double longitude
) {
}
