package com.hdbank.customer_fee_service.service.strategy;

import com.hdbank.customer_fee_service.entity.CalculationType;
import com.hdbank.customer_fee_service.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Tiered Transaction Fee - charge based on balance tiers
 *  Example:
 *  {
 *      "balance": 10000000 // 10M VND
 *      "tiers": [
 *          {"from": 0, "to": 50000000, "fee":10000}, // 0 - 50M : 10k VND
 *          {"from": 50000001, "to": 200000000, "fee":20000}, // 50M - 200M : 20k VND
 *          {"from": 200000001, "to": null, "fee":50000} // > 200M : 50k VND
 *      ]
 *  }
 */
@Component
@Slf4j
public class TieredBalanceFeeStrategy implements FeeCalculationStrategy{

    private static final int LESS_THAN = -1;
    private static final int EQUAL_TO = 0;
    private static final int GREATER_THAN = 1;

    @Override
    public BigDecimal calculateFee(BigDecimal monthlyFeeAmount, Map<String, Object> calculationParams) {
        log.info("Calculating tiered transaction fee with params: {}", calculationParams);
        BigDecimal balance = getBalance(calculationParams);
        List<Map<String,Object>> tiers = getTiers(calculationParams);
        // Find matching tier
        for( Map<String, Object> tier : tiers){
            BigDecimal from = new BigDecimal(tier.get("from").toString());
            Object toObj = tier.get("to");
            BigDecimal to = toObj != null ? new BigDecimal(toObj.toString()) : null;

            // Check if balances falls in this tier
            boolean matchesFrom = balance.compareTo(from) >= EQUAL_TO;
            boolean matchesTo = (to == null) || (balance.compareTo(to) <= EQUAL_TO);

            if(matchesFrom && matchesTo){
                BigDecimal fee = new BigDecimal(tier.get("fee").toString());
                log.info("Balance {} falls in tier (from: {}, to: {}), applying fee: {}", balance, from, to, fee);
                return fee;
            }
        }
        log.warn("No matching tier found, defaulting to monthly fee amount: {}", monthlyFeeAmount);
        return monthlyFeeAmount;
    }

    @Override
    public String getCalculationType() {
        return CalculationType.TIERED.toString();
    }

    @Override
    public void validateParams(Map<String, Object> calculationParams) {
        if (calculationParams == null || calculationParams.isEmpty()) {
            throw new ValidationException("Calculation params are required for tiered fee");
        }

        if (!calculationParams.containsKey("balance")) {
            throw new ValidationException("'balance' is required in calculation params for tiered fee");
        }

        if (!calculationParams.containsKey("tiers")) {
            throw new ValidationException("'tiers' is required in calculation params for tiered fee");
        }

        Object tiersObj = calculationParams.get("tiers");
        if (!(tiersObj instanceof List)) {
            throw new ValidationException("'tiers' must be a list");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tiers = (List<Map<String, Object>>) tiersObj;

        if (tiers.isEmpty()) {
            throw new ValidationException("At least one tier is required");
        }

        for (Map<String, Object> tier : tiers) {
            if (!tier.containsKey("from") || !tier.containsKey("fee")) {
                throw new ValidationException("Each tier must have 'from' and 'fee' fields");
            }
        }
    }

    private BigDecimal getBalance(Map<String, Object> params) {
        Object balanceObj = params.get("balance");
        if( balanceObj instanceof  Number){
            return new BigDecimal(balanceObj.toString());
        } else {
            throw new IllegalArgumentException("Invalid or missing 'balance' parameter");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> getTiers(Map<String, Object> params) {
        return (List<Map<String, Object>>) params.get("tiers");
    }
}
