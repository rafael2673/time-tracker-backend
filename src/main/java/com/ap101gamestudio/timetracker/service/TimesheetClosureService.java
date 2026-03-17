package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.MonthlyClosureResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.OvertimeStrategy;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TimesheetClosureService {

    private final MonthlyClosureRepository closureRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final TimeTrackingService timeTrackingService;
    private final UserRepository userRepository;

    public TimesheetClosureService(MonthlyClosureRepository closureRepository, WorkspaceMembershipRepository membershipRepository, TimeTrackingService timeTrackingService, UserRepository userRepository) {
        this.closureRepository = closureRepository;
        this.membershipRepository = membershipRepository;
        this.timeTrackingService = timeTrackingService;
        this.userRepository = userRepository;
    }

    private void validateManagerAccess(String email, UUID workspaceId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId).orElseThrow(() -> new DomainException("error.permission.denied"));
        if (membership.getRole() == UserRole.EMPLOYEE) throw new DomainException("error.permission.denied");
    }

    @Transactional
    public List<MonthlyClosureResponse> closeWorkspaceMonth(String email, UUID workspaceId, int year, int month) {
        validateManagerAccess(email, workspaceId);

        if (closureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(workspaceId, year, month)) {
            throw new DomainException("error.closure.already_closed");
        }

        List<WorkspaceMembership> activeMembers = membershipRepository.findByWorkspaceId(workspaceId)
                .stream().filter(WorkspaceMembership::isActive).toList();

        List<MonthlyClosureResponse> responses = new ArrayList<>();

        for (WorkspaceMembership member : activeMembers) {
            var balance = timeTrackingService.getMonthlyBalance(member.getUser().getEmail(), year, month, workspaceId);

            double rawBalance = balance.balance();
            double paidOvertime = 0.0;
            double bankedDelta = 0.0;

            WorkPolicy policy = member.getWorkPolicy();

            if (rawBalance > 0) {
                if (policy.getOvertimeStrategy() == OvertimeStrategy.PAY_ONLY) {
                    paidOvertime = rawBalance;
                } else if (policy.getOvertimeStrategy() == OvertimeStrategy.BANK_ONLY) {
                    bankedDelta = rawBalance;
                } else if (policy.getOvertimeStrategy() == OvertimeStrategy.MIXED) {
                    if (rawBalance <= policy.getMaxBankHoursPerMonth()) {
                        bankedDelta = rawBalance;
                    } else {
                        bankedDelta = policy.getMaxBankHoursPerMonth();
                        paidOvertime = rawBalance - bankedDelta;
                    }
                }
            } else if (rawBalance < 0) {
                bankedDelta = rawBalance;
            }

            MonthlyClosure closure = new MonthlyClosure(
                    member.getWorkspace(),
                    member.getUser(),
                    year, month,
                    balance.workedHours(),
                    balance.expectedHours(),
                    rawBalance,
                    paidOvertime,
                    bankedDelta
            );

            MonthlyClosure saved = closureRepository.save(closure);
            responses.add(new MonthlyClosureResponse(saved.getId(), member.getUser().getFullName(), year, month, balance.workedHours(), balance.expectedHours(), rawBalance, paidOvertime, bankedDelta, saved.getClosedAt()));
        }

        return responses;
    }

    public List<MonthlyClosureResponse> getClosures(String email, UUID workspaceId, int year, int month) {
        validateManagerAccess(email, workspaceId);
        return closureRepository.findByWorkspaceIdAndReferenceYearAndReferenceMonth(workspaceId, year, month)
                .stream()
                .map(c -> new MonthlyClosureResponse(c.getId(), c.getUser().getFullName(), c.getReferenceYear(), c.getReferenceMonth(), c.getWorkedHours(), c.getExpectedHours(), c.getRawBalance(), c.getPaidOvertimeHours(), c.getBankedHoursDelta(), c.getClosedAt()))
                .toList();
    }
}