package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.TimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeRecordRepository extends JpaRepository<TimeRecord, UUID> {
    List<TimeRecord> findByUserIdAndWorkspaceIdAndRegisteredAtBetween(UUID userId, UUID workspaceId, LocalDateTime start, LocalDateTime end);
}
