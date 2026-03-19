package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CreateTimeRecordRequest;
import com.ap101gamestudio.timetracker.dto.MonthlyBalanceResponse;
import com.ap101gamestudio.timetracker.dto.TimeRecordResponse;
import com.ap101gamestudio.timetracker.dto.UpdateTimeRecordRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.RecordSource;
import com.ap101gamestudio.timetracker.model.enums.RecordType;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class TimeTrackingServiceTest {

    @Mock
    private TimeRecordRepository timeRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    @Mock
    private SpecialDateRepository specialDateRepository;

    @Mock
    private EmployeeLeaveRepository employeeLeaveRepository;

    @Mock
    private MonthlyClosureRepository monthlyClosureRepository;

    @InjectMocks
    private TimeTrackingService timeTrackingService;

    @Test
    void shouldRegisterPointSuccessfully() {
        String email = "rafaelribeirofranco4@gmail.com";
        UUID workspaceId = UUID.randomUUID();
        Workspace office = new Workspace("Office", -5.8428, -35.1969, 100);
        User user = new User(email, "pass", "Rafa");

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(office));
        Mockito.when(timeRecordRepository.save(any(TimeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTimeRecordRequest request = new CreateTimeRecordRequest(
                RecordType.ENTRY,
                RecordSource.AUTOMATIC_GPS,
                LocalDateTime.now(),
                -5.8428,
                -35.1969
        );

        TimeRecordResponse response = timeTrackingService.registerPoint(email, request, workspaceId);

        Assertions.assertEquals("Office", response.workspaceName());
        Assertions.assertEquals("Rafa", response.userName());
        Assertions.assertEquals(RecordType.ENTRY, response.recordType());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingRecordFromAnotherUser() {
        String email = "rafael@email.com";
        UUID workspaceId = UUID.randomUUID();
        User currentUser = new User(email, "pass", "Rafa");
        ReflectionTestUtils.setField(currentUser, "id", UUID.randomUUID());

        User otherUser = new User("outro@email.com", "pass", "Outro");
        ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());

        Workspace workspace = new Workspace("Office", -5.0, -35.0, 100);
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        TimeRecord originalRecord = new TimeRecord(otherUser, workspace, RecordType.ENTRY, RecordSource.MANUAL, LocalDateTime.now(), null, null);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(currentUser));
        Mockito.when(timeRecordRepository.findById(any())).thenReturn(Optional.of(originalRecord));

        UpdateTimeRecordRequest request = new UpdateTimeRecordRequest(LocalDateTime.now(), "Ajuste");

        Assertions.assertThrows(DomainException.class, () -> timeTrackingService.updateRecord(UUID.randomUUID(), email, request, workspaceId));
    }

    @Test
    void shouldCalculateMonthlyBalanceCorrectly() {
        String email = "rafael@email.com";
        UUID workspaceId = UUID.randomUUID();
        User user = new User(email, "pass", "Rafa");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Workspace workspace = new Workspace("Office", -5.0, -35.0, 100);
        WorkPolicy policy = new WorkPolicy(workspace, "Padrão", 480, 10, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        WorkspaceMembership membership = new WorkspaceMembership(user, workspace, UserRole.EMPLOYEE, policy);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(any(), any())).thenReturn(Optional.of(membership));
        Mockito.when(specialDateRepository.findRelevantDates(any(), any(), any())).thenReturn(List.of());
        Mockito.when(employeeLeaveRepository.findOverlappingLeaves(any(), any(), any(), any())).thenReturn(List.of());
        Mockito.when(monthlyClosureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(any(), any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        
        LocalDateTime today = LocalDateTime.now();
        List<TimeRecord> records = List.of(
                new TimeRecord(user, workspace, RecordType.ENTRY, RecordSource.MANUAL, today.withHour(8).withMinute(0), null, null),
                new TimeRecord(user, workspace, RecordType.EXIT, RecordSource.MANUAL, today.withHour(18).withMinute(0), null, null)
        );

        Mockito.when(timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(any(), any(), any(), any())).thenReturn(records);

        MonthlyBalanceResponse response = timeTrackingService.getMonthlyBalance(email, today.getYear(), today.getMonthValue(), workspaceId);

        Assertions.assertEquals(10.0, response.workedHours());
        Assertions.assertEquals(10.0 - response.expectedHours(), response.balance());
    }

    @Test
    void shouldCalculateMonthlyBalanceWithPausesCorrectly() {
        String email = "rafael@email.com";
        UUID workspaceId = UUID.randomUUID();
        User user = new User(email, "pass", "Rafa");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Workspace workspace = new Workspace("Office", -5.0, -35.0, 100);
        WorkPolicy policy = new WorkPolicy(workspace, "Padrão", 480, 10, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        WorkspaceMembership membership = new WorkspaceMembership(user, workspace, UserRole.EMPLOYEE, policy);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(any(), any())).thenReturn(Optional.of(membership));
        Mockito.when(specialDateRepository.findRelevantDates(any(), any(), any())).thenReturn(List.of());
        Mockito.when(employeeLeaveRepository.findOverlappingLeaves(any(), any(), any(), any())).thenReturn(List.of());
        Mockito.when(monthlyClosureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(any(), any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        
        LocalDateTime today = LocalDateTime.now();
        List<TimeRecord> records = List.of(
                new TimeRecord(user, workspace, RecordType.ENTRY, RecordSource.MANUAL, today.withHour(8).withMinute(0), null, null),
                new TimeRecord(user, workspace, RecordType.PAUSE_START, RecordSource.MANUAL, today.withHour(12).withMinute(0), null, null),
                new TimeRecord(user, workspace, RecordType.PAUSE_END, RecordSource.MANUAL, today.withHour(13).withMinute(0), null, null),
                new TimeRecord(user, workspace, RecordType.EXIT, RecordSource.MANUAL, today.withHour(18).withMinute(0), null, null)
        );

        Mockito.when(timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(any(), any(), any(), any())).thenReturn(records);

        MonthlyBalanceResponse response = timeTrackingService.getMonthlyBalance(email, today.getYear(), today.getMonthValue(), workspaceId);

        // 8 to 12 = 4h
        // 13 to 18 = 5h
        // Total = 9h
        Assertions.assertEquals(9.0, response.workedHours());
    }

    @Test
    void shouldCalculateMonthlyBalanceIgnoringSupersededRecords() {
        String email = "rafael@email.com";
        UUID workspaceId = UUID.randomUUID();
        User user = new User(email, "pass", "Rafa");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Workspace workspace = new Workspace("Office", -5.0, -35.0, 100);
        WorkPolicy policy = new WorkPolicy(workspace, "Padrão", 480, 10, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        WorkspaceMembership membership = new WorkspaceMembership(user, workspace, UserRole.EMPLOYEE, policy);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(any(), any())).thenReturn(Optional.of(membership));
        Mockito.when(specialDateRepository.findRelevantDates(any(), any(), any())).thenReturn(List.of());
        Mockito.when(employeeLeaveRepository.findOverlappingLeaves(any(), any(), any(), any())).thenReturn(List.of());
        Mockito.when(monthlyClosureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(any(), any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        
        LocalDateTime today = LocalDateTime.now();
        TimeRecord oldRecord = new TimeRecord(user, workspace, RecordType.ENTRY, RecordSource.MANUAL, today.withHour(8).withMinute(0), null, null);
        ReflectionTestUtils.setField(oldRecord, "id", UUID.randomUUID());
        
        TimeRecord newRecord = new TimeRecord(user, workspace, RecordType.ENTRY, RecordSource.MANUAL, today.withHour(9).withMinute(0), null, oldRecord);
        ReflectionTestUtils.setField(newRecord, "id", UUID.randomUUID());

        TimeRecord exitRecord = new TimeRecord(user, workspace, RecordType.EXIT, RecordSource.MANUAL, today.withHour(18).withMinute(0), null, null);
        ReflectionTestUtils.setField(exitRecord, "id", UUID.randomUUID());

        List<TimeRecord> records = List.of(oldRecord, newRecord, exitRecord);

        Mockito.when(timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(any(), any(), any(), any())).thenReturn(records);

        MonthlyBalanceResponse response = timeTrackingService.getMonthlyBalance(email, today.getYear(), today.getMonthValue(), workspaceId);

        // newRecord (9:00) to exitRecord (18:00) = 9 hours
        // oldRecord (8:00) is ignored
        Assertions.assertEquals(9.0, response.workedHours());
    }

    @Test
    void shouldCalculateExpectedHoursWithSpecialDatesAndLeaves() {
        String email = "rafael@email.com";
        UUID workspaceId = UUID.randomUUID();
        User user = new User(email, "pass", "Rafa");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Workspace workspace = new Workspace("Office", -5.0, -35.0, 100);
        WorkPolicy policy = new WorkPolicy(workspace, "Padrão", 480, 10, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        WorkspaceMembership membership = new WorkspaceMembership(user, workspace, UserRole.EMPLOYEE, policy);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(any(), any())).thenReturn(Optional.of(membership));
        
        SpecialDate specialDate = new SpecialDate(workspace, LocalDate.of(2023, 1, 2), "Half Day", 0.5, false);
        Mockito.when(specialDateRepository.findRelevantDates(any(), any(), any())).thenReturn(List.of(specialDate));
        
        EmployeeLeave leave = new EmployeeLeave(workspace, user, LocalDate.of(2023, 1, 9), LocalDate.of(2023, 1, 10), "Vacation");
        Mockito.when(employeeLeaveRepository.findOverlappingLeaves(any(), any(), any(), any())).thenReturn(List.of(leave));
        Mockito.when(monthlyClosureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(any(), any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        
        Mockito.when(timeRecordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(any(), any(), any(), any())).thenReturn(List.of());

        MonthlyBalanceResponse response = timeTrackingService.getMonthlyBalance(email, 2023, 1, workspaceId);

        Assertions.assertEquals(0.0, response.workedHours());
        Assertions.assertEquals(156.0, response.expectedHours());
        Assertions.assertEquals(-156.0, response.balance());
    }
}
