package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.EmployeeLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, UUID> {
    List<EmployeeLeave> findByWorkspaceIdAndUserIdOrderByStartDateDesc(UUID workspaceId, UUID userId);

    @Query("SELECT e FROM EmployeeLeave e WHERE e.user.id = :userId AND e.workspace.id = :workspaceId AND e.startDate <= :end AND e.endDate >= :start")
    List<EmployeeLeave> findOverlappingLeaves(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}