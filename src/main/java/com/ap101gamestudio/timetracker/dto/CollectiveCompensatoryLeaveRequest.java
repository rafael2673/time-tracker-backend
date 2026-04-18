package com.ap101gamestudio.timetracker.dto;

import java.time.LocalDate;

public record CollectiveCompensatoryLeaveRequest(
        LocalDate date,
        String reason
) {}
