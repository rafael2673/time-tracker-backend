package com.ap101gamestudio.timetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_holiday_syncs")
@Getter
@Setter
public class WorkspaceHolidaySync {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    public WorkspaceHolidaySync() {}

    public WorkspaceHolidaySync(Workspace workspace, Integer year) {
        this.workspace = workspace;
        this.year = year;
        this.syncedAt = LocalDateTime.now();
    }
}