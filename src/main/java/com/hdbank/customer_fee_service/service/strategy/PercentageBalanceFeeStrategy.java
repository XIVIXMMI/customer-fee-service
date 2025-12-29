package com.hdbank.customer_fee_service.service.strategy;

import com.hdbank.customer_fee_service.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Percentage fee strategy - charge a percentage of account balance
 * Example params:
 * {
 *   "balance": 100000000,    // 100M VND
 *   "percentage": 0.001,     // 0.1% per month
 *   "min_fee": 10000,        // Minimum 10k VND
 *   "max_fee": 50000         // Maximum 50k VND
 * }
 * Fee = balance * percentage, capped by min and max
 */
@Component
@Slf4j
public class PercentageBalanceFeeStrategy implements FeeCalculationStrategy{

    private static final int SCALE = 2; // Decimal places for currency
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public BigDecimal calculateFee(BigDecimal monthlyFeeAmount, Map<String, Object> calculationParams) {
        log.info("Calculating percentage fee with params: {}", calculationParams);
        BigDecimal balance = getBalance(calculationParams);
        BigDecimal percentage = getPercentage(calculationParams);
        BigDecimal rawFee = balance.multiply(percentage).setScale(SCALE,ROUNDING_MODE);
        log.info("Raw fee calculated: {} (balance: {} * percentage: {})", rawFee, balance, percentage);
        BigDecimal finalFee = applyMinMaxCaps(rawFee, calculationParams);
        log.info("Final fee after applying min/max caps: {}", finalFee);
        return finalFee;
    }

    @Override
    public String getCalculationType() {
        return "PERCENTAGE";
    }

    @Override
    public void validateParams(Map<String, Object> calculationParams) {
        if (calculationParams == null || calculationParams.isEmpty()) {
            throw new ValidationException("Calculation params are required for percentage fee");
        }

        if (!calculationParams.containsKey("balance")) {
            throw new ValidationException("'balance' is required in calculation params");
        }

        if (!calculationParams.containsKey("percentage")) {
            throw new ValidationException("'percentage' is required in calculation params");
        }

        BigDecimal percentage = getPercentage(calculationParams);
        if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Percentage must be greater than 0");
        }

        if (percentage.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("Percentage must be less than or equal to 1 (100%)");
        }
    }

    private BigDecimal getBalance(Map<String, Object> params) {
        Object balanceObj = params.get("balance");
        if (balanceObj instanceof Number) {
            return new BigDecimal(balanceObj.toString());
        }
        throw new ValidationException("Invalid balance value");
    }

    private BigDecimal getPercentage(Map<String, Object> params) {
        Object percentageObj = params.get("percentage");
        if (percentageObj instanceof Number) {
            return new BigDecimal(percentageObj.toString());
        }
        throw new ValidationException("Invalid percentage value");
    }

    private BigDecimal applyMinMaxCaps(BigDecimal fee, Map<String, Object> params) {
        BigDecimal result = fee;

        // Apply minimum cap
        if (params.containsKey("min_fee")) {
            BigDecimal minFee = new BigDecimal(params.get("min_fee").toString());
            if (result.compareTo(minFee) < 0) {
                log.info("Applying min cap: {} -> {}", result, minFee);
                result = minFee;
            }
        }

        // Apply maximum cap
        if (params.containsKey("max_fee")) {
            BigDecimal maxFee = new BigDecimal(params.get("max_fee").toString());
            if (result.compareTo(maxFee) > 0) {
                log.info("Applying max cap: {} -> {}", result, maxFee);
                result = maxFee;
            }
        }

        return result;
    }
}
