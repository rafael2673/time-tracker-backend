package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.model.enums.RecordType;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.TimeRecordRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import com.ap101gamestudio.timetracker.strategy.TimeCalculationStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TimeTrackingServiceTest {

    @Mock
    private TimeRecordRepository timeRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private TimeCalculationStrategy calculationStrategy;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TimeTrackingService timeTrackingService;

    @BeforeEach
    void setup() {
        Mockito.doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldRegisterRecordAndDetectWorkspace() {
        String userEmail = "rafa@intersistemas.com.br";
        Workspace office = new Workspace("Office", -5.8428, -35.1969, 100);
        User user = new User(userEmail, "pass", "Rafa", UserRole.EMPLOYEE, null, new WorkPolicy("Default", 480, 10));

        Mockito.when(authentication.getName()).thenReturn(userEmail);
        Mockito.when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        Mockito.when(workspaceRepository.findAll()).thenReturn(List.of(office));
        Mockito.when(timeRecordRepository.save(any(TimeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTimeRecordRequest request = new CreateTimeRecordRequest(
                RecordType.ENTRY,
                RecordSource.AUTOMATIC_GPS,
                LocalDateTime.now(),
                -5.8428,
                -35.1969
        );

        TimeRecordResponse response = timeTrackingService.registerRecord(request);

        Assertions.assertEquals("Office", response.workspaceName());
        Assertions.assertEquals(user.getFullName(), response.userName());
    }

    @Test
    void shouldRegisterRecordAsRemoteWhenOutsideRadius() {
        String userEmail = "rafa@intersistemas.com.br";
        Workspace office = new Workspace("Office", -5.8428, -35.1969, 50);
        User user = new User(userEmail, "pass", "Rafa", UserRole.EMPLOYEE, null, new WorkPolicy("Default", 480, 10));

        Mockito.when(authentication.getName()).thenReturn(userEmail);
        Mockito.when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        Mockito.when(workspaceRepository.findAll()).thenReturn(List.of(office));
        Mockito.when(timeRecordRepository.save(any(TimeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTimeRecordRequest request = new CreateTimeRecordRequest(
                RecordType.ENTRY,
                RecordSource.AUTOMATIC_GPS,
                LocalDateTime.now(),
                -5.9000,
                -35.3000
        );

        TimeRecordResponse response = timeTrackingService.registerRecord(request);

        Assertions.assertEquals("Remote / Unknown", response.workspaceName());
    }
}