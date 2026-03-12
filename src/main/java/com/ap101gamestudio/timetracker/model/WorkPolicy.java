package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "work_policies")
@Getter
public class WorkPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(name = "daily_minutes_limit", nullable = false)
    private int dailyMinutesLimit;

    @Setter
    @Column(name = "tolerance_minutes", nullable = false)
    private int toleranceMinutes;

    @Setter
    @Column(name = "working_days", nullable = false)
    private String workingDays;

    protected WorkPolicy() {}

    public WorkPolicy(Workspace workspace, String name, int dailyMinutesLimit, int toleranceMinutes, String workingDays) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Policy name cannot be blank");
        }
        this.workspace = workspace;
        this.name = name;
        this.dailyMinutesLimit = dailyMinutesLimit;
        this.toleranceMinutes = toleranceMinutes;
        this.workingDays = workingDays;
    }

    public List<DayOfWeek> getWorkingDaysList() {
        if (workingDays == null || workingDays.isBlank()) {
            return List.of();
        }
        return Arrays.stream(workingDays.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toList());
    }
}