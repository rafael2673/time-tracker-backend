package com.ap101gamestudio.timetracker.repository;

import com.ap101gamestudio.timetracker.model.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {
}