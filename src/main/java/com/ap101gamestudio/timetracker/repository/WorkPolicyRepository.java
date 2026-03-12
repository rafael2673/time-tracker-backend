package com.ap101gamestudio.timetracker.repository;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkPolicyRepository extends JpaRepository<WorkPolicy, UUID> {
    List<WorkPolicy> findByWorkspaceId(UUID workspaceId);
}