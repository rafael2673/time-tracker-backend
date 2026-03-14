package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.EmployeeDashboardSummary;
import com.ap101gamestudio.timetracker.dto.MonthlyBalanceResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.UUID;

@Service
public class SummaryService {

    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final TimeTrackingService timeTrackingService;

    public SummaryService(UserRepository userRepository, WorkspaceMembershipRepository membershipRepository,
                          TimeTrackingService timeTrackingService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.timeTrackingService = timeTrackingService;
    }

    public EmployeeDashboardSummary getEmployeeSummary(String authenticatedEmail, UUID employeeId, UUID workspaceId) {

        User authUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        WorkspaceMembership authMembership =
                membershipRepository.findByUserIdAndWorkspaceId(authUser.getId(), workspaceId)
                        .orElseThrow(() -> new DomainException("error.permission.denied"));

        if (authMembership.getRole() == UserRole.EMPLOYEE && !authUser.getId().equals(employeeId)) {
            throw new DomainException("error.permission.denied");
        }

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        WorkspaceMembership membership =
                membershipRepository.findByUserIdAndWorkspaceId(employee.getId(), workspaceId)
                        .orElseThrow(() -> new DomainException("error.employee.not_in_workspace"));

        YearMonth now = YearMonth.now();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;

        MonthlyBalanceResponse quarterlyBalance =
                timeTrackingService.getQuarterlyBalance(employee.getEmail(), now.getYear(), currentQuarter, workspaceId);

        long pendingJustifications = timeTrackingService.countJustificationsPending(employeeId, workspaceId);

        return new EmployeeDashboardSummary(
                quarterlyBalance.workedHours(),
                quarterlyBalance.balance(),
                pendingJustifications
        );
    }
}