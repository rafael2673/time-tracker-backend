package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.WorkPolicyRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkPolicyService {

    private final WorkPolicyRepository policyRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public WorkPolicyService(
            WorkPolicyRepository policyRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            UserRepository userRepository
    ) {
        this.policyRepository = policyRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public List<WorkPolicy> listPolicies(String authenticatedEmail, UUID workspaceId) {
        validateManagerOrAdmin(authenticatedEmail, workspaceId);
        return policyRepository.findByWorkspaceId(workspaceId);
    }

    @Transactional
    public WorkPolicy createPolicy(String authenticatedEmail, UUID workspaceId, WorkPolicyRequest request) {
        validateAdmin(authenticatedEmail, workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new DomainException("error.workspace.not_found"));

        WorkPolicy policy = new WorkPolicy(
                workspace,
                request.name(),
                request.dailyMinutesLimit(),
                request.toleranceMinutes(),
                request.workingDays()
        );

        return policyRepository.save(policy);
    }

    @Transactional
    public WorkPolicy updatePolicy(String authenticatedEmail, UUID workspaceId, UUID policyId, WorkPolicyRequest request) {
        validateAdmin(authenticatedEmail, workspaceId);

        WorkPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new DomainException("error.policy.not_found"));

        if (!policy.getWorkspace().getId().equals(workspaceId)) {
            throw new DomainException("error.permission.denied");
        }

        policy.setName(request.name());
        policy.setDailyMinutesLimit(request.dailyMinutesLimit());
        policy.setToleranceMinutes(request.toleranceMinutes());
        policy.setWorkingDays(request.workingDays());

        return policyRepository.save(policy);
    }

    private WorkspaceMembership getMembership(String email, UUID workspaceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));
        return membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));
    }

    private void validateManagerOrAdmin(String email, UUID workspaceId) {
        WorkspaceMembership membership = getMembership(email, workspaceId);
        if (membership.getRole() != UserRole.MANAGER && membership.getRole() != UserRole.ADMIN) {
            throw new DomainException("error.permission.denied");
        }
    }

    private void validateAdmin(String email, UUID workspaceId) {
        WorkspaceMembership membership = getMembership(email, workspaceId);
        if (membership.getRole() != UserRole.ADMIN) {
            throw new DomainException("error.permission.denied");
        }
    }
}