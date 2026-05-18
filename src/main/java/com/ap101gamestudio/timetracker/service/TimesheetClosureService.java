package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.*;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.ClosurePendingStrategy;
import com.ap101gamestudio.timetracker.model.enums.OvertimeStrategy;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.*;
import com.ap101gamestudio.timetracker.strategy.BankExpirationStrategy;
import com.ap101gamestudio.timetracker.strategy.BankExpirationStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class TimesheetClosureService {

    private final MonthlyClosureRepository closureRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final TimeTrackingService timeTrackingService;
    private final UserRepository userRepository;
    private final TimeRecordRepository recordRepository;
    private final BankExpirationStrategyFactory expirationStrategyFactory;

    public TimesheetClosureService(MonthlyClosureRepository closureRepository,
                                   WorkspaceMembershipRepository membershipRepository,
                                   TimeTrackingService timeTrackingService,
                                   UserRepository userRepository,
                                   TimeRecordRepository recordRepository,
                                   BankExpirationStrategyFactory expirationStrategyFactory) {
        this.closureRepository = closureRepository;
        this.membershipRepository = membershipRepository;
        this.timeTrackingService = timeTrackingService;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.expirationStrategyFactory = expirationStrategyFactory;
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

    @Transactional
    public void executeAutoClosureForWorkspace(Workspace workspace, YearMonth yearMonth) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        if (closureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(workspace.getId(), year, month)) {
            return;
        }

        List<WorkspaceMembership> members = getActiveMembers(workspace.getId());
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        PreviousMonthDto previousMonth = getPreviousMonth(year, month);

        for (WorkspaceMembership member : members) {
            boolean hasPendingRecords = hasPendingApprovals(member.getUser().getId(), workspace.getId(), startOfMonth, endOfMonth);

            if (hasPendingRecords && workspace.getClosurePendingStrategy() == ClosurePendingStrategy.BLOCKING) {
                continue;
            }

            if (hasPendingRecords && workspace.getClosurePendingStrategy() == ClosurePendingStrategy.FORCE_CLOSE) {
                rejectPendingRecords(member.getUser().getId(), workspace.getId(), startOfMonth, endOfMonth);
            }

            processAndSaveMemberClosure(member, workspace.getId(), year, month, previousMonth);
        }
    }

    private boolean hasPendingApprovals(UUID userId, UUID workspaceId, LocalDateTime start, LocalDateTime end) {
        List<TimeRecord> records = recordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(userId, workspaceId, start, end);
        return records.stream().anyMatch(TimeRecord::isPendingApprovation);
    }

    private void rejectPendingRecords(UUID userId, UUID workspaceId, LocalDateTime start, LocalDateTime end) {
        List<TimeRecord> pendingRecords = recordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(userId, workspaceId, start, end)
                .stream()
                .filter(TimeRecord::isPendingApprovation)
                .toList();

        for (TimeRecord record : pendingRecords) {
            record.setPendingApprovation(false);
            record.setRejected(true);
        }

        recordRepository.saveAll(pendingRecords);
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

        BankExpirationStrategy expirationStrategy = expirationStrategyFactory.getStrategy(policy.getExpirationModel());
        var finalBalances = expirationStrategy.apply(overtimeCalculation, accumulation, policy, year, month, member.getUser().getId());

        MonthlyClosure closure = saveClosure(member, year, month, balance, finalBalances.overtime(), finalBalances.accumulation());
        return mapToResponse(closure);
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
                closure.getClosedAt(),
                closure.getSignedAt()
        );
    }

    public List<MonthlyClosureResponse> getClosures(String email, UUID workspaceId, int year, int month) {
        // Se for manager, retorna todos do mês. Se for empregado, retorna só o dele.
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));

        if (membership.getRole() == com.ap101gamestudio.timetracker.model.enums.UserRole.EMPLOYEE) {
            return closureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(workspaceId, user.getId(), year, month)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return closureRepository.findByWorkspaceIdAndReferenceYearAndReferenceMonth(workspaceId, year, month)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MonthlyClosureResponse signClosure(String email, UUID workspaceId, UUID closureId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        MonthlyClosure closure = closureRepository.findById(closureId).orElseThrow(() -> new DomainException("error.closure.not_found"));

        if (!closure.getWorkspace().getId().equals(workspaceId) || !closure.getUser().getId().equals(user.getId())) {
            throw new DomainException("error.permission.denied");
        }

        if (closure.getSignedAt() != null) {
            throw new DomainException("error.closure.already_signed");
        }

        closure.setSignedAt(LocalDateTime.now());
        MonthlyClosure saved = closureRepository.save(closure);
        return mapToResponse(saved);
    }
}