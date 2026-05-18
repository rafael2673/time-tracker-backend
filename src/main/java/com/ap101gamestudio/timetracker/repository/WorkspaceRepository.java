package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    List<Workspace> findAllByAutoClosureEnabledTrue();
    long countByCreatedAtAfter(LocalDateTime date);
}
