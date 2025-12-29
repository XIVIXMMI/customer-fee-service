package com.hdbank.customer_fee_service.service.strategy;

import com.hdbank.customer_fee_service.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context class that manages fee calculation strategies
 * Implements Strategy Pattern
 */
@Component
@Slf4j
public class FeeCalculationContext {

    private final Map<String, FeeCalculationStrategy> strategies = new HashMap<>();

    /**
     * Auto inject all strategies implementations
     */
    public FeeCalculationContext (List<FeeCalculationStrategy> strategyList){
        for( FeeCalculationStrategy strategy : strategyList){
            strategies.put(strategy.getCalculationType(), strategy);
            log.info("Registered FeeCalculationStrategy: {}", strategy.getCalculationType());
        }
    }

    /**
     * Calculate fee using the appropriate strategy based on calculation type
     */
    public BigDecimal calculateFee(
            String calculationType,
            BigDecimal monthlyFeeAmount,
            Map<String, Object> calculationParams
    ) {
        log.info("Calculating fee using strategy: {}", calculationType);
        FeeCalculationStrategy strategy = strategies.get(calculationType);
        if(strategy == null) {
            throw new BusinessException("INVALID_CALCULATION_TYPE", "No strategy found for calculation type: " + calculationType);
        }

        strategy.validateParams(calculationParams);
        BigDecimal fee = strategy.calculateFee(monthlyFeeAmount, calculationParams);
        log.info("Calculated fee: {} for type {}", fee, calculationType);
        return fee;
    }

    /**
     * Check if calculation type is supported
     */
    public boolean isCalculationTypeSupported(String calculationType) {
        return strategies.containsKey(calculationType);
    }
}
