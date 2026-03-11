package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {
    List<WorkspaceMembership> findByUserId(UUID userId);
    Optional<WorkspaceMembership> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);
}