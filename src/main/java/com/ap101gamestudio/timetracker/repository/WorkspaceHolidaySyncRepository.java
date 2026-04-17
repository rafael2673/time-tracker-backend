package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.WorkspaceHolidaySync;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceHolidaySyncRepository extends JpaRepository<WorkspaceHolidaySync, UUID> {
    Optional<WorkspaceHolidaySync> findByWorkspaceIdAndYear(UUID workspaceId, Integer year);
}