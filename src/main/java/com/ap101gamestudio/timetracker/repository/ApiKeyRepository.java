package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyAndActiveTrue(String key);
}