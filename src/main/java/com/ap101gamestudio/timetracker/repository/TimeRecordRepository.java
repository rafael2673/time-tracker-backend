package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.TimeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<TimeRecord> findByWorkspaceIdAndPendingApprovationIsTrue(UUID workspaceId, Pageable pageable);
    @Query("SELECT r FROM TimeRecord r WHERE r.workspace.id = :workspaceId AND r.pendingApprovation = true AND " +
            "(:search IS NULL OR :search = '' OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.justification) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TimeRecord> findPendingWithSearch(@Param("workspaceId") UUID workspaceId, @Param("search") String search, Pageable pageable);
    @Query("SELECT r FROM TimeRecord r WHERE r.workspace.id = :workspaceId AND r.pendingApprovation = false AND r.justification IS NOT NULL AND " +
            "(:search IS NULL OR :search = '' OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.justification) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:searchDay IS NULL OR EXTRACT(DAY FROM r.registeredAt) = :searchDay) AND " +
            "(:searchMonth IS NULL OR EXTRACT(MONTH FROM r.registeredAt) = :searchMonth) AND " +
            "(:searchYear IS NULL OR EXTRACT(YEAR FROM r.registeredAt) = :searchYear)")
    Page<TimeRecord> findHistoryWithFilters(
            @Param("workspaceId") UUID workspaceId,
            @Param("search") String search,
            @Param("searchDay") Integer searchDay,
            @Param("searchMonth") Integer searchMonth,
            @Param("searchYear") Integer searchYear,
            Pageable pageable);

}
