package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.EmployeeDashboardSummary;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SummaryService {

    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public SummaryService(UserRepository userRepository, WorkspaceMembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    public EmployeeDashboardSummary getEmployeeSummary(String authenticatedEmail, UUID workspaceId, UUID employeeId) {
        User authUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        WorkspaceMembership authMembership = membershipRepository.findByUserIdAndWorkspaceId(authUser.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));

        if (authMembership.getRole() == UserRole.EMPLOYEE && !authUser.getId().equals(employeeId)) {
            throw new DomainException("error.permission.denied");
        }

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        membershipRepository.findByUserIdAndWorkspaceId(employee.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.employee.not_in_workspace"));

        return new EmployeeDashboardSummary("0h", "0h", 0);
    }
}