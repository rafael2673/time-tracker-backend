package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.CollectiveCompensatoryLeaveRequest;
import com.ap101gamestudio.timetracker.dto.MonthlyBalanceResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.EmployeeLeave;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.EmployeeLeaveRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class EmployeeLeaveServiceTest {

    @Mock
    private EmployeeLeaveRepository leaveRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    @Mock
    private TimeTrackingService timeTrackingService;

    @InjectMocks
    private EmployeeLeaveService employeeLeaveService;

    @Test
    void shouldCreateCollectiveCompensatoryLeaveOnlyForEmployeesWithMinimumBalance() {
        String adminEmail = "admin@email.com";
        UUID workspaceId = UUID.randomUUID();

        User adminUser = new User(adminEmail, "pass", "Admin");
        ReflectionTestUtils.setField(adminUser, "id", UUID.randomUUID());
        Workspace workspace = new Workspace("Office", 0.0, 0.0, 10);
        ReflectionTestUtils.setField(workspace, "id", workspaceId);
        
        WorkspaceMembership adminMembership = new WorkspaceMembership(adminUser, workspace, UserRole.MANAGER, null);

        User positiveUser = new User("positive@email.com", "pass", "Positive");
        ReflectionTestUtils.setField(positiveUser, "id", UUID.randomUUID());
        WorkspaceMembership positiveMembership = new WorkspaceMembership(positiveUser, workspace, UserRole.EMPLOYEE, null);
        positiveMembership.setActive(true);

        User negativeUser = new User("negative@email.com", "pass", "Negative");
        ReflectionTestUtils.setField(negativeUser, "id", UUID.randomUUID());
        WorkspaceMembership negativeMembership = new WorkspaceMembership(negativeUser, workspace, UserRole.EMPLOYEE, null);
        negativeMembership.setActive(true);

        Mockito.when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(adminUser.getId(), workspaceId)).thenReturn(Optional.of(adminMembership));
        Mockito.when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        Mockito.when(membershipRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(positiveMembership, negativeMembership));

        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq("positive@email.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(100.0, 92.0, 8.0, 0, 0.0));

        Mockito.when(timeTrackingService.getQuarterlyBalance(Mockito.eq("negative@email.com"), anyInt(), anyInt(), Mockito.eq(workspaceId)))
                .thenReturn(new MonthlyBalanceResponse(100.0, 105.0, -5.0, 0, 0.0));

        CollectiveCompensatoryLeaveRequest request = new CollectiveCompensatoryLeaveRequest(LocalDate.now(), "Folga", true);

        employeeLeaveService.createCollectiveCompensatoryLeave(adminEmail, workspaceId, request);

        ArgumentCaptor<EmployeeLeave> leaveCaptor = ArgumentCaptor.forClass(EmployeeLeave.class);
        Mockito.verify(leaveRepository, Mockito.times(1)).save(leaveCaptor.capture());

        EmployeeLeave savedLeave = leaveCaptor.getValue();
        Assertions.assertEquals(positiveUser.getId(), savedLeave.getUser().getId());
        Assertions.assertTrue(savedLeave.isDeductFromBalance());
    }

    @Test
    void shouldThrowExceptionIfEmployeeTriesToCreateCollectiveLeave() {
        String email = "emp@email.com";
        UUID workspaceId = UUID.randomUUID();

        User empUser = new User(email, "pass", "Emp");
        ReflectionTestUtils.setField(empUser, "id", UUID.randomUUID());
        
        WorkspaceMembership empMembership = new WorkspaceMembership(empUser, new Workspace("Office", 0.0, 0.0, 10), UserRole.EMPLOYEE, null);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(empUser));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(empUser.getId(), workspaceId)).thenReturn(Optional.of(empMembership));

        CollectiveCompensatoryLeaveRequest request = new CollectiveCompensatoryLeaveRequest(LocalDate.now(), "Folga", true);

        Assertions.assertThrows(DomainException.class, () -> 
            employeeLeaveService.createCollectiveCompensatoryLeave(email, workspaceId, request)
        );
    }
}
