package com.ap101gamestudio.timetracker.job;

import com.ap101gamestudio.timetracker.model.SpecialDate;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.repository.SpecialDateRepository;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import com.ap101gamestudio.timetracker.service.ClosureCalendarService;
import com.ap101gamestudio.timetracker.service.TimesheetClosureService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AutoClosureCronJob {

    private final WorkspaceRepository workspaceRepository;
    private final SpecialDateRepository specialDateRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final ClosureCalendarService calendarService;
    private final TimesheetClosureService closureService;

    @Scheduled(cron = "0 0 2 * * *")
    public void executeDailyClosureCheck() {
        LocalDate today = LocalDate.now();
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);

        List<Workspace> activeWorkspaces = workspaceRepository.findAllByAutoClosureEnabledTrue();

        for (Workspace workspace : activeWorkspaces) {
            evaluateAndExecuteWorkspaceClosure(workspace, today, previousMonth);
        }
    }

    private void evaluateAndExecuteWorkspaceClosure(Workspace workspace, LocalDate today, YearMonth targetMonth) {
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<SpecialDate> holidays = specialDateRepository.findRelevantDates(workspace.getId(), monthStart, monthEnd);

        WorkPolicy defaultPolicy = workPolicyRepository.findByWorkspaceId(workspace.getId())
                .stream()
                .findFirst()
                .orElse(null);

        LocalDate scheduledExecutionDate = calendarService.calculateExecutionDate(targetMonth, workspace, holidays, defaultPolicy);

        if (today.equals(scheduledExecutionDate)) {
            closureService.executeAutoClosureForWorkspace(workspace, targetMonth);
        }
    }
}