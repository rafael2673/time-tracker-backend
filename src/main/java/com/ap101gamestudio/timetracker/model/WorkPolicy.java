package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import java.util.UUID;

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

    protected WorkPolicy() {
    }

    public WorkPolicy(String name, Integer dailyMinutesLimit, Integer toleranceMinutes) {
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
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Integer getDailyMinutesLimit() { return dailyMinutesLimit; }
    public Integer getToleranceMinutes() { return toleranceMinutes; }
}
