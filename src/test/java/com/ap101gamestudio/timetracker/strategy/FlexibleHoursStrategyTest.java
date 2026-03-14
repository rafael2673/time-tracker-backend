package com.ap101gamestudio.timetracker.strategy;

import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.model.enums.RecordType;
import com.ap101gamestudio.timetracker.model.TimeRecord;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

class FlexibleHoursStrategyTest {

    private final FlexibleHoursStrategy strategy = new FlexibleHoursStrategy();
    private Workspace createWorkspace() {
        return new Workspace(
                "Office",
                -5.7945,
                -35.2110,
                100
        );
    }
    @Test
    void shouldReturnZeroOvertimeWhenHoursAreWithinTolerance() {
        Workspace workspace = createWorkspace();
        WorkPolicy policy = new WorkPolicy(workspace,"Standard", 480, 10, "MONDAY,TUESDAY");
        User user = new User("test@test.com", "hash", "Test User");
        LocalDateTime date = LocalDateTime.of(2026, 2, 23, 0, 0);

        List<TimeRecord> records = List.of(
                new TimeRecord(user, null, RecordType.ENTRY, RecordSource.MANUAL, date.withHour(8).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.PAUSE_START, RecordSource.MANUAL, date.withHour(12).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.PAUSE_END, RecordSource.MANUAL, date.withHour(13).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.EXIT, RecordSource.MANUAL, date.withHour(17).withMinute(5), null, null)
        );

        Duration overtime = strategy.calculateOvertime(records, policy);

        Assertions.assertEquals(0, overtime.toMinutes());
    }

    @Test
    void shouldReturnPositiveOvertimeWhenHoursExceedTolerance() {
        Workspace workspace = createWorkspace();
        WorkPolicy policy = new WorkPolicy(workspace,"Standard", 480, 10, "MONDAY,TUESDAY");
        User user = new User("test@test.com", "hash", "Test User");
        LocalDateTime date = LocalDateTime.of(2026, 2, 23, 0, 0);

        List<TimeRecord> records = List.of(
                new TimeRecord(user, null, RecordType.ENTRY, RecordSource.MANUAL, date.withHour(8).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.PAUSE_START, RecordSource.MANUAL, date.withHour(12).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.PAUSE_END, RecordSource.MANUAL, date.withHour(13).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.EXIT, RecordSource.MANUAL, date.withHour(18).withMinute(0), null, null)
        );

        Duration overtime = strategy.calculateOvertime(records, policy);

        Assertions.assertEquals(60, overtime.toMinutes());
    }

    @Test
    void shouldIgnoreIncompletePairs() {
        Workspace workspace = createWorkspace();
        WorkPolicy policy = new WorkPolicy(workspace,"Standard", 480, 10, "MONDAY,TUESDAY");
        User user = new User("test@test.com", "hash", "Test User");
        LocalDateTime date = LocalDateTime.of(2026, 2, 23, 0, 0);

        List<TimeRecord> records = List.of(
                new TimeRecord(user, null, RecordType.ENTRY, RecordSource.MANUAL, date.withHour(8).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.PAUSE_START, RecordSource.MANUAL, date.withHour(12).withMinute(0), null, null),
                new TimeRecord(user, null, RecordType.PAUSE_END, RecordSource.MANUAL, date.withHour(13).withMinute(0), null, null)
        );

        Duration regularHours = strategy.calculateRegularHours(records, policy);

        Assertions.assertEquals(240, regularHours.toMinutes());
    }
}