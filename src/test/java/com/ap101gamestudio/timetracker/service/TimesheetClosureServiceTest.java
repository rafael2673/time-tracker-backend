package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.MonthlyBalanceResponse;
import com.ap101gamestudio.timetracker.dto.MonthlyClosureResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.OvertimeStrategy;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.MonthlyClosureRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class TimesheetClosureServiceTest {

    @Mock
    private MonthlyClosureRepository closureRepository;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    @Mock
    private TimeTrackingService timeTrackingService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TimesheetClosureService closureService;

    private User createMockUser(String email, String name) {
        User user = new User(email, "pass", name);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Workspace createMockWorkspace() {
        Workspace workspace = new Workspace("Office", -5.0, -35.0, 100);
        ReflectionTestUtils.setField(workspace, "id", UUID.randomUUID());
        return workspace;
    }

    private void mockManagerAccess(User manager, Workspace workspace) {
        WorkspaceMembership managerMembership = new WorkspaceMembership(manager, workspace, UserRole.MANAGER, null);
        Mockito.when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(manager.getId(), workspace.getId())).thenReturn(Optional.of(managerMembership));
    }

    @Test
    void shouldThrowExceptionWhenEmployeeTriesToCloseMonth() {
        Workspace workspace = createMockWorkspace();
        User employee = createMockUser("emp@test.com", "Employee");
        WorkspaceMembership membership = new WorkspaceMembership(employee, workspace, UserRole.EMPLOYEE, null);

        Mockito.when(userRepository.findByEmail(employee.getEmail())).thenReturn(Optional.of(employee));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(employee.getId(), workspace.getId())).thenReturn(Optional.of(membership));

        Assertions.assertThrows(DomainException.class, () -> closureService.closeWorkspaceMonth(employee.getEmail(), workspace.getId(), 2026, 3));
    }

    @Test
    void shouldThrowExceptionWhenMonthIsAlreadyClosed() {
        Workspace workspace = createMockWorkspace();
        User manager = createMockUser("mgr@test.com", "Manager");
        mockManagerAccess(manager, workspace);

        Mockito.when(closureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(workspace.getId(), 2026, 3)).thenReturn(true);

        Assertions.assertThrows(DomainException.class, () -> closureService.closeWorkspaceMonth(manager.getEmail(), workspace.getId(), 2026, 3));
    }

    @Test
    void shouldCalculateMixedStrategyCorrectlyAndTransbordOvertime() {
        Workspace workspace = createMockWorkspace();
        User manager = createMockUser("mgr@test.com", "Manager");
        User employee = createMockUser("emp@test.com", "Employee");

        mockManagerAccess(manager, workspace);
        Mockito.when(closureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(workspace.getId(), 2026, 3)).thenReturn(false);

        WorkPolicy mixedPolicy = new WorkPolicy(workspace, "Misto", 480, 10, "MONDAY");
        mixedPolicy.setOvertimeStrategy(OvertimeStrategy.MIXED);
        mixedPolicy.setMaxBankHoursPerMonth(10.0);
        mixedPolicy.setBankExpirationMonths(0);

        WorkspaceMembership employeeMembership = new WorkspaceMembership(employee, workspace, UserRole.EMPLOYEE, mixedPolicy);
        ReflectionTestUtils.setField(employeeMembership, "active", true);

        Mockito.when(membershipRepository.findByWorkspaceId(workspace.getId())).thenReturn(List.of(employeeMembership));

        MonthlyBalanceResponse balance = new MonthlyBalanceResponse(175.0, 160.0, 15.0);
        Mockito.when(timeTrackingService.getMonthlyBalance(employee.getEmail(), 2026, 3, workspace.getId())).thenReturn(balance);
        Mockito.when(closureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(any(), any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        Mockito.when(closureRepository.save(any(MonthlyClosure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<MonthlyClosureResponse> results = closureService.closeWorkspaceMonth(manager.getEmail(), workspace.getId(), 2026, 3);

        Assertions.assertEquals(1, results.size());
        MonthlyClosureResponse response = results.getFirst();

        Assertions.assertEquals(10.0, response.bankedHoursDelta());
        Assertions.assertEquals(5.0, response.paidOvertimeHours());
        Assertions.assertEquals(10.0, response.accumulatedBankHours());
    }

    @Test
    void shouldApplyExpirationRuleAndClearBankAndPayAllAccumulated() {
        Workspace workspace = createMockWorkspace();
        User manager = createMockUser("mgr@test.com", "Manager");
        User employee = createMockUser("emp@test.com", "Employee");

        mockManagerAccess(manager, workspace);
        Mockito.when(closureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(workspace.getId(), 2026, 3)).thenReturn(false);

        WorkPolicy expiringPolicy = new WorkPolicy(workspace, "Trimestral", 480, 10, "MONDAY");
        expiringPolicy.setOvertimeStrategy(OvertimeStrategy.BANK_ONLY);
        expiringPolicy.setBankExpirationMonths(3);

        WorkspaceMembership employeeMembership = new WorkspaceMembership(employee, workspace, UserRole.EMPLOYEE, expiringPolicy);
        ReflectionTestUtils.setField(employeeMembership, "active", true);

        Mockito.when(membershipRepository.findByWorkspaceId(workspace.getId())).thenReturn(List.of(employeeMembership));

        MonthlyBalanceResponse balance = new MonthlyBalanceResponse(165.0, 160.0, 5.0);
        Mockito.when(timeTrackingService.getMonthlyBalance(employee.getEmail(), 2026, 3, workspace.getId())).thenReturn(balance);

        MonthlyClosure previousClosure = new MonthlyClosure(workspace, employee, 2026, 2, 160.0, 160.0, 0.0, 0.0, 0.0, 20.0);
        Mockito.when(closureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(workspace.getId(), employee.getId(), 2026, 2)).thenReturn(Optional.of(previousClosure));

        Mockito.when(closureRepository.save(any(MonthlyClosure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<MonthlyClosureResponse> results = closureService.closeWorkspaceMonth(manager.getEmail(), workspace.getId(), 2026, 3);

        Assertions.assertEquals(1, results.size());
        MonthlyClosureResponse response = results.getFirst();

        Assertions.assertEquals(5.0, response.bankedHoursDelta());
        Assertions.assertEquals(25.0, response.paidOvertimeHours());
        Assertions.assertEquals(0.0, response.accumulatedBankHours());
    }

    @Test
    void shouldReturnPayOnlyOvertimeCorrectly() {
        Workspace workspace = createMockWorkspace();
        User manager = createMockUser("mgr@test.com", "Manager");
        User employee = createMockUser("emp@test.com", "Employee");

        mockManagerAccess(manager, workspace);
        Mockito.when(closureRepository.existsByWorkspaceIdAndReferenceYearAndReferenceMonth(workspace.getId(), 2026, 3)).thenReturn(false);

        WorkPolicy payOnlyPolicy = new WorkPolicy(workspace, "Pay Only", 480, 10, "MONDAY");
        payOnlyPolicy.setOvertimeStrategy(OvertimeStrategy.PAY_ONLY);
        payOnlyPolicy.setBankExpirationMonths(0);

        WorkspaceMembership employeeMembership = new WorkspaceMembership(employee, workspace, UserRole.EMPLOYEE, payOnlyPolicy);
        ReflectionTestUtils.setField(employeeMembership, "active", true);

        Mockito.when(membershipRepository.findByWorkspaceId(workspace.getId())).thenReturn(List.of(employeeMembership));

        MonthlyBalanceResponse balance = new MonthlyBalanceResponse(168.0, 160.0, 8.0);
        Mockito.when(timeTrackingService.getMonthlyBalance(employee.getEmail(), 2026, 3, workspace.getId())).thenReturn(balance);
        Mockito.when(closureRepository.findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(any(), any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        Mockito.when(closureRepository.save(any(MonthlyClosure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<MonthlyClosureResponse> results = closureService.closeWorkspaceMonth(manager.getEmail(), workspace.getId(), 2026, 3);

        Assertions.assertEquals(1, results.size());
        MonthlyClosureResponse response = results.getFirst();

        Assertions.assertEquals(0.0, response.bankedHoursDelta());
        Assertions.assertEquals(8.0, response.paidOvertimeHours());
        Assertions.assertEquals(0.0, response.accumulatedBankHours());
    }
}