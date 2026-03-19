package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CreateWorkspaceRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAdminService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public SystemAdminService(UserRepository userRepository, WorkspaceRepository workspaceRepository, WorkspaceMembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
    }

    private void validateSuperAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        if (!user.isSystemAdmin()) {
            throw new DomainException("error.permission.denied");
        }
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