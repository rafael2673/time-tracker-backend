package com.ap101gamestudio.timetracker.controller;

import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-policies")
public class WorkPolicyController {

    private final WorkPolicyRepository repository;

    public WorkPolicyController(WorkPolicyRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<WorkPolicy>> listAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<WorkPolicy> create(@RequestBody WorkPolicy policy) {
        WorkPolicy saved = repository.save(policy);
        return ResponseEntity.created(URI.create("/api/v1/work-policies/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkPolicy> update(@PathVariable UUID id, @RequestBody WorkPolicy updated) {
        return repository.findById(id)
                .map(existing -> {
                    WorkPolicy newPolicy = new WorkPolicy(
                            updated.getName(),
                            updated.getDailyMinutesLimit(),
                            updated.getToleranceMinutes()
                    );
                    repository.delete(existing);
                    return ResponseEntity.ok(repository.save(newPolicy));
                })
                .orElseThrow(() -> new DomainException("error.policy.not_found"));
    }
}