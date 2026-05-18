package com.ap101gamestudio.timetracker.strategy;

import com.ap101gamestudio.timetracker.dto.BankAccumulationDto;
import com.ap101gamestudio.timetracker.dto.ExpirationResultDto;
import com.ap101gamestudio.timetracker.dto.OvertimeCalculationDto;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.enums.BankExpirationModel;

import java.util.UUID;

public interface BankExpirationStrategy {
    ExpirationResultDto apply(
        OvertimeCalculationDto currentOvertime, 
        BankAccumulationDto currentAccumulation, 
        WorkPolicy policy, 
        int year, 
        int month, 
        UUID userId
    );
    BankExpirationModel getSupportedModel();
}