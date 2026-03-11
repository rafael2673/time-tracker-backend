package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "work_policies")
public class WorkPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "daily_minutes_limit", nullable = false)
    private Integer dailyMinutesLimit;

    @Column(name = "tolerance_minutes", nullable = false)
    private Integer toleranceMinutes;

    @Column(name = "working_days", nullable = false)
    private String workingDays;

    protected WorkPolicy() {
    }

    public WorkPolicy(String name, Integer dailyMinutesLimit, Integer toleranceMinutes) {
        this(name, dailyMinutesLimit, toleranceMinutes, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
    }

    public WorkPolicy(String name, Integer dailyMinutesLimit, Integer toleranceMinutes, String workingDays) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (dailyMinutesLimit == null || dailyMinutesLimit <= 0) {
            throw new IllegalArgumentException("Daily minutes limit must be positive");
        }
        if (toleranceMinutes == null || toleranceMinutes < 0) {
            throw new IllegalArgumentException("Tolerance minutes cannot be negative");
        }
        this.name = name;
        this.dailyMinutesLimit = dailyMinutesLimit;
        this.toleranceMinutes = toleranceMinutes;
        this.workingDays = workingDays != null && !workingDays.isBlank() ? workingDays : "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
    }

    public List<DayOfWeek> getWorkingDaysList() {
        return Arrays.stream(workingDays.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .toList();
    }
}