package com.ap101gamestudio.timetracker.model;

import com.ap101gamestudio.timetracker.model.enums.OvertimeStrategy;
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
@Setter
public class WorkPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String name;

    @Column(name = "daily_minutes_limit", nullable = false)
    private int dailyMinutesLimit;

    @Column(name = "tolerance_minutes", nullable = false)
    private int toleranceMinutes;

    @Column(name = "working_days", nullable = false)
    private String workingDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "overtime_strategy", nullable = false)
    private OvertimeStrategy overtimeStrategy = OvertimeStrategy.BANK_ONLY;

    @Column(name = "max_bank_hours_per_month", nullable = false)
    private double maxBankHoursPerMonth = 0.0;

    @Column(name = "bank_expiration_months", nullable = false)
    private int bankExpirationMonths = 0;


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