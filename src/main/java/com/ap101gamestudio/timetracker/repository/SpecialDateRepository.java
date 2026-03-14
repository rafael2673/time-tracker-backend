package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.SpecialDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpecialDateRepository extends JpaRepository<SpecialDate, UUID> {

    @Query("SELECT s FROM SpecialDate s WHERE s.workspace.id = :workspaceId AND (s.isRecurring = true OR (s.date >= :startDate AND s.date <= :endDate))")
    List<SpecialDate> findRelevantDates(@Param("workspaceId") UUID workspaceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM SpecialDate s WHERE s.workspace.id = :workspaceId AND " +
            "(s.isRecurring = true OR (s.date >= :startDate AND s.date <= :endDate)) AND " +
            "(:search IS NULL OR :search = '' OR LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SpecialDate> findRelevantDatesWithSearch(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );
}