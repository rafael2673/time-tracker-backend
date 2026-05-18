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
    long countByWorkspaceIdAndPendingApprovationIsTrue(UUID workspaceId);

    @Query("SELECT r FROM TimeRecord r WHERE r.workspace.id = :workspaceId AND r.pendingApprovation = true AND " +
            "(:search IS NULL OR :search = '' OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.justification) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TimeRecord> findPendingWithSearch(@Param("workspaceId") UUID workspaceId, @Param("search") String search, Pageable pageable);
    @Query("SELECT t FROM TimeRecord t WHERE t.workspace.id = :workspaceId " +
            "AND t.pendingApprovation = false " +
            "AND t.justification IS NOT NULL AND LENGTH(TRIM(t.justification)) > 0 " +
            "AND (:search IS NULL OR LOWER(t.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:year IS NULL OR YEAR(t.registeredAt) = :year) " +
            "AND (:month IS NULL OR MONTH(t.registeredAt) = :month) " +
            "AND (:day IS NULL OR DAY(t.registeredAt) = :day) " +
            "AND (:status IS NULL OR " +
            "     (:status = 'REJECTED' AND t.rejected = true) OR " +
            "     (:status = 'APPROVED' AND t.rejected = false))")
    Page<TimeRecord> findHistoryWithFilters(
            @Param("workspaceId") UUID workspaceId,
            @Param("search") String search,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("day") Integer day,
            @Param("status") String status,
            Pageable pageable);

    long countByRegisteredAtAfter(LocalDateTime dateTime);
}
