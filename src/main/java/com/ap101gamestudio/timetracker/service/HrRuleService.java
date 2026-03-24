package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.VacationRightResponse;
import com.ap101gamestudio.timetracker.model.MonthlyClosure;
import com.ap101gamestudio.timetracker.repository.MonthlyClosureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HrRuleService {

    private final MonthlyClosureRepository closureRepository;

    public HrRuleService(MonthlyClosureRepository closureRepository) {
        this.closureRepository = closureRepository;
    }

    public VacationRightResponse calculateVacationRights(UUID workspaceId, UUID userId, int referenceYear) {
        List<MonthlyClosure> closures = closureRepository.findAllByWorkspaceIdAndUserIdAndReferenceYear(workspaceId, userId, referenceYear);

        int totalAbsences = closures.stream()
                .mapToInt(MonthlyClosure::getUnjustifiedAbsences)
                .sum();

        int vacationDays = calculateDaysByClt(totalAbsences);

        return new VacationRightResponse(totalAbsences, vacationDays, referenceYear);
    }

    private int calculateDaysByClt(int absences) {
        if (absences <= 5) return 30;
        if (absences <= 14) return 24;
        if (absences <= 23) return 18;
        if (absences <= 32) return 12;
        return 0;
    }
}