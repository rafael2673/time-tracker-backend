package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "monthly_closures")
public class MonthlyClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reference_year", nullable = false)
    private int referenceYear;

    @Column(name = "reference_month", nullable = false)
    private int referenceMonth;

    @Column(name = "worked_hours", nullable = false)
    private double workedHours;

    @Column(name = "expected_hours", nullable = false)
    private double expectedHours;

    @Column(name = "raw_balance", nullable = false)
    private double rawBalance;

    @Column(name = "paid_overtime_hours", nullable = false)
    private double paidOvertimeHours;

    @Column(name = "banked_hours_delta", nullable = false)
    private double bankedHoursDelta;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    protected MonthlyClosure() {}

    public MonthlyClosure(Workspace workspace, User user, int referenceYear, int referenceMonth, double workedHours, double expectedHours, double rawBalance, double paidOvertimeHours, double bankedHoursDelta) {
        this.workspace = workspace;
        this.user = user;
        this.referenceYear = referenceYear;
        this.referenceMonth = referenceMonth;
        this.workedHours = workedHours;
        this.expectedHours = expectedHours;
        this.rawBalance = rawBalance;
        this.paidOvertimeHours = paidOvertimeHours;
        this.bankedHoursDelta = bankedHoursDelta;
        this.closedAt = LocalDateTime.now();
    }
}