package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.LaborRiskRankingResponse;
import com.ap101gamestudio.timetracker.dto.MonthlyBalanceResponse;
import com.ap101gamestudio.timetracker.dto.NextHolidayResponse;
import com.ap101gamestudio.timetracker.dto.TimeDistributionResponse;
import com.ap101gamestudio.timetracker.model.SpecialDate;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.SpecialDateType;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.SpecialDateRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.MonthlyClosureRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    @Mock
    private SpecialDateRepository specialDateRepository;

    @Mock
    @SuppressWarnings("unused")
    private MonthlyClosureRepository monthlyClosureRepository;

    @Mock
    private TimeTrackingService timeTrackingService;

    @InjectMocks
    private SummaryService summaryService;

    @Test
    void shouldGenerateLaborRiskRankingCorrectly() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = new Workspace("Office", 0.0, 0.0, 10);
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        User uP1 = new User("p1@e.com", "pass", "P1"); ReflectionTestUtils.setField(uP1, "id", UUID.randomUUID());
        User uP2 = new User("p2@e.com", "pass", "P2"); ReflectionTestUtils.setField(uP2, "id", UUID.randomUUID());
        User uP3 = new User("p3@e.com", "pass", "P3"); ReflectionTestUtils.setField(uP3, "id", UUID.randomUUID());

        User uN1 = new User("n1@e.com", "pass", "N1"); ReflectionTestUtils.setField(uN1, "id", UUID.randomUUID());
        User uN2 = new User("n2@e.com", "pass", "N2"); ReflectionTestUtils.setField(uN2, "id", UUID.randomUUID());

        List<WorkspaceMembership> members = List.of(
                createMembership(uP1, workspace), createMembership(uP2, workspace), createMembership(uP3, workspace),
                createMembership(uN1, workspace), createMembership(uN2, workspace)
        );

        Mockito.when(membershipRepository.findByWorkspaceId(workspaceId)).thenReturn(members);
        Mockito.when(monthlyClosureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(
                Mockito.eq(workspaceId), anyInt(), anyInt())).thenReturn(true);

        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq("p1@e.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(0.0, 0.0, 20.0, 0, 0.0));
        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq("p2@e.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(0.0, 0.0, 10.0, 0, 0.0));
        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq("p3@e.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(0.0, 0.0, 50.0, 0, 0.0));

        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq("n1@e.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(0.0, 0.0, -15.0, 0, 0.0));
        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq("n2@e.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(0.0, 0.0, -5.0, 0, 0.0));

        LaborRiskRankingResponse response = summaryService.getLaborRiskRanking(workspaceId, null, null);

        Assertions.assertEquals(3, response.topPositive().size());
        Assertions.assertEquals(50.0, response.topPositive().get(0).balance());
        Assertions.assertEquals(20.0, response.topPositive().get(1).balance());
        Assertions.assertEquals(10.0, response.topPositive().get(2).balance());

        Assertions.assertEquals(2, response.topNegative().size());
        Assertions.assertEquals(-15.0, response.topNegative().get(0).balance());
        Assertions.assertEquals(-5.0, response.topNegative().get(1).balance());
    }

    @Test
    void shouldGenerateTimeDistributionCorrectly() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = new Workspace("Office", 0.0, 0.0, 10);
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        User u1 = new User("u1@e.com", "pass", "U1"); ReflectionTestUtils.setField(u1, "id", UUID.randomUUID());
        User u2 = new User("u2@e.com", "pass", "U2"); ReflectionTestUtils.setField(u2, "id", UUID.randomUUID());

        Mockito.when(membershipRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(
                createMembership(u1, workspace), createMembership(u2, workspace)
        ));

        Mockito.when(timeTrackingService.getMonthlyBalance(Mockito.eq("u1@e.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(180.0, 160.0, 20.0, 0, 0.0));

        Mockito.when(timeTrackingService.getMonthlyBalance(Mockito.eq("u2@e.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(150.0, 160.0, -10.0, 0, 0.0));

        TimeDistributionResponse response = summaryService.getTimeDistribution(workspaceId, 2023, 10);

        Assertions.assertEquals(310.0, response.regularHours());
        Assertions.assertEquals(20.0, response.overtimeHours());
        Assertions.assertEquals(10.0, response.absenceHours());
        Assertions.assertEquals(320.0, response.totalExpectedHours());
    }

    @Test
    void shouldIgnoreCompensatoryCollectiveWhenEmployeeHasInsufficientBalance() {
        UUID workspaceId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);

        User employee = new User("employee@email.com", "pass", "Employee");
        ReflectionTestUtils.setField(employee, "id", employeeId);

        Workspace workspace = new Workspace("Office", 0.0, 0.0, 10);

        SpecialDate compensatoryCollective = new SpecialDate(
                workspace,
                tomorrow,
                "Folga coletiva",
                0.0,
                false,
                SpecialDateType.COMPENSATORY_COLLECTIVE
        );

        SpecialDate nationalHoliday = new SpecialDate(
                workspace,
                dayAfterTomorrow,
                "Feriado nacional",
                0.0,
                false,
                SpecialDateType.NATIONAL
        );

        Mockito.when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        Mockito.when(specialDateRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(compensatoryCollective, nationalHoliday));
        Mockito.when(monthlyClosureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(
                Mockito.eq(workspaceId), anyInt(), anyInt())).thenReturn(true);
        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq(employee.getEmail()), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(0.0, 0.0, 7.5, 0, 0.0));

        NextHolidayResponse response = summaryService.getNextSpecialDate(workspaceId, employeeId, employee.getEmail());

        Assertions.assertNotNull(response);
        Assertions.assertEquals("Feriado nacional", response.name());
        Assertions.assertEquals(dayAfterTomorrow, response.date());
    }

    @Test
    void shouldReturnCompensatoryCollectiveWhenEmployeeHasEnoughBalance() {
        UUID workspaceId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);

        User employee = new User("employee@email.com", "pass", "Employee");
        ReflectionTestUtils.setField(employee, "id", employeeId);

        Workspace workspace = new Workspace("Office", 0.0, 0.0, 10);

        SpecialDate compensatoryCollective = new SpecialDate(
                workspace,
                tomorrow,
                "Folga coletiva",
                0.0,
                false,
                SpecialDateType.COMPENSATORY_COLLECTIVE
        );

        SpecialDate nationalHoliday = new SpecialDate(
                workspace,
                dayAfterTomorrow,
                "Feriado nacional",
                0.0,
                false,
                SpecialDateType.NATIONAL
        );

        Mockito.when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        Mockito.when(specialDateRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(compensatoryCollective, nationalHoliday));
        Mockito.when(monthlyClosureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(
                Mockito.eq(workspaceId), anyInt(), anyInt())).thenReturn(true);
        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq(employee.getEmail()), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(0.0, 0.0, 8.0, 0, 0.0));

        NextHolidayResponse response = summaryService.getNextSpecialDate(workspaceId, employeeId, employee.getEmail());

        Assertions.assertNotNull(response);
        Assertions.assertEquals("Folga coletiva", response.name());
        Assertions.assertEquals(tomorrow, response.date());
    }

    private WorkspaceMembership createMembership(User user, Workspace workspace) {
        WorkspaceMembership membership = new WorkspaceMembership(user, workspace, UserRole.EMPLOYEE, null);
        membership.setActive(true);
        return membership;
    }
}