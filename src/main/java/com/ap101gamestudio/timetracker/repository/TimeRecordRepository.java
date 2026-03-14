package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.TimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeRecordRepository extends JpaRepository<TimeRecord, UUID> {
    List<TimeRecord> findByUserIdAndWorkspaceIdAndRegisteredAtBetween(UUID userId, UUID workspaceId, LocalDateTime start, LocalDateTime end);
    @Query("SELECT DISTINCT YEAR(t.registeredAt) FROM TimeRecord t WHERE t.user.id = :userId AND t.workspace.id = :workspaceId ORDER BY YEAR(t.registeredAt) DESC")
    List<Integer> findAvailableYears(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId);
    long countByUserIdAndWorkspaceIdAndPendingApprovationIsTrue(UUID userId, UUID workspaceId);
}
