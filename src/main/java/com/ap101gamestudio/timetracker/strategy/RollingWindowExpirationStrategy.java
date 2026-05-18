package com.ap101gamestudio.timetracker.strategy;

import com.ap101gamestudio.timetracker.dto.BankAccumulationDto;
import com.ap101gamestudio.timetracker.dto.ExpirationResultDto;
import com.ap101gamestudio.timetracker.dto.OvertimeCalculationDto;
import com.ap101gamestudio.timetracker.model.MonthlyClosure;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.enums.BankExpirationModel;
import com.ap101gamestudio.timetracker.repository.MonthlyClosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RollingWindowExpirationStrategy implements BankExpirationStrategy {

    private final MonthlyClosureRepository closureRepository;

    @Override
    public ExpirationResultDto apply(
            OvertimeCalculationDto overtime,
            BankAccumulationDto accumulation,
            WorkPolicy policy,
            int year,
            int month,
            UUID userId
    ) {
        int expirationMonths = policy.getBankExpirationMonths();
        if (expirationMonths <= 0) {
            return new ExpirationResultDto(overtime, accumulation);
        }

        // Calculate the month that should expire (FIFO)
        // Example: If expires in 3 months. In month 4, the saldo from month 1 expires.
        int targetMonth = month - expirationMonths;
        int targetYear = year;

        if (targetMonth <= 0) {
            targetMonth += 12;
            targetYear -= 1;
        }

        // 1. Get the raw balance generated in that target month
        // We need to know how much "surplus" was added to the bank in that specific month.
        // bankedHoursDelta is what was added to the bank in that month closure.
        double surplusToExpire = closureRepository
                .findByWorkspaceIdAndUserIdAndReferenceYearAndReferenceMonth(policy.getWorkspace().getId(), userId, targetYear, targetMonth)
                .map(MonthlyClosure::getBankedHoursDelta)
                .filter(delta -> delta > 0) // Only positive deltas expire
                .orElse(0.0);

        if (surplusToExpire <= 0) {
            return new ExpirationResultDto(overtime, accumulation);
        }

        // 2. Check if this surplus was already "consumed" by negative balances in subsequent months
        // Logic: FIFO means we consume the oldest hours first. 
        // If the current accumulated bank is smaller than the surplus from that month, 
        // it means part of it was already used.
        double currentTotalBank = accumulation.newAccumulated();
        double actualExpiredAmount = Math.min(surplusToExpire, currentTotalBank);

        if (actualExpiredAmount <= 0) {
            return new ExpirationResultDto(overtime, accumulation);
        }

        // 3. Apply expiration
        OvertimeCalculationDto newOvertime = new OvertimeCalculationDto(
                overtime.paidOvertimeHours() + actualExpiredAmount,
                overtime.bankedDelta()
        );

        BankAccumulationDto newAccumulation = new BankAccumulationDto(
                accumulation.previousAccumulated(),
                currentTotalBank - actualExpiredAmount
        );

        return new ExpirationResultDto(newOvertime, newAccumulation);
    }

    @Override
    public BankExpirationModel getSupportedModel() {
        return BankExpirationModel.ROLLING_WINDOW;
    }
}