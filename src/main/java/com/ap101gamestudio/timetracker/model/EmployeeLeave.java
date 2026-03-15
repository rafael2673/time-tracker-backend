package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "employee_leaves")
public class EmployeeLeave {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String reason;

    protected EmployeeLeave() {}

    public EmployeeLeave(Workspace workspace, User user, LocalDate startDate, LocalDate endDate, String reason) {
        this.workspace = workspace;
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }
}