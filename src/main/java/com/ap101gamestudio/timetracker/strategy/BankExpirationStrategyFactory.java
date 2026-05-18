package com.ap101gamestudio.timetracker.strategy;

import com.ap101gamestudio.timetracker.model.enums.BankExpirationModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BankExpirationStrategyFactory {

    private final Map<BankExpirationModel, BankExpirationStrategy> strategies;

    public BankExpirationStrategyFactory(List<BankExpirationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(BankExpirationStrategy::getSupportedModel, Function.identity()));
    }

    public BankExpirationStrategy getStrategy(BankExpirationModel model) {
        BankExpirationStrategy strategy = strategies.get(model);
        if (strategy == null) {
            // Default to Fixed Cycle if not found
            return strategies.get(BankExpirationModel.FIXED_CYCLE);
        }
        return strategy;
    }
}