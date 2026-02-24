package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.TimeRecord;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.repository.TimeRecordRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import com.ap101gamestudio.timetracker.strategy.TimeCalculationStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
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

    public TimeRecordResponse registerRecord(CreateTimeRecordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new DomainException("No authentication data found");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("User not found in context"));

        Workspace detectedWorkspace = findWorkspaceByLocation(request.latitude(), request.longitude());

        TimeRecord record = new TimeRecord(
                user,
                detectedWorkspace,
                request.recordType(),
                request.source(),
                request.registeredAt(),
                null,
                null
        );

        TimeRecord saved = timeRecordRepository.save(record);

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

    public Duration calculateDailyHours(UUID userId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        List<TimeRecord> records = timeRecordRepository.findByUserIdAndRegisteredAtBetween(userId, startOfDay, endOfDay);

        return calculationStrategy.calculateOvertime(records, user.getWorkPolicy());
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
}