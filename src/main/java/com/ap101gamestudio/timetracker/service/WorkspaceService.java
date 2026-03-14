package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.AddMemberRequest;
import com.ap101gamestudio.timetracker.dto.MemberResponse;
import com.ap101gamestudio.timetracker.dto.WorkspaceResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final PasswordEncoder passwordEncoder;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            UserRepository userRepository,
            WorkPolicyRepository workPolicyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.workPolicyRepository = workPolicyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<WorkspaceResponse> getUserWorkspaces(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        return membershipRepository.findAllByUserId(user.getId())
                .stream()
                .map(membership -> new WorkspaceResponse(
                        membership.getWorkspace().getId(),
                        membership.getWorkspace().getName(),
                        membership.getRole()
                ))
                .collect(Collectors.toList());
    }

    public List<MemberResponse> getWorkspaceMembers(String authenticatedEmail, UUID workspaceId) {
        validateManagerOrAdmin(authenticatedEmail, workspaceId);

        return membershipRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .map(membership -> new MemberResponse(
                        membership.getUser().getId(),
                        membership.getUser().getFullName(),
                        membership.getUser().getEmail(),
                        membership.getRole(),
                        membership.getWorkPolicy() != null ? membership.getWorkPolicy().getName() : null
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberResponse addMember(String authenticatedEmail, UUID workspaceId, AddMemberRequest request) {
        WorkspaceMembership authMembership = validateManagerOrAdmin(authenticatedEmail, workspaceId);

        if (authMembership.getRole() == UserRole.MANAGER && request.role() == UserRole.ADMIN) {
            throw new DomainException("error.permission.denied");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new DomainException("error.workspace.not_found"));

        WorkPolicy policy = workPolicyRepository.findById(request.workPolicyId())
                .orElseThrow(() -> new DomainException("error.policy.not_found"));

        if (!policy.getWorkspace().getId().equals(workspaceId)) {
            throw new DomainException("error.permission.denied");
        }

        User targetUser = userRepository.findByEmail(request.email()).orElse(null);

        if (targetUser == null) {
            String temporaryPassword = UUID.randomUUID().toString();
            targetUser = new User(request.email(), passwordEncoder.encode(temporaryPassword), request.fullName());
            targetUser = userRepository.save(targetUser);
        }

        boolean alreadyMember = membershipRepository.findByUserIdAndWorkspaceId(targetUser.getId(), workspaceId).isPresent();
        if (alreadyMember) {
            throw new DomainException("error.user.already_in_workspace");
        }

        WorkspaceMembership newMembership = new WorkspaceMembership(targetUser, workspace, request.role(), policy);
        membershipRepository.save(newMembership);

        return new MemberResponse(
                targetUser.getId(),
                targetUser.getFullName(),
                targetUser.getEmail(),
                request.role(),
                policy.getName()
        );
    }

    public WorkspaceMembership validateManagerOrAdmin(String email, UUID workspaceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));

        if (membership.getRole() != UserRole.MANAGER && membership.getRole() != UserRole.ADMIN) {
            throw new DomainException("error.permission.denied");
        }

        return membership;
    }
}