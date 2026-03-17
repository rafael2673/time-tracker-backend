package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public PageResponse<WorkspaceMemberResponse> getWorkspaceMembers(UUID workspaceId, String search, String roleStr, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("user.fullName").ascending());
        UserRole role = (roleStr != null && !roleStr.isBlank()) ? UserRole.valueOf(roleStr) : null;

        Page<WorkspaceMembership> result = membershipRepository.findByWorkspaceIdWithFilters(workspaceId, search, role, pageable);

        List<WorkspaceMemberResponse> content = result.getContent().stream()
                .map(wm -> new WorkspaceMemberResponse(wm.getUser().getId(), wm.getUser().getFullName(), wm.getUser().getEmail(), wm.getRole().name(), wm.getWorkPolicy() != null ? wm.getWorkPolicy().getName() : null, wm.getWorkPolicy() != null ? wm.getWorkPolicy().getId() : null, wm.getJoinedAt(), wm.isActive()))
                .toList();

        return new PageResponse<>(content, result.getTotalPages(), result.getTotalElements(), result.getNumber());
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

    public void updateMember(String authenticatedEmail, UUID workspaceId, UUID employeeId, UpdateMemberRequest request) {
        User authUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        WorkspaceMembership authMembership = membershipRepository.findByUserIdAndWorkspaceId(authUser.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));

        if (authMembership.getRole() == UserRole.EMPLOYEE) {
            throw new DomainException("error.permission.denied");
        }

        WorkspaceMembership targetMembership = membershipRepository.findByUserIdAndWorkspaceId(employeeId, workspaceId)
                .orElseThrow(() -> new DomainException("error.employee.not_in_workspace"));

        WorkPolicy policy = workPolicyRepository.findById(request.workPolicyId())
                .orElseThrow(() -> new DomainException("error.policy.not_found"));

        if (!policy.getWorkspace().getId().equals(workspaceId)) {
            throw new DomainException("error.permission.denied");
        }

        User targetUser = targetMembership.getUser();
        targetUser.setFullName(request.fullName());
        userRepository.save(targetUser);

        targetMembership.setRole(UserRole.valueOf(request.role()));
        targetMembership.setWorkPolicy(policy);
        membershipRepository.save(targetMembership);
    }

    public void changeMemberStatus(String authenticatedEmail, UUID workspaceId, UUID employeeId, boolean active) {
        User authUser = userRepository.findByEmail(authenticatedEmail).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership authMembership = membershipRepository.findByUserIdAndWorkspaceId(authUser.getId(), workspaceId).orElseThrow(() -> new DomainException("error.permission.denied"));

        if (authMembership.getRole() == UserRole.EMPLOYEE) throw new DomainException("error.permission.denied");

        WorkspaceMembership targetMembership = membershipRepository.findByUserIdAndWorkspaceId(employeeId, workspaceId).orElseThrow(() -> new DomainException("error.employee.not_in_workspace"));
        targetMembership.setActive(active);
        membershipRepository.save(targetMembership);
    }
}