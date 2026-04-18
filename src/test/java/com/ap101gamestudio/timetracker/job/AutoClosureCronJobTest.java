package com.ap101gamestudio.timetracker.job;

import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.repository.SpecialDateRepository;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import com.ap101gamestudio.timetracker.service.ClosureCalendarService;
import com.ap101gamestudio.timetracker.service.TimesheetClosureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AutoClosureCronJobTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private SpecialDateRepository specialDateRepository;

    @Mock
    private WorkPolicyRepository workPolicyRepository;

    @Mock
    private TimesheetClosureService closureService;

    @Mock
    private ClosureCalendarService calendarService;

    @InjectMocks
    private AutoClosureCronJob autoClosureCronJob;

    @Test
    void shouldExecuteClosureOnlyForWorkspacesWithMatchingTargetDate() {
        Workspace w1 = new Workspace("W1", 0.0, 0.0, 10);
        w1.setAutoClosureEnabled(true);
        w1.setClosureTargetDay(15);
        ReflectionTestUtils.setField(w1, "id", UUID.randomUUID());

        Workspace w2 = new Workspace("W2", 0.0, 0.0, 10);
        w2.setAutoClosureEnabled(true);
        w2.setClosureTargetDay(20);
        ReflectionTestUtils.setField(w2, "id", UUID.randomUUID());

        Mockito.when(workspaceRepository.findAllByAutoClosureEnabledTrue()).thenReturn(List.of(w1, w2));

        LocalDate today = LocalDate.now();
        YearMonth targetMonth = YearMonth.from(today).minusMonths(1);

        Mockito.when(specialDateRepository.findRelevantDates(any(), any(), any())).thenReturn(List.of());
        Mockito.when(workPolicyRepository.findByWorkspaceId(any())).thenReturn(List.of());

        // Let's pretend today is w1's closure day, but not w2's
        Mockito.when(calendarService.calculateExecutionDate(eq(targetMonth), eq(w1), any(), any()))
                .thenReturn(today);

        Mockito.when(calendarService.calculateExecutionDate(eq(targetMonth), eq(w2), any(), any()))
                .thenReturn(today.plusDays(5));

        autoClosureCronJob.executeDailyClosureCheck();

        // W1 should be closed
        Mockito.verify(closureService, Mockito.times(1)).executeAutoClosureForWorkspace(eq(w1), eq(targetMonth));

        // W2 should NOT be closed
        Mockito.verify(closureService, Mockito.never()).executeAutoClosureForWorkspace(eq(w2), any());
    }
}
