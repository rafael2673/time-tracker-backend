package com.ap101gamestudio.timetracker.model;

import com.ap101gamestudio.timetracker.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "workspace_memberships")
public class WorkspaceMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_policy_id", nullable = false)
    private WorkPolicy workPolicy;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_system_admin", nullable = false)
    private final boolean systemAdmin = false;

    protected WorkspaceMembership() {}

    public WorkspaceMembership(User user, Workspace workspace, UserRole role, WorkPolicy workPolicy) {
        this.user = user;
        this.workspace = workspace;
        this.role = role;
        this.workPolicy = workPolicy;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}