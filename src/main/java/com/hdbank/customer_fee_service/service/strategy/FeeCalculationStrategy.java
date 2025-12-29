package com.hdbank.customer_fee_service.service.strategy;

import java.math.BigDecimal;
import java.util.Map;

public interface FeeCalculationStrategy {

    /**
     * Calculate fee based on provided monthly fee amount and calculation parameters.
     * @param monthlyFeeAmount
     * @param calculationParams
     * @return calculate fee amount as BigDecimal
     */
    BigDecimal calculateFee(BigDecimal monthlyFeeAmount, Map<String, Object> calculationParams);

    /**
     * Get the calculation type associated with this strategy.
     * @return calculation type as String
     */
    String getCalculationType();

    /**
     * Validate the calculation parameters.
     * @param calculationParams
     */
    void validateParams(Map<String, Object> calculationParams);
}
