package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.DailySummaryResponse;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.TimeRecord;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.repository.TimeRecordRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import com.ap101gamestudio.timetracker.strategy.TimeCalculationStrategy;
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
    private final TimeCalculationStrategy calculationStrategy;

    public TimeTrackingService(
            TimeRecordRepository timeRecordRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            TimeCalculationStrategy calculationStrategy
    ) {
        this.timeRecordRepository = timeRecordRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.calculationStrategy = calculationStrategy;
    }

    public TimeRecordResponse registerPoint(String email, CreateTimeRecordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("User not found"));

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

    public List<TimeRecordResponse> getRecordsByDate(String email, LocalDate date) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("User not found"));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<TimeRecord> records = timeRecordRepository.findByUserIdAndRegisteredAtBetween(user.getId(), startOfDay, endOfDay);

        return records.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DailySummaryResponse> getWeeklySummary(String email, LocalDate referenceDate) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("User not found"));

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
            double hours = calculateWorkedHours(dailyRecords);

            weeklySummary.add(new DailySummaryResponse(dayNames[i], hours, currentDate.format(formatter)));
        }

        return weeklySummary;
    }

    public Duration calculateDailyHours(UUID userId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        List<TimeRecord> records = timeRecordRepository.findByUserIdAndRegisteredAtBetween(userId, startOfDay, endOfDay);

        return calculationStrategy.calculateOvertime(records, user.getWorkPolicy());
    }

    private double calculateWorkedHours(List<TimeRecord> records) {
        if (records == null || records.isEmpty()) return 0.0;

        long totalSeconds = 0;
        LocalDateTime lastEntry = null;
        String currentStatus = "IDLE";

        records.sort(Comparator.comparing(TimeRecord::getRegisteredAt));

        for (TimeRecord record : records) {
            LocalDateTime time = record.getRegisteredAt();
            switch (record.getRecordType().name()) {
                case "ENTRY":
                case "PAUSE_END":
                    lastEntry = time;
                    currentStatus = "WORKING";
                    break;
                case "PAUSE_START":
                case "EXIT":
                    if (lastEntry != null && "WORKING".equals(currentStatus)) {
                        totalSeconds += Duration.between(lastEntry, time).getSeconds();
                    }
                    currentStatus = "PAUSED_OR_FINISHED";
                    break;
            }
        }

        if ("WORKING".equals(currentStatus) && lastEntry != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.toLocalDate().equals(lastEntry.toLocalDate())) {
                totalSeconds += Duration.between(lastEntry, now).getSeconds();
            }
        }

        return Math.round((totalSeconds / 3600.0) * 10.0) / 10.0;
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