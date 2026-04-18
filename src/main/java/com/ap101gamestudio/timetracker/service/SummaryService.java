package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.*;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.SpecialDate;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.SpecialDateType;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.SpecialDateRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SummaryService {

    private static final int DIAS_UTEIS_MEDIOS_POR_MES = 22;
    private static final double MIN_COMPENSATORY_BALANCE_HOURS = 8.0;

    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final TimeTrackingService timeTrackingService;
    private final SpecialDateRepository specialDateRepository;
    private final HrRuleService hrRuleService;

    public SummaryService(UserRepository userRepository,
                          WorkspaceMembershipRepository membershipRepository,
                          TimeTrackingService timeTrackingService,
                          SpecialDateRepository specialDateRepository,
                          HrRuleService hrRuleService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.timeTrackingService = timeTrackingService;
        this.specialDateRepository = specialDateRepository;
        this.hrRuleService = hrRuleService;
    }

    public EmployeeDashboardSummary getEmployeeSummary(String authenticatedEmail, UUID employeeId, UUID workspaceId) {
        validarPermissaoAcesso(authenticatedEmail, employeeId, workspaceId);

        User employee = buscarUsuarioPorId(employeeId);
        validarVinculoComWorkspace(employee.getId(), workspaceId);

        YearMonth now = YearMonth.now();
        int currentQuarter = calcularTrimestreAtual(now);

        MonthlyBalanceResponse quarterlyBalance = timeTrackingService.getQuarterlyBalance(
                employee.getEmail(), now.getYear(), currentQuarter, workspaceId
        );

        MonthlyBalanceResponse monthlyBalance = timeTrackingService.getMonthlyBalance(
                employee.getEmail(), now.getYear(), now.getMonthValue(), workspaceId
        );

        VacationRightResponse vacationRight = hrRuleService.calculateVacationRights(
                workspaceId, employeeId, now.getYear()
        );

        long pendingJustifications = timeTrackingService.countJustificationsPending(employeeId, workspaceId);

        return new EmployeeDashboardSummary(
                quarterlyBalance.workedHours(),
                monthlyBalance.expectedHours(),
                quarterlyBalance.balance(),
                monthlyBalance.balance(),
                monthlyBalance.unjustifiedAbsences(),
                pendingJustifications,
                vacationRight.earnedVacationDays()
        );
    }

    public NextHolidayResponse getNextSpecialDate(UUID workspaceId, UUID employeeId, String authenticatedEmail) {
        LocalDate today = LocalDate.now();
        
        UUID targetEmployeeId = employeeId;
        if (targetEmployeeId == null) {
            targetEmployeeId = userRepository.findByEmail(authenticatedEmail)
                    .map(User::getId)
                    .orElse(null);
        }

        final UUID finalEmployeeId = targetEmployeeId;

        return specialDateRepository.findByWorkspaceId(workspaceId).stream()
                .filter(sd -> isEligibleForSpecialDate(sd, workspaceId, finalEmployeeId))
                .map(sd -> mapearParaProximaOcorrencia(sd, today))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(NextHolidayResponse::date))
                .orElse(null);
    }

    private boolean isEligibleForSpecialDate(SpecialDate sd, UUID workspaceId, UUID employeeId) {
        if (sd.getType() != SpecialDateType.COMPENSATORY_COLLECTIVE) {
            return true;
        }

        if (employeeId == null) {
            return false;
        }

        User employee = userRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return false;
        }

        YearMonth now = YearMonth.now();
        int currentQuarter = calcularTrimestreAtual(now);

        MonthlyBalanceResponse balance = timeTrackingService.getQuarterlyBalance(
                employee.getEmail(), now.getYear(), currentQuarter, workspaceId
        );

        return balance.balance() >= MIN_COMPENSATORY_BALANCE_HOURS;
    }

    public AbsencePieChartResponse getCompanyAbsences(UUID workspaceId, int year, int month) {
        List<WorkspaceMembership> members = buscarMembrosAtivos(workspaceId);

        int totalAbsences = 0;
        int totalExpectedDays = 0;

        for (WorkspaceMembership member : members) {
            MonthlyBalanceResponse balance = timeTrackingService.getMonthlyBalance(
                    member.getUser().getEmail(), year, month, workspaceId
            );
            totalAbsences += balance.unjustifiedAbsences();
            totalExpectedDays += DIAS_UTEIS_MEDIOS_POR_MES;
        }

        double percentage = calcularPercentualFaltas(totalAbsences, totalExpectedDays);
        return new AbsencePieChartResponse(totalExpectedDays, totalAbsences, percentage);
    }

    private void validarPermissaoAcesso(String authenticatedEmail, UUID employeeId, UUID workspaceId) {
        User authUser = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        WorkspaceMembership authMembership = membershipRepository.findByUserIdAndWorkspaceId(authUser.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));

        if (authMembership.getRole() == UserRole.EMPLOYEE && !authUser.getId().equals(employeeId)) {
            throw new DomainException("error.permission.denied");
        }
    }

    private User buscarUsuarioPorId(UUID employeeId) {
        return userRepository.findById(employeeId)
                .orElseThrow(() -> new DomainException("error.user.not_found"));
    }

    private void validarVinculoComWorkspace(UUID userId, UUID workspaceId) {
        membershipRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new DomainException("error.employee.not_in_workspace"));
    }

    private int calcularTrimestreAtual(YearMonth date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    private NextHolidayResponse mapearParaProximaOcorrencia(SpecialDate specialDate, LocalDate referenceDate) {
        LocalDate nextOccurrence = calcularDataOcorrencia(specialDate, referenceDate);

        if (nextOccurrence.isBefore(referenceDate)) {
            return null;
        }

        return new NextHolidayResponse(
                specialDate.getDescription(),
                nextOccurrence,
                specialDate.getWorkloadMultiplier()
        );
    }

    private LocalDate calcularDataOcorrencia(SpecialDate specialDate, LocalDate referenceDate) {
        if (!specialDate.isRecurring()) {
            return specialDate.getDate();
        }

        LocalDate currentYearOccurrence = specialDate.getDate().withYear(referenceDate.getYear());

        if (currentYearOccurrence.isBefore(referenceDate)) {
            return currentYearOccurrence.plusYears(1);
        }

        return currentYearOccurrence;
    }

    private List<WorkspaceMembership> buscarMembrosAtivos(UUID workspaceId) {
        return membershipRepository.findByWorkspaceId(workspaceId).stream()
                .filter(WorkspaceMembership::isActive)
                .toList();
    }

    private double calcularPercentualFaltas(int absences, int expectedDays) {
        if (expectedDays <= 0) {
            return 0.0;
        }
        return ((double) absences / expectedDays) * 100;
    }

    public List<MonthSummaryResponse> getCompanyYearlyAverage(UUID workspaceId, int year, UUID policyId, String localeString) {
        List<WorkspaceMembership> members = buscarMembrosAtivos(workspaceId);

        if (policyId != null) {
            members = members.stream()
                    .filter(m -> m.getWorkPolicy() != null && m.getWorkPolicy().getId().equals(policyId))
                    .toList();
        }

        java.util.Locale locale = java.util.Locale.forLanguageTag(localeString);
        String[] monthNames = timeTrackingService.getMonthNames(locale);
        List<MonthSummaryResponse> summary = new java.util.ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            double totalWorked = 0.0;
            double totalExpected = 0.0;

            for (WorkspaceMembership member : members) {
                MonthlyBalanceResponse balance = timeTrackingService.getMonthlyBalance(
                        member.getUser().getEmail(), year, i, workspaceId
                );
                totalWorked += balance.workedHours();
                totalExpected += balance.expectedHours();
            }

            double avgWorked = members.isEmpty() ? 0.0 : Math.round((totalWorked / members.size()) * 100.0) / 100.0;
            double avgExpected = members.isEmpty() ? 0.0 : Math.round((totalExpected / members.size()) * 100.0) / 100.0;

            summary.add(new MonthSummaryResponse(i, monthNames[i - 1], avgWorked, avgExpected));
        }

        return summary;
    }

    public LaborRiskRankingResponse getLaborRiskRanking(UUID workspaceId) {
        List<WorkspaceMembership> members = buscarMembrosAtivos(workspaceId);
        YearMonth now = YearMonth.now();
        int currentQuarter = calcularTrimestreAtual(now);

        List<EmployeeBalanceDTO> allBalances = members.stream()
                .map(member -> {
                    MonthlyBalanceResponse balance = timeTrackingService.getQuarterlyBalance(
                            member.getUser().getEmail(), now.getYear(), currentQuarter, workspaceId
                    );
                    return new EmployeeBalanceDTO(
                            member.getUser().getId(),
                            member.getUser().getFullName(),
                            balance.balance()
                    );
                })
                .toList();

        List<EmployeeBalanceDTO> topPositive = allBalances.stream()
                .filter(b -> b.balance() > 0)
                .sorted(Comparator.comparing(EmployeeBalanceDTO::balance).reversed())
                .limit(5)
                .toList();

        List<EmployeeBalanceDTO> topNegative = allBalances.stream()
                .filter(b -> b.balance() < 0)
                .sorted(Comparator.comparing(EmployeeBalanceDTO::balance))
                .limit(5)
                .toList();

        return new LaborRiskRankingResponse(topPositive, topNegative);
    }

    private TimeDistributionResponse createTimeDistributionResponse(double totalExpected, double totalWorked, double totalOvertime, double totalAbsence) {
        double regularHours = Math.max(0, totalWorked - totalOvertime);
        
        return new TimeDistributionResponse(
                Math.round(regularHours * 100.0) / 100.0,
                Math.round(totalOvertime * 100.0) / 100.0,
                Math.round(totalAbsence * 100.0) / 100.0,
                Math.round(totalExpected * 100.0) / 100.0
        );
    }

    public TimeDistributionResponse getTimeDistribution(UUID workspaceId, int year, int month) {
        List<WorkspaceMembership> members = buscarMembrosAtivos(workspaceId);

        double totalExpected = 0.0;
        double totalWorked = 0.0;
        double totalOvertime = 0.0;
        double totalAbsence = 0.0;

        for (WorkspaceMembership member : members) {
            MonthlyBalanceResponse balance = timeTrackingService.getMonthlyBalance(
                    member.getUser().getEmail(), year, month, workspaceId
            );

            totalExpected += balance.expectedHours();
            totalWorked += balance.workedHours();

            if (balance.balance() > 0) {
                totalOvertime += balance.balance();
            } else if (balance.balance() < 0) {
                totalAbsence += Math.abs(balance.balance());
            }
        }

        return createTimeDistributionResponse(totalExpected, totalWorked, totalOvertime, totalAbsence);
    }

    public TimeDistributionResponse getEmployeeTimeDistribution(String authenticatedEmail, UUID employeeId, UUID workspaceId, int year, int month) {
        validarPermissaoAcesso(authenticatedEmail, employeeId, workspaceId);

        User employee = buscarUsuarioPorId(employeeId);
        validarVinculoComWorkspace(employee.getId(), workspaceId);

        MonthlyBalanceResponse balance = timeTrackingService.getMonthlyBalance(
                employee.getEmail(), year, month, workspaceId
        );

        double totalWorked = balance.workedHours();
        double totalExpected = balance.expectedHours();
        double overtime = Math.max(0, balance.balance());
        double absence = Math.max(0, -balance.balance());

        return createTimeDistributionResponse(totalExpected, totalWorked, overtime, absence);
    }
}