package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyAndActiveTrue(String key);
    Optional<ApiKey> findByWorkspaceIdAndActiveTrue(UUID workspaceId);
    List<ApiKey> findAllByWorkspaceIdAndActiveTrue(UUID workspaceId);
}