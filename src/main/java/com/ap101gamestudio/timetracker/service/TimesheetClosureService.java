package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.*;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.OvertimeStrategy;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        validateMonthNotAlreadyClosed(workspaceId, year, month);

        List<WorkspaceMembership> activeMembers = getActiveMembers(workspaceId);
        PreviousMonthDto previousMonth = getPreviousMonth(year, month);

        return activeMembers.stream()
                .map(member -> processAndSaveMemberClosure(member, workspaceId, year, month, previousMonth))
                .toList();
    }

    private void validateMonthNotAlreadyClosed(UUID workspaceId, int year, int month) {
        if (closureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(workspaceId, year, month)) {
            throw new DomainException("error.closure.already_closed");
        }
    }

    private List<WorkspaceMembership> getActiveMembers(UUID workspaceId) {
        return membershipRepository.findByWorkspaceId(workspaceId)
                .stream()
                .filter(WorkspaceMembership::isActive)
                .toList();
    }

    private PreviousMonthDto getPreviousMonth(int year, int month) {
        int prevYear = (month == 1) ? year - 1 : year;
        int prevMonth = (month == 1) ? 12 : month - 1;
        return new PreviousMonthDto(prevYear, prevMonth);
    }

    private MonthlyClosureResponse processAndSaveMemberClosure(WorkspaceMembership member, UUID workspaceId, int year, int month, PreviousMonthDto previousMonth) {
        var balance = timeTrackingService.getMonthlyBalance(member.getUser().getEmail(), year, month, workspaceId);
        WorkPolicy policy = member.getWorkPolicy();

        var overtimeCalculation = calculateOvertime(balance.balance(), policy);
        var accumulation = calculateAccumulation(member.getUser().getId(), workspaceId, previousMonth, overtimeCalculation.bankedDelta());

        var finalBalances = applyExpirationRules(overtimeCalculation, accumulation, policy, month);

        MonthlyClosure closure = saveClosure(member, year, month, balance, finalBalances.overtime(), finalBalances.accumulation());
        return mapToResponse(closure);
    }

    private ExpirationResultDto applyExpirationRules(OvertimeCalculationDto overtime, BankAccumulationDto accumulation, WorkPolicy policy, int month) {
        if (policy == null || policy.getBankExpirationMonths() <= 0 || month % policy.getBankExpirationMonths() != 0) {
            return new ExpirationResultDto(overtime, accumulation);
        }

        double finalAccumulated = accumulation.newAccumulated();
        OvertimeCalculationDto newOvertime = overtime;

        if (finalAccumulated > 0) {
            newOvertime = new OvertimeCalculationDto(
                    overtime.paidOvertimeHours() + finalAccumulated,
                    overtime.bankedDelta()
            );
        }

        BankAccumulationDto newAccumulation = new BankAccumulationDto(accumulation.previousAccumulated(), 0.0);

        return new ExpirationResultDto(newOvertime, newAccumulation);
    }

    private OvertimeCalculationDto calculateOvertime(double rawBalance, WorkPolicy policy) {
        OvertimeStrategy strategy = (policy != null) ? policy.getOvertimeStrategy() : OvertimeStrategy.BANK_ONLY;
        double maxBankHours = (policy != null) ? policy.getMaxBankHoursPerMonth() : 0.0;

        if (rawBalance > 0) {
            return switch (strategy) {
                case PAY_ONLY -> new OvertimeCalculationDto(rawBalance, 0.0);
                case BANK_ONLY -> new OvertimeCalculationDto(0.0, rawBalance);
                case MIXED -> calculateMixedOvertime(rawBalance, maxBankHours);
            };
        } else if (rawBalance < 0) {
            return new OvertimeCalculationDto(0.0, rawBalance);
        }
        return new OvertimeCalculationDto(0.0, 0.0);
    }

    private OvertimeCalculationDto calculateMixedOvertime(double rawBalance, double maxBankHours) {
        if (rawBalance <= maxBankHours) {
            return new OvertimeCalculationDto(0.0, rawBalance);
        }
        return new OvertimeCalculationDto(rawBalance - maxBankHours, maxBankHours);
    }

    private BankAccumulationDto calculateAccumulation(UUID userId, UUID workspaceId, PreviousMonthDto previousMonth, double bankedDelta) {
        double previousAccumulated = closureRepository
                .findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(workspaceId, userId, previousMonth.year(), previousMonth.month())
                .map(MonthlyClosure::getAccumulatedBankHours)
                .orElse(0.0);

        return new BankAccumulationDto(previousAccumulated, previousAccumulated + bankedDelta);
    }

    private MonthlyClosure saveClosure(WorkspaceMembership member, int year, int month, MonthlyBalanceResponse balance, OvertimeCalculationDto overtime, BankAccumulationDto accumulation) {
        MonthlyClosure closure = new MonthlyClosure(
                member.getWorkspace(),
                member.getUser(),
                year, month,
                balance.workedHours(),
                balance.expectedHours(),
                balance.balance(),
                overtime.paidOvertimeHours(),
                overtime.bankedDelta(),
                accumulation.newAccumulated()
        );
        return closureRepository.save(closure);
    }

    private MonthlyClosureResponse mapToResponse(MonthlyClosure closure) {
        return new MonthlyClosureResponse(
                closure.getId(),
                closure.getUser().getId(),
                closure.getUser().getFullName(),
                closure.getReferenceYear(),
                closure.getReferenceMonth(),
                closure.getWorkedHours(),
                closure.getExpectedHours(),
                closure.getRawBalance(),
                closure.getPaidOvertimeHours(),
                closure.getBankedHoursDelta(),
                closure.getAccumulatedBankHours(),
                closure.getClosedAt()
        );
    }

    public List<MonthlyClosureResponse> getClosures(String email, UUID workspaceId, int year, int month) {
        validateManagerAccess(email, workspaceId);
        return closureRepository.findByWorkspaceIdAndReferenceYearAndReferenceMonth(workspaceId, year, month)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}