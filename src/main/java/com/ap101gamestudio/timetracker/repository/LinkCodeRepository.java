package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.LinkCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LinkCodeRepository extends JpaRepository<LinkCode, Long> {
    Optional<LinkCode> findByCode(String code);
}