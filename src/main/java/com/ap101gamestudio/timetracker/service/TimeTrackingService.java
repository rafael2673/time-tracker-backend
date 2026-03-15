package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.*;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.repository.*;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TimeTrackingService {

    private final TimeRecordRepository timeRecordRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final MessageSource messageSource;
    private final SpecialDateRepository specialDateRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;


    public TimeTrackingService(
            TimeRecordRepository timeRecordRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            MessageSource messageSource,
            SpecialDateRepository specialDateRepository,
            EmployeeLeaveRepository employeeLeaveRepository
    ) {
        this.timeRecordRepository = timeRecordRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.messageSource = messageSource;
        this.specialDateRepository = specialDateRepository;
        this.employeeLeaveRepository = employeeLeaveRepository;
    }

    public TimeRecordResponse registerPoint(String email, CreateTimeRecordRequest request, UUID workspaceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new DomainException("error.workspace.not_found"));

        TimeRecord record = new TimeRecord(
                user,
                workspace,
                request.recordType(),
                request.source(),
                request.registeredAt() != null ? request.registeredAt() : LocalDateTime.now(),
                null,
                null
        );

        TimeRecord saved = timeRecordRepository.save(record);
        return mapToResponse(saved);
    }

    public TimeRecordResponse updateRecord(UUID id, String email, UpdateTimeRecordRequest request, UUID workspaceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        TimeRecord originalRecord = timeRecordRepository.findById(id)
                .orElseThrow(() -> new DomainException("error.record.not_found"));

        if (!originalRecord.getUser().getId().equals(user.getId()) ||
                originalRecord.getWorkspace() == null ||
                !originalRecord.getWorkspace().getId().equals(workspaceId)) {
            throw new DomainException("error.permission.denied");
        }

        TimeRecord newRecord = new TimeRecord(
                user,
                originalRecord.getWorkspace(),
                originalRecord.getRecordType(),
                RecordSource.MANUAL,
                request.registeredAt(),
                request.justification(),
                originalRecord
        );

        TimeRecord saved = timeRecordRepository.save(newRecord);
        return mapToResponse(saved);
    }

    public List<TimeRecordResponse> getRecordsByDate(String email, LocalDate date, UUID workspaceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<TimeRecord> records = timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(
                user.getId(), workspaceId, startOfDay, endOfDay);
        records = filterActiveRecords(records);

        return records.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DailySummaryResponse> getWeeklySummary(String email, LocalDate referenceDate, UUID workspaceId, String localeString) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId).orElseThrow(() -> new DomainException("error.permission.denied"));

        LocalDate startOfWeek = referenceDate;
        while (startOfWeek.getDayOfWeek() != DayOfWeek.SUNDAY) {
            startOfWeek = startOfWeek.minusDays(1);
        }
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        List<SpecialDate> specialDates = specialDateRepository.findRelevantDates(workspaceId, startOfWeek, endOfWeek);

        List<DailySummaryResponse> weeklySummary = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Locale locale = Locale.forLanguageTag(localeString);
        String[] dayNames = getDayNames(locale);
        List<DayOfWeek> workingDays = membership.getWorkPolicy().getWorkingDaysList();
        double dailyLimitHours = membership.getWorkPolicy().getDailyMinutesLimit() / 60.0;

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startOfWeek.plusDays(i);
            LocalDateTime startOfDay = currentDate.atStartOfDay();
            LocalDateTime endOfDay = currentDate.atTime(23, 59, 59);

            List<TimeRecord> dailyRecords = timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(user.getId(), workspaceId, startOfDay, endOfDay);
            dailyRecords = filterActiveRecords(dailyRecords);
            double hours = roundHours(calculateWorkedHours(dailyRecords));

            double expectedHoursForDay = 0.0;
            if (workingDays.contains(currentDate.getDayOfWeek())) {
                SpecialDate specialDate = findMatchingSpecialDate(specialDates, currentDate);
                if (specialDate != null) {
                    expectedHoursForDay = dailyLimitHours * specialDate.getWorkloadMultiplier();
                } else {
                    expectedHoursForDay = dailyLimitHours;
                }
            }

            weeklySummary.add(new DailySummaryResponse(dayNames[i], hours, expectedHoursForDay, currentDate.format(formatter)));
        }
        return weeklySummary;
    }

    public MonthlyBalanceResponse getQuarterlyBalance(String email, int year, int quarter, UUID workspaceId) {
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = startMonth + 2;

        double totalWorked = 0;
        double totalExpected = 0;

        for(int m = startMonth; m <= endMonth; m++) {
            MonthlyBalanceResponse monthly = getMonthlyBalance(email, year, m, workspaceId);
            totalWorked += monthly.workedHours();
            totalExpected += monthly.expectedHours();
        }

        double finalBalance = roundHours(totalWorked - totalExpected);

        return new MonthlyBalanceResponse(roundHours(totalWorked), roundHours(totalExpected), finalBalance);
    }

    public List<Integer> getAvailableYears(String email, UUID workspaceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        List<Integer> years = timeRecordRepository.findAvailableYears(user.getId(), workspaceId);
        if (years.isEmpty()) {
            years.add(LocalDate.now().getYear());
        }
        return years;
    }

    public List<MonthSummaryResponse> getYearlySummary(String email, int year, UUID workspaceId, String localeString) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId).orElseThrow(() -> new DomainException("error.permission.denied"));

        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        
        PeriodData periodData = getPeriodData(user.getId(), workspaceId, startOfYear, endOfYear);

        Locale locale = Locale.forLanguageTag(localeString);
        String[] monthNames = getMonthNames(locale);
        List<MonthSummaryResponse> summary = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            final int currentMonth = i;
            List<TimeRecord> monthRecords = periodData.records().stream().filter(r -> r.getRegisteredAt().getMonthValue() == currentMonth).toList();
            double hours = roundHours(calculateWorkedHours(monthRecords));

            YearMonth ym = YearMonth.of(year, currentMonth);
            double expectedHours = roundHours(calculateExpectedHours(ym, membership.getWorkPolicy(), periodData.specialDates(), periodData.leaves()));

            summary.add(new MonthSummaryResponse(currentMonth, monthNames[currentMonth - 1], hours, expectedHours));
        }
        return summary;
    }

    public MonthlyBalanceResponse getMonthlyBalance(String email, int year, int month, UUID workspaceId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId).orElseThrow(() -> new DomainException("error.permission.denied"));

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59);
        
        PeriodData periodData = getPeriodData(user.getId(), workspaceId, startOfMonth, endOfMonth);

        double workedHours = roundHours(calculateWorkedHours(periodData.records()));
        double expectedHours = roundHours(calculateExpectedHours(ym, membership.getWorkPolicy(), periodData.specialDates(), periodData.leaves()));
        double balance = roundHours(workedHours - expectedHours);

        return new MonthlyBalanceResponse(workedHours, expectedHours, balance);
    }
    
    private record PeriodData(List<SpecialDate> specialDates, List<EmployeeLeave> leaves, List<TimeRecord> records) {}

    private PeriodData getPeriodData(UUID userId, UUID workspaceId, LocalDateTime start, LocalDateTime end) {
        List<SpecialDate> specialDates = specialDateRepository.findRelevantDates(workspaceId, start.toLocalDate(), end.toLocalDate());
        List<EmployeeLeave> leaves = employeeLeaveRepository.findOverlappingLeaves(userId, workspaceId, start.toLocalDate(), end.toLocalDate());
        List<TimeRecord> records = timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(userId, workspaceId, start, end);
        return new PeriodData(specialDates, leaves, filterActiveRecords(records));
    }

    private double calculateExpectedHours(YearMonth yearMonth, WorkPolicy policy, List<SpecialDate> specialDates, List<EmployeeLeave> leaves) {
        LocalDate today = LocalDate.now();
        LocalDate lastDayToCount = resolveLastDayToCount(yearMonth, today);
        List<DayOfWeek> workingDays = policy.getWorkingDaysList();
        double dailyHours = policy.getDailyMinutesLimit() / 60.0;
        double totalExpectedHours = 0.0;

        for (int i = 1; i <= yearMonth.lengthOfMonth(); i++) {
            LocalDate day = yearMonth.atDay(i);
            if (!day.isAfter(lastDayToCount)) {
                boolean isLeave = leaves.stream().anyMatch(l -> !day.isBefore(l.getStartDate()) && !day.isAfter(l.getEndDate()));

                if (isLeave) continue;

                if (workingDays.contains(day.getDayOfWeek())) {
                    SpecialDate specialDate = findMatchingSpecialDate(specialDates, day);
                    if (specialDate != null) {
                        totalExpectedHours += dailyHours * specialDate.getWorkloadMultiplier();
                    } else {
                        totalExpectedHours += dailyHours;
                    }
                }
            }
        }
        return totalExpectedHours;
    }

    private LocalDate resolveLastDayToCount(YearMonth yearMonth, LocalDate today) {
        if (yearMonth.atDay(1).isAfter(today)) {
            return yearMonth.atDay(1).minusDays(1);
        }
        if (yearMonth.atEndOfMonth().isBefore(today)) {
            return yearMonth.atEndOfMonth();
        }
        return today;
    }

    private List<TimeRecord> filterActiveRecords(List<TimeRecord> records) {
        List<UUID> supersededIds = records.stream()
                .filter(r -> r.getEditedFrom() != null)
                .map(r -> r.getEditedFrom().getId())
                .toList();

        return records.stream()
                .filter(r -> !supersededIds.contains(r.getId()))
                .toList();
    }

    private double calculateWorkedHours(List<TimeRecord> records) {
        if (records == null || records.isEmpty()) return 0.0;
        List<TimeRecord> modifiableRecords = new ArrayList<>(records);
        long totalSeconds = 0;
        LocalDateTime lastEntry = null;
        String currentStatus = "IDLE";

        modifiableRecords.sort(Comparator.comparing(TimeRecord::getRegisteredAt));

        for (TimeRecord record : modifiableRecords) {
            LocalDateTime time = record.getRegisteredAt();
            currentStatus = switch (record.getRecordType().name()) {
                case "ENTRY", "PAUSE_END" -> {
                    lastEntry = time;
                    yield "WORKING";
                }
                case "PAUSE_START", "EXIT" -> {
                    if (lastEntry != null && "WORKING".equals(currentStatus)) {
                        totalSeconds += Duration.between(lastEntry, time).getSeconds();
                    }
                    yield "PAUSED_OR_FINISHED";
                }
                default -> currentStatus;
            };
        }

        if ("WORKING".equals(currentStatus) && lastEntry != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.toLocalDate().equals(lastEntry.toLocalDate())) {
                totalSeconds += Duration.between(lastEntry, now).getSeconds();
            }
        }

        return totalSeconds / 3600.0;
    }

    private double roundHours(double value) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private TimeRecordResponse mapToResponse(TimeRecord saved) {
        String workspaceName = "Remote / Unknown";
        Workspace ws = saved.getWorkspace();
        if (ws != null) {
            workspaceName = ws.getName();
        }

        return new TimeRecordResponse(
                saved.getId(),
                saved.getUser().getFullName(),
                workspaceName,
                saved.getRecordType(),
                saved.getSource(),
                saved.getRegisteredAt(),
                saved.getJustification()
        );
    }

    public long countJustificationsPending(UUID employeeId, UUID workspaceId){
        return timeRecordRepository.countByUserIdAndWorkspaceIdAndPendingApprovationIsTrue(employeeId, workspaceId);
    }

    public List<TimeRecordResponse> getRecordsByUserIdAndDate(UUID employeeId, LocalDate date, UUID workspaceId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<TimeRecord> records = timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(
                employeeId, workspaceId, startOfDay, endOfDay);
        records = filterActiveRecords(records);

        return records.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public String[] getMonthNames(Locale locale) {
        String rawNames = messageSource.getMessage("month.names.abrev", null, locale);
        return rawNames.split(",");
    }

    public String[] getDayNames(Locale locale) {
        String rawNames = messageSource.getMessage("day.names.abrev", null, locale);
        return rawNames.split(",");
    }

    private SpecialDate findMatchingSpecialDate(List<SpecialDate> specialDates, LocalDate date) {
        return specialDates.stream()
                .filter(sd -> sd.getDate().equals(date) ||
                        (sd.isRecurring() && sd.getDate().getMonth() == date.getMonth() && sd.getDate().getDayOfMonth() == date.getDayOfMonth()))
                .findFirst()
                .orElse(null);
    }
}
