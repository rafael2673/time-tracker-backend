package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.model.SpecialDate;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.enums.ClosureCountType;
import com.ap101gamestudio.timetracker.model.enums.ClosureShiftRule;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class ClosureCalendarService {

    public LocalDate calculateExecutionDate(YearMonth referenceMonth, Workspace workspace, List<SpecialDate> holidays, WorkPolicy defaultPolicy) {
        if (!workspace.isAutoClosureEnabled() || workspace.getClosureTargetDay() == null) {
            return null;
        }

        LocalDate baseDate = determineBaseDate(referenceMonth, workspace, holidays, defaultPolicy);
        return applyShiftRule(baseDate, workspace.getClosureShiftRule(), holidays, defaultPolicy);
    }

    private LocalDate determineBaseDate(YearMonth referenceMonth, Workspace workspace, List<SpecialDate> holidays, WorkPolicy defaultPolicy) {
        LocalDate firstDayOfNextMonth = referenceMonth.plusMonths(1).atDay(1);
        int targetDay = workspace.getClosureTargetDay();

        if (workspace.getClosureCountType() == ClosureCountType.CALENDAR_DAYS) {
            int maxDays = firstDayOfNextMonth.lengthOfMonth();
            return firstDayOfNextMonth.withDayOfMonth(Math.min(targetDay, maxDays));
        }

        LocalDate currentDate = firstDayOfNextMonth;
        int businessDaysCount = isBusinessDay(currentDate, holidays, defaultPolicy) ? 1 : 0;

        while (businessDaysCount < targetDay) {
            currentDate = currentDate.plusDays(1);
            if (isBusinessDay(currentDate, holidays, defaultPolicy)) {
                businessDaysCount++;
            }
        }

        return currentDate;
    }

    private LocalDate applyShiftRule(LocalDate date, ClosureShiftRule shiftRule, List<SpecialDate> holidays, WorkPolicy defaultPolicy) {
        if (shiftRule == null) return date;

        LocalDate shiftedDate = date;

        if (shiftRule == ClosureShiftRule.SHIFT_BACKWARD) {
            while (!isBusinessDay(shiftedDate, holidays, defaultPolicy)) {
                shiftedDate = shiftedDate.minusDays(1);
            }
        } else if (shiftRule == ClosureShiftRule.SHIFT_FORWARD) {
            while (!isBusinessDay(shiftedDate, holidays, defaultPolicy)) {
                shiftedDate = shiftedDate.plusDays(1);
            }
        }

        return shiftedDate;
    }

    private boolean isBusinessDay(LocalDate date, List<SpecialDate> holidays, WorkPolicy policy) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = policy != null && policy.getWorkingDays() != null && !policy.getWorkingDays().contains(dayOfWeek.name());

        if (isWeekend) {
            return false;
        }

        return holidays.stream().noneMatch(holiday -> isFullHoliday(holiday, date));
    }

    private boolean isFullHoliday(SpecialDate holiday, LocalDate date) {
        boolean dateMatches = holiday.isRecurring()
                ? holiday.getDate().getMonth() == date.getMonth() && holiday.getDate().getDayOfMonth() == date.getDayOfMonth()
                : holiday.getDate().equals(date);

        if (!dateMatches) return false;

        Double multiplier = holiday.getWorkloadMultiplier();
        return multiplier == null || multiplier <= 0.0;
    }
}