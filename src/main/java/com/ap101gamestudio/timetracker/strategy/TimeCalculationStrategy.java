package com.ap101gamestudio.timetracker.strategy;

import com.ap101gamestudio.timetracker.model.TimeRecord;
import com.ap101gamestudio.timetracker.model.WorkPolicy;

import java.time.Duration;
import java.util.List;

public interface TimeCalculationStrategy {
    Duration calculateOvertime(List<TimeRecord> dailyRecords, WorkPolicy policy);
    Duration calculateRegularHours(List<TimeRecord> dailyRecords, WorkPolicy policy);
}
