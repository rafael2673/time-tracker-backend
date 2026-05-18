package com.ap101gamestudio.timetracker.strategy;

import com.ap101gamestudio.timetracker.dto.BankAccumulationDto;
import com.ap101gamestudio.timetracker.dto.ExpirationResultDto;
import com.ap101gamestudio.timetracker.dto.OvertimeCalculationDto;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.enums.BankExpirationModel;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FixedCycleExpirationStrategy implements BankExpirationStrategy {

    @Override
    public ExpirationResultDto apply(
            OvertimeCalculationDto overtime,
            BankAccumulationDto accumulation,
            WorkPolicy policy,
            int year,
            int month,
            UUID userId
    ) {
        if (policy == null || policy.getBankExpirationMonths() <= 0 || month % policy.getBankExpirationMonths() != 0) {
            return new ExpirationResultDto(overtime, accumulation);
        }

        double finalAccumulated = accumulation.newAccumulated();
        OvertimeCalculationDto newOvertime = overtime;

        if (finalAccumulated > 0) {
            newOvertime = new OvertimeCalculationDto(
                    overtime.paidOvertimeHours() + finalAccumulated,
                    overtime.bankedDelta()
            );
        }

        BankAccumulationDto newAccumulation = new BankAccumulationDto(accumulation.previousAccumulated(), 0.0);

        return new ExpirationResultDto(newOvertime, newAccumulation);
    }

    @Override
    public BankExpirationModel getSupportedModel() {
        return BankExpirationModel.FIXED_CYCLE;
    }
}