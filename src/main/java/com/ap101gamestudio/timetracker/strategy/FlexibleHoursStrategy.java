package com.ap101gamestudio.timetracker.strategy;


import com.ap101gamestudio.timetracker.model.enums.RecordType;
import com.ap101gamestudio.timetracker.model.TimeRecord;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Service
@Primary
public class FlexibleHoursStrategy implements TimeCalculationStrategy {

    @Override
    public Duration calculateOvertime(List<TimeRecord> dailyRecords, WorkPolicy policy) {
        Duration totalWorked = calculateTotalWorkedTime(dailyRecords);
        Duration expectedWork = Duration.ofMinutes(policy.getDailyMinutesLimit());
        Duration tolerance = Duration.ofMinutes(policy.getToleranceMinutes());

        Duration difference = totalWorked.minus(expectedWork);

        if (Math.abs(difference.toMinutes()) <= tolerance.toMinutes()) {
            return Duration.ZERO;
        }

        return difference.isNegative() ? Duration.ZERO : difference;
    }

    @Override
    public Duration calculateRegularHours(List<TimeRecord> dailyRecords, WorkPolicy policy) {
        Duration totalWorked = calculateTotalWorkedTime(dailyRecords);
        Duration expectedWork = Duration.ofMinutes(policy.getDailyMinutesLimit());

        if (totalWorked.compareTo(expectedWork) >= 0) {
            return expectedWork;
        }
        return totalWorked;
    }

    private Duration calculateTotalWorkedTime(List<TimeRecord> records) {
        if (records == null || records.isEmpty()) {
            return Duration.ZERO;
        }

        List<TimeRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(TimeRecord::getRegisteredAt))
                .toList();

        Duration totalDuration = Duration.ZERO;
        TimeRecord lastEntry = null;

        for (TimeRecord record : sortedRecords) {
            if (record.getRecordType() == RecordType.ENTRY || record.getRecordType() == RecordType.PAUSE_END) {
                lastEntry = record;
            } else if ((record.getRecordType() == RecordType.EXIT || record.getRecordType() == RecordType.PAUSE_START) && lastEntry != null) {
                totalDuration = totalDuration.plus(Duration.between(lastEntry.getRegisteredAt(), record.getRegisteredAt()));
                lastEntry = null;
            }
        }

        return totalDuration;
    }
}