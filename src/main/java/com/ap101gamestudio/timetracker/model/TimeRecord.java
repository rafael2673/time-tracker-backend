package com.ap101gamestudio.timetracker.model;

import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.model.enums.RecordType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "time_records")
public class TimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false)
    private RecordType recordType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordSource source;

    @Setter
    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String justification;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_from_id")
    private TimeRecord editedFrom;

    @Getter
    @Setter
    @Column(name = "pending_approvation", nullable = false)
    private boolean pendingApprovation = false;

    protected TimeRecord() {
    }

    public TimeRecord(User user, Workspace workspace, RecordType recordType, RecordSource source, LocalDateTime registeredAt, String justification, TimeRecord editedFrom) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (recordType == null) {
            throw new IllegalArgumentException("Record type cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (registeredAt == null) {
            throw new IllegalArgumentException("Registration time cannot be null");
        }
        this.user = user;
        this.workspace = workspace;
        this.recordType = recordType;
        this.source = source;
        this.registeredAt = registeredAt;
        this.justification = justification;
        this.editedFrom = editedFrom;
    }

}
