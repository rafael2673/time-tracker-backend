package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "special_dates")
public class SpecialDate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String description;

    @Column(name = "workload_multiplier", nullable = false)
    private Double workloadMultiplier;

    @Column(name = "is_recurring", nullable = false)
    private boolean isRecurring;

    public SpecialDate() {
    }

    public SpecialDate(Workspace workspace, LocalDate date, String description, Double workloadMultiplier, boolean isRecurring) {
        this.workspace = workspace;
        this.date = date;
        this.description = description;
        this.workloadMultiplier = workloadMultiplier;
        this.isRecurring = isRecurring;
    }
}