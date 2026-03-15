package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {
    List<WorkspaceMembership> findByUserId(UUID userId);
    Optional<WorkspaceMembership> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);
    List<WorkspaceMembership> findAllByUserId(UUID userId);
    List<WorkspaceMembership> findAllByWorkspaceId(UUID workspaceId);
    @Query("SELECT wm FROM WorkspaceMembership wm WHERE wm.workspace.id = :workspaceId AND " +
            "(:search IS NULL OR :search = '' OR LOWER(wm.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(wm.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkspaceMembership> findByWorkspaceIdWithSearch(@Param("workspaceId") UUID workspaceId, @Param("search") String search, Pageable pageable);
}