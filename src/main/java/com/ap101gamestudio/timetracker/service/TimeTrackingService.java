package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.DailySummaryResponse;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.dto.UpdateTimeRecordRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.TimeRecord;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.repository.TimeRecordRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TimeTrackingService {

    private final TimeRecordRepository timeRecordRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    public TimeTrackingService(
            TimeRecordRepository timeRecordRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository
    ) {
        this.timeRecordRepository = timeRecordRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public TimeRecordResponse registerPoint(String email, CreateTimeRecordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        Workspace detectedWorkspace = findWorkspaceByLocation(request.latitude(), request.longitude());

        TimeRecord record = new TimeRecord(
                user,
                detectedWorkspace,
                request.recordType(),
                request.source(),
                request.registeredAt() != null ? request.registeredAt() : LocalDateTime.now(),
                null,
                null
        );

        TimeRecord saved = timeRecordRepository.save(record);
        return mapToResponse(saved);
    }

    public TimeRecordResponse updateRecord(UUID id, String email, UpdateTimeRecordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        TimeRecord originalRecord = timeRecordRepository.findById(id)
                .orElseThrow(() -> new DomainException("error.record.not_found"));

        if (!originalRecord.getUser().getId().equals(user.getId())) {
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

    public List<TimeRecordResponse> getRecordsByDate(String email, LocalDate date) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<TimeRecord> records = timeRecordRepository.findByUserIdAndRegisteredAtBetween(user.getId(), startOfDay, endOfDay);
        records = filterActiveRecords(records);

        return records.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DailySummaryResponse> getWeeklySummary(String email, LocalDate referenceDate) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        LocalDate startOfWeek = referenceDate;
        while (startOfWeek.getDayOfWeek() != DayOfWeek.SUNDAY) {
            startOfWeek = startOfWeek.minusDays(1);
        }

        List<DailySummaryResponse> weeklySummary = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String[] dayNames = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startOfWeek.plusDays(i);
            LocalDateTime startOfDay = currentDate.atStartOfDay();
            LocalDateTime endOfDay = currentDate.atTime(23, 59, 59);

            List<TimeRecord> dailyRecords = timeRecordRepository.findByUserIdAndRegisteredAtBetween(user.getId(), startOfDay, endOfDay);
            dailyRecords = filterActiveRecords(dailyRecords);
            double hours = calculateWorkedHours(dailyRecords);

            weeklySummary.add(new DailySummaryResponse(dayNames[i], hours, currentDate.format(formatter)));
        }

        return weeklySummary;
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

    private Workspace findWorkspaceByLocation(Double lat, Double lon) {
        if (lat == null || lon == null) return null;

        List<Workspace> allWorkspaces = workspaceRepository.findAll();

        return allWorkspaces.stream()
                .filter(ws -> calculateDistance(lat, lon, ws.getLatitude(), ws.getLongitude()) <= ws.getRadiusMeters())
                .findFirst()
                .orElse(null);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
}