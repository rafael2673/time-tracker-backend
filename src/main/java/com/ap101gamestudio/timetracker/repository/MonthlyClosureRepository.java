package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.MonthlyClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlyClosureRepository extends JpaRepository<MonthlyClosure, UUID> {
    Optional<MonthlyClosure> findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(UUID workspaceId, UUID userId, int year, int month);
    boolean existsByWorkspaceIdAndReferenceYearAndReferenceMonth(UUID workspaceId, int year, int month);
    List<MonthlyClosure> findByWorkspaceIdAndReferenceYearAndReferenceMonth(UUID workspaceId, int year, int month);
}