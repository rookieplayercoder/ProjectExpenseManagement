package com.prateek.ProjectExpenseManagement.strategy;

import com.prateek.ProjectExpenseManagement.domain.SplitType;
import com.prateek.ProjectExpenseManagement.exception.BusinessValidationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SplitStrategyFactory {

    private final Map<SplitType, ExpenseSplitStrategy> strategyMap = new EnumMap<>(SplitType.class);

    public SplitStrategyFactory(List<ExpenseSplitStrategy> strategies) {
        for (ExpenseSplitStrategy strategy : strategies) {
            strategyMap.put(strategy.getSupportedType(), strategy);
        }
    }

    public ExpenseSplitStrategy getStrategy(SplitType splitType) {
        ExpenseSplitStrategy strategy = strategyMap.get(splitType);
        if (strategy == null) {
            throw new BusinessValidationException("No split strategy found for split type: " + splitType);
        }
        return strategy;
    }
}

