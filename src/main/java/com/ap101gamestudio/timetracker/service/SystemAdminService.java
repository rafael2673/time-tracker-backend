package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CreateWorkspaceRequest;
import com.ap101gamestudio.timetracker.dto.SaasMetricsResponse;
import com.ap101gamestudio.timetracker.dto.WorkspaceSummaryResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.TimeRecordRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SystemAdminService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final TimeRecordRepository timeRecordRepository;

    public SystemAdminService(UserRepository userRepository, WorkspaceRepository workspaceRepository, WorkspaceMembershipRepository membershipRepository, TimeRecordRepository timeRecordRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.timeRecordRepository = timeRecordRepository;
    }

    private void validateSuperAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        if (!user.isSystemAdmin()) {
            throw new DomainException("error.permission.denied");
        }
    }

    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> listAllWorkspaces(String requesterEmail) {
        validateSuperAdmin(requesterEmail);
        return workspaceRepository.findAll().stream()
                .map(ws -> new WorkspaceSummaryResponse(
                        ws.getId(),
                        ws.getName(),
                        membershipRepository.countByWorkspaceId(ws.getId()),
                        ws.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SaasMetricsResponse getMetrics(String requesterEmail) {
        validateSuperAdmin(requesterEmail);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        return new SaasMetricsResponse(
                workspaceRepository.count(),
                userRepository.count(),
                timeRecordRepository.countByRegisteredAtAfter(LocalDateTime.now().minusHours(24)),
                workspaceRepository.countByCreatedAtAfter(thirtyDaysAgo),
                userRepository.countByCreatedAtAfter(thirtyDaysAgo)
        );
    }

    @Transactional
    public void toggleWorkspaceStatus(String requesterEmail, UUID workspaceId, boolean active) {
        validateSuperAdmin(requesterEmail);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new DomainException("error.workspace.not_found"));
        workspace.setActive(active);
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void createWorkspaceWithAdmin(String requesterEmail, CreateWorkspaceRequest request) {
        validateSuperAdmin(requesterEmail);

        Workspace workspace = new Workspace(
            request.workspaceName(),
            0.0,
            0.0,
            1000
        );
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        User companyAdmin = userRepository.findByEmail(request.adminEmail()).orElseGet(() -> 
            new User(
                request.adminEmail(),
                "",
                request.adminName()
            )
        );
        userRepository.save(companyAdmin);

        WorkspaceMembership membership = new WorkspaceMembership(companyAdmin, savedWorkspace, UserRole.ADMIN, null);
        membershipRepository.save(membership);
    }
}